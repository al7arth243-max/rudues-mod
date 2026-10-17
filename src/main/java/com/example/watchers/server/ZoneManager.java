package com.example.watchers.server;

import com.example.watchers.WatchersConfig;
import com.example.watchers.WatchersMod;
import com.example.watchers.entity.WatcherEntity;
import com.example.watchers.network.ModNetwork;
import com.example.watchers.registry.ModDamage;
import com.example.watchers.zone.HauntedZones;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Runs the haunting for every player, every tick.
 *
 * "Exposure" is how long (in ticks) a player has been inside a zone. It grows by 1 per tick inside
 * a zone and drains faster outside. Everything is decided from that one number:
 *
 *   exposure >= clueDelay                        first clue: track + fog start, watcher #1 appears
 *   every further watcherInterval                one more watcher, up to maxWatchers
 *   the last watcher                             appears right in front of the player, who dies shortly after
 *   exposure falls back below a watcher's step   that watcher leaves (once the player looks away)
 *
 * Client messages (see {@link ModNetwork}): 0 = fade out, 1 = clue active, 2 = final scare, 3 = instant reset.
 */
@Mod.EventBusSubscriber(modid = WatchersMod.MODID)
public final class ZoneManager {
    private static final Map<UUID, PlayerState> STATES = new HashMap<>();

    private ZoneManager() {
    }

    private static final class PlayerState {
        final ServerLevel level;
        double exposure;
        final List<UUID> watchers = new ArrayList<>();
        boolean clueActive;
        int deathCountdown = -1;
        int spawnCooldown;

        PlayerState(ServerLevel level) {
            this.level = level;
        }
    }

    // ---------------------------------------------------------------- events

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) {
            return;
        }
        if (event.player instanceof ServerPlayer player) {
            tick(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player, false, 0);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player, true, 3);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player, true, 3);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player, true, 0);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        STATES.clear();
    }

    // ------------------------------------------------------------ main logic

    private static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        PlayerState st = STATES.get(id);

        boolean exempt = !player.isAlive()
                || player.isSpectator()
                || (WatchersConfig.EXEMPT_CREATIVE.get() && player.isCreative());
        if (exempt) {
            if (st != null) {
                clear(player, true, 0);
            }
            return;
        }

        ServerLevel level = player.serverLevel();
        if (st != null && st.level != level) {
            clear(player, true, 3);
            st = null;
        }

        boolean inZone = HauntedZones.isHaunted(level, player.getX(), player.getZ());
        if (st == null) {
            if (!inZone) {
                return;
            }
            st = new PlayerState(level);
            STATES.put(id, st);
        }

        // The last watcher is out: count down to the kill. Nothing can stop it now.
        if (st.deathCountdown >= 0) {
            if (st.deathCountdown == 0) {
                kill(player, level);
                clear(player, true, 0);
            } else {
                st.deathCountdown--;
            }
            return;
        }

        int clueStart = ticks(WatchersConfig.CLUE_DELAY_SECONDS.get());
        int interval = ticks(WatchersConfig.WATCHER_INTERVAL_SECONDS.get());
        int max = WatchersConfig.MAX_WATCHERS.get();

        // Build up inside a zone, drain faster outside.
        if (inZone) {
            st.exposure += 1.0D;
        } else {
            st.exposure = Math.max(0.0D, st.exposure - WatchersConfig.DRAIN_MULTIPLIER.get());
            if (st.exposure <= 0.0D) {
                clear(player, true, 0);
                return;
            }
        }

        // Forget watchers that no longer exist.
        st.watchers.removeIf(wid -> !(level.getEntity(wid) instanceof WatcherEntity w && w.isAlive()));

        // How many watchers should there be right now?
        int target = 0;
        if (st.exposure >= clueStart) {
            target = Math.min(max, 1 + (int) ((st.exposure - clueStart) / interval));
        }

        // Too many (exposure drained): the newest ones leave once the player is not looking.
        while (st.watchers.size() > target) {
            UUID extra = st.watchers.remove(st.watchers.size() - 1);
            if (level.getEntity(extra) instanceof WatcherEntity w) {
                w.markForRemoval();
            }
        }

        // Too few: bring in the next one.
        if (st.spawnCooldown > 0) {
            st.spawnCooldown--;
        }
        if (inZone && st.watchers.size() < target && st.spawnCooldown == 0) {
            boolean isLast = st.watchers.size() + 1 >= max;
            if (isLast) {
                if (spawnFinal(player, level, st)) {
                    return;
                }
                st.spawnCooldown = 10;
            } else if (spawnFar(player, level, st)) {
                st.spawnCooldown = 10;
            } else {
                st.spawnCooldown = 20;
            }
        }

        // Watchers left far behind catch up (while the player cannot see them).
        if (player.tickCount % 20 == 0) {
            relocateStragglers(player, level, st);
        }

        // The clue starts with the first watcher (or a moment later if none could be placed yet).
        boolean wantClue = inZone
                && st.exposure >= clueStart
                && (!st.watchers.isEmpty() || st.exposure >= clueStart + 100);
        if (wantClue != st.clueActive) {
            st.clueActive = wantClue;
            ModNetwork.sendState(player, wantClue ? 1 : 0);
        }
    }

    private static boolean spawnFar(ServerPlayer player, ServerLevel level, PlayerState st) {
        Vec3 spot = WatcherSpawner.findFarSpot(level, player, liveWatchers(level, st));
        if (spot == null) {
            return false;
        }
        WatcherEntity w = WatcherSpawner.spawn(level, player, spot.x, spot.y, spot.z);
        if (w == null) {
            return false;
        }
        st.watchers.add(w.getUUID());
        return true;
    }

    /** The last watcher: right in front of the player, who dies shortly after. */
    private static boolean spawnFinal(ServerPlayer player, ServerLevel level, PlayerState st) {
        double yaw = Math.toRadians(player.getYRot());
        double distance = WatchersConfig.FINAL_DISTANCE.get();
        double x = player.getX() - Math.sin(yaw) * distance;
        double z = player.getZ() + Math.cos(yaw) * distance;

        Vec3 ground = WatcherSpawner.findGround(level, x, z, player.getY());
        double y = ground != null && Math.abs(ground.y - player.getY()) <= 3.0D ? ground.y : player.getY();

        WatcherEntity w = WatcherSpawner.spawn(level, player, x, y, z);
        if (w == null) {
            return false;
        }
        st.watchers.add(w.getUUID());
        st.deathCountdown = ticks(WatchersConfig.DEATH_DELAY_SECONDS.get());
        st.clueActive = true;
        ModNetwork.sendState(player, 2);

        if (WatchersConfig.FORCE_LOOK_AT_FINAL.get()) {
            player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(x, y + WatcherEntity.FACE_HEIGHT, z));
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        }
        return true;
    }

    private static void relocateStragglers(ServerPlayer player, ServerLevel level, PlayerState st) {
        double limit = WatchersConfig.SPAWN_MAX_DISTANCE.get() + 25.0D;
        List<WatcherEntity> alive = liveWatchers(level, st);
        for (WatcherEntity w : alive) {
            if (w.distanceToSqr(player) > limit * limit) {
                Vec3 spot = WatcherSpawner.findFarSpot(level, player, alive);
                if (spot != null) {
                    w.moveTo(spot.x, spot.y, spot.z, w.getYRot(), 0.0F);
                }
            }
        }
    }

    private static List<WatcherEntity> liveWatchers(ServerLevel level, PlayerState st) {
        List<WatcherEntity> list = new ArrayList<>(st.watchers.size());
        for (UUID wid : st.watchers) {
            if (level.getEntity(wid) instanceof WatcherEntity w) {
                list.add(w);
            }
        }
        return list;
    }

    /** Kills the player in a way that ignores totems of undying, armor, effects and invulnerability. */
    private static void kill(ServerPlayer player, ServerLevel level) {
        DamageSource source = ModDamage.watched(level);
        player.hurt(source, Float.MAX_VALUE);
        if (player.isAlive()) {
            player.setHealth(0.0F);
            player.die(source);
        }
    }

    /**
     * Removes the player's haunting state and its watchers.
     *
     * @param notify tell the player's client to stop the effects
     * @param mode   0 = fade out, 3 = instant reset
     */
    private static void clear(ServerPlayer player, boolean notify, int mode) {
        PlayerState st = STATES.remove(player.getUUID());
        if (st == null) {
            return;
        }
        for (UUID wid : st.watchers) {
            if (st.level.getEntity(wid) instanceof WatcherEntity w) {
                w.discard();
            }
        }
        if (notify && (st.clueActive || st.deathCountdown >= 0 || mode == 3)) {
            ModNetwork.sendState(player, mode);
        }
    }

    private static int ticks(double seconds) {
        return Math.max(0, Mth.floor(seconds * 20.0D + 0.5D));
    }

    // ----------------------------------------------- used by the /watchers command

    public static double getExposureTicks(UUID id) {
        PlayerState st = STATES.get(id);
        return st == null ? 0.0D : st.exposure;
    }

    public static int getWatcherCount(UUID id) {
        PlayerState st = STATES.get(id);
        return st == null ? 0 : st.watchers.size();
    }

    /** Adds exposure for testing. Only works while the player is inside a zone. Returns false otherwise. */
    public static boolean addExposure(ServerPlayer player, int seconds) {
        ServerLevel level = player.serverLevel();
        PlayerState st = STATES.get(player.getUUID());
        if (st == null) {
            if (!HauntedZones.isHaunted(level, player.getX(), player.getZ())) {
                return false;
            }
            st = new PlayerState(level);
            STATES.put(player.getUUID(), st);
        }
        st.exposure += seconds * 20.0D;
        return true;
    }

    public static void reset(ServerPlayer player) {
        clear(player, true, 3);
    }

    @Nullable
    public static HauntedZones.Zone currentZone(ServerPlayer player) {
        return HauntedZones.zoneAt(player.serverLevel(), player.getX(), player.getZ());
    }
}
