package com.rudues.mod.events;

import com.rudues.mod.RuduesMod;
import com.rudues.mod.ability.AbilityHandler;
import com.rudues.mod.ability.HeroForm;
import com.rudues.mod.ability.PowerData;
import com.rudues.mod.registry.ModEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RuduesMod.MODID)
public class CommonEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer sp)) {
            return;
        }
        if (!PowerData.has(sp)) {
            return;
        }
        if (sp.tickCount % 20 == 0) {
            PowerData.applyEffects(sp);
        }
        HeroForm.tick(sp);
    }

    /** While transformed the player's hitbox becomes demon-sized (both client and server run this). */
    @SubscribeEvent
    public static void onSize(EntityEvent.Size event) {
        try {
            if (event.getEntity() instanceof Player player
                    && ModEffects.HERO_OF_HELL.isPresent()
                    && player.hasEffect(ModEffects.HERO_OF_HELL.get())
                    && (event.getPose() == Pose.STANDING || event.getPose() == Pose.CROUCHING)) {
                event.setNewSize(EntityDimensions.scalable(1.6F, 5.0F), true);
            }
        } catch (RuntimeException ignored) {
            // this event can fire while the player object is still being constructed (effects not ready yet)
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        CompoundTag old = event.getOriginal().getPersistentData();
        if (old.contains(Player.PERSISTED_NBT_TAG, 10)) {
            event.getEntity().getPersistentData().put(Player.PERSISTED_NBT_TAG,
                    old.getCompound(Player.PERSISTED_NBT_TAG).copy());
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            HeroForm.cleanup(sp);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            // the marker effect is saved with the player; make sure nobody logs in stuck as a demon
            sp.removeEffect(ModEffects.HERO_OF_HELL.get());
            sp.refreshDimensions();
            HeroForm.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            sp.removeEffect(ModEffects.HERO_OF_HELL.get());
            sp.refreshDimensions();
            if (PowerData.has(sp)) {
                PowerData.applyEffects(sp);
            }
            HeroForm.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            HeroForm.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        AbilityHandler.clear(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer sp) {
            HeroForm.cleanup(sp);
        }
    }
}
