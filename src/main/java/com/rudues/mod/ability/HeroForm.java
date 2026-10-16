package com.rudues.mod.ability;

import com.rudues.mod.registry.ModParticles;
import com.rudues.mod.entity.DemonEntity;
import com.rudues.mod.entity.FiendEntity;
import com.rudues.mod.entity.RageBlastEntity;
import com.rudues.mod.network.Network;
import com.rudues.mod.registry.ModEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server side of the Hero of Hell mechanic.
 * phase 0: gauge is filling   phase 1: flame-pillar transformation   phase 2: demon form (boss bar + 4 moves)
 */
public class HeroForm {
    private static final UUID HEALTH_MOD = UUID.fromString("5d3f9f0e-2f55-4a0b-9d9e-2b1d6c0a7e11");
    /** Extra max health while a demon (boss-bar sized health pool). */
    private static final double BONUS_HEALTH = 180.0;
    private static final int TRANSFORM_TICKS = 60;


    private static class State {
        int gauge;
        int phase;
        int phaseTicks;
        int busy;
        int attackTicks;
        AbilityType attack;
        UUID avatar;
        ServerBossEvent bar;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private static State state(ServerPlayer p) {
        return STATES.computeIfAbsent(p.getUUID(), k -> new State());
    }

    // ------------------------------------------------------------------ queries
    public static boolean isHero(net.minecraft.world.entity.player.Player p) {
        State s = STATES.get(p.getUUID());
        return s != null && s.phase == 2;
    }

    public static boolean canAct(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        return s != null && s.phase == 2 && s.busy <= 0;
    }

    public static void sync(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        int gauge = s == null ? 0 : s.gauge;
        int remaining = (s != null && s.phase == 2) ? Math.max(0, HeroConstants.DEMON_TICKS - s.phaseTicks) : 0;
        Network.sendState(p, PowerData.has(p), gauge, remaining);
    }

    // ------------------------------------------------------------------ gauge
    public static void addGauge(ServerPlayer p, int amount) {
        State s = state(p);
        if (s.phase != 0 || amount <= 0) {
            return;
        }
        s.gauge = Math.min(HeroConstants.GAUGE_MAX, s.gauge + amount);
        if (s.gauge >= HeroConstants.GAUGE_MAX) {
            startTransformation(p, s);
        }
        sync(p);
    }

    private static void startTransformation(ServerPlayer p, State s) {
        s.phase = 1;
        s.phaseTicks = 0;
        p.setInvulnerable(true);
        // root the player for the transformation
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TRANSFORM_TICKS + 10, 9, false, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, TRANSFORM_TICKS + 10, 128, false, false, false));
        p.displayClientMessage(Component.translatable("message.rudues.hero_awakened"), true);
    }

    // ------------------------------------------------------------------ per-tick
    public static void tick(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s == null || s.phase == 0) {
            return;
        }
        ServerLevel level = p.serverLevel();
        if (s.phase == 1) {
            tickTransform(p, s, level);
        } else {
            tickDemon(p, s, level);
        }
    }

    private static void tickTransform(ServerPlayer p, State s, ServerLevel level) {
        int t = ++s.phaseTicks;
        p.fallDistance = 0;

        // pillar of purple flame engulfing the player
        double h = Math.min(14.0, t * 2.0);
        for (double y = 0; y <= h; y += 1.2) {
            double spread = 1.0 - Math.min(0.5, y / 30.0);
            level.sendParticles(ModParticles.PURPLE_FLAME_BIG.get(), p.getX(), p.getY() + y, p.getZ(), 4, spread, 0.4, spread, 0.02);
            level.sendParticles(ModParticles.PURPLE_FLAME.get(), p.getX(), p.getY() + y, p.getZ(), 2, spread, 0.4, spread, 0.04);
            if (((int) (y / 1.2)) % 2 == 0) {
                level.sendParticles(ModParticles.PURPLE_EMBER.get(), p.getX(), p.getY() + y, p.getZ(), 1, spread, 0.4, spread, 0.03);
            }
        }
        if (t == 1) {
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.2F, 0.7F);
        }
        if (t % 10 == 0) {
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.4F);
        }

        // the demon rises inside the pillar
        if (t == 45) {
            p.addEffect(new MobEffectInstance(ModEffects.HERO_OF_HELL.get(), -1, 0, false, false, false));
            p.refreshDimensions();
            DemonEntity demon = new DemonEntity(level, p, true);
            level.addFreshEntity(demon);
            s.avatar = demon.getUUID();
        }

        // pillar fades, blast wave, demon is out
        if (t >= TRANSFORM_TICKS) {
            s.phase = 2;
            s.phaseTicks = 0;
            s.busy = 0;
            s.attack = null;
            p.setInvulnerable(false);

            AttributeInstance hp = p.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null && hp.getModifier(HEALTH_MOD) == null) {
                hp.addTransientModifier(new AttributeModifier(HEALTH_MOD, "rudues_hero_of_hell", BONUS_HEALTH,
                        AttributeModifier.Operation.ADDITION));
            }
            p.setHealth(p.getMaxHealth());

            s.bar = new ServerBossEvent(Component.translatable("gui.rudues.hero_of_hell"),
                    BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
            s.bar.setDarkenScreen(false);
            s.bar.setProgress(1.0F);
            s.bar.addPlayer(p);

            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(10.0),
                    e -> e != p && !(e instanceof FiendEntity f && f.isOwner(p)))) {
                Vec3 away = new Vec3(e.getX() - p.getX(), 0, e.getZ() - p.getZ());
                away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
                e.hurt(p.damageSources().playerAttack(p), 20.0F);
                e.setSecondsOnFire(8);
                e.setDeltaMovement(e.getDeltaMovement().add(away.x * 2.2, 0.9, away.z * 2.2));
                e.hurtMarked = true;
            }
            FxUtil.purpleBurst(level, p.position().add(0, 2.0, 0), 260, 3.0);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, p.getX(), p.getY() + 1.0, p.getZ(), 1, 0, 0, 0, 0);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5F, 0.6F);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5F, 0.5F);
            sync(p);
        }
    }

    private static void tickDemon(ServerPlayer p, State s, ServerLevel level) {
        s.phaseTicks++;
        if (s.busy > 0) {
            s.busy--;
        }
        if (s.bar != null) {
            s.bar.setProgress(Mth.clamp(p.getHealth() / p.getMaxHealth(), 0.0F, 1.0F));
        }
        if (s.phaseTicks >= HeroConstants.DEMON_TICKS) {
            endForm(p, s, true);
            return;
        }

        // keep the visual demon alive (e.g. after changing dimension)
        Entity existing = s.avatar == null ? null : level.getEntity(s.avatar);
        if (existing == null || existing.isRemoved()) {
            DemonEntity demon = new DemonEntity(level, p, false);
            level.addFreshEntity(demon);
            s.avatar = demon.getUUID();
        }

        // ember aura
        if (p.tickCount % 3 == 0) {
            level.sendParticles(ModParticles.PURPLE_FLAME.get(), p.getX(), p.getY() + 2.5, p.getZ(), 6, 1.2, 2.0, 1.2, 0.02);
            level.sendParticles(ModParticles.PURPLE_EMBER.get(), p.getX(), p.getY() + 0.3, p.getZ(), 2, 1.2, 0.1, 1.2, 0.02);
        }

        if (s.attack != null) {
            s.attackTicks++;
            switch (s.attack) {
                case CONCENTRATED_BEAM -> tickBeam(p, s, level);
                case OBLITERATOR_MAXIMUM -> tickPunch(p, s, level);
                case HELLFIRE -> tickHellfire(p, s, level);
                case SLASH_COMBO -> tickSlash(p, s, level);
                default -> s.attack = null;
            }
        }
    }

    // ------------------------------------------------------------------ ending
    private static void endForm(ServerPlayer p, State s, boolean natural) {
        cleanupVisuals(p, s);
        s.phase = 0;
        s.phaseTicks = 0;
        s.gauge = 0;
        s.attack = null;
        s.busy = 0;
        if (natural) {
            ServerLevel level = p.serverLevel();
            FxUtil.purpleBurst(level, p.position().add(0, 2.0, 0), 160, 2.0);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.0F, 1.5F);
            p.displayClientMessage(Component.translatable("message.rudues.hero_ended"), true);
        }
        sync(p);
    }

    private static void cleanupVisuals(ServerPlayer p, State s) {
        p.removeEffect(ModEffects.HERO_OF_HELL.get());
        p.setInvulnerable(false);
        AttributeInstance hp = p.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            hp.removeModifier(HEALTH_MOD);
        }
        if (p.getHealth() > p.getMaxHealth()) {
            p.setHealth(p.getMaxHealth());
        }
        if (s.bar != null) {
            s.bar.removeAllPlayers();
            s.bar = null;
        }
        if (s.avatar != null) {
            Entity e = p.serverLevel().getEntity(s.avatar);
            if (e != null) {
                e.discard();
            }
            s.avatar = null;
        }
        p.refreshDimensions();
    }

    /** Death / logout: drop everything silently. */
    public static void cleanup(ServerPlayer p) {
        State s = STATES.remove(p.getUUID());
        if (s != null) {
            cleanupVisuals(p, s);
        }
    }

    // ------------------------------------------------------------------ move dispatch
    public static void beginMove(ServerPlayer p, AbilityType type) {
        State s = STATES.get(p.getUUID());
        if (s == null || s.phase != 2) {
            return;
        }
        s.attack = type;
        s.attackTicks = 0;
        String anim;
        switch (type) {
            case CONCENTRATED_BEAM -> {
                s.busy = 76;
                anim = "beam";
                rootPlayer(p, 76);
            }
            case OBLITERATOR_MAXIMUM -> {
                s.busy = 18;
                anim = "obliterator";
            }
            case HELLFIRE -> {
                s.busy = 28;
                anim = "hellfire";
                rootPlayer(p, 26);
            }
            default -> {
                s.busy = 24;
                anim = "slash";
            }
        }
        if (s.avatar != null && p.serverLevel().getEntity(s.avatar) instanceof DemonEntity d) {
            d.triggerAnim("attack", anim);
        }
    }

    private static void rootPlayer(ServerPlayer p, int ticks) {
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9, false, false, false));
    }

    // ------------------------------------------------------------------ 1. Concentrated Beam (1s charge + 3s beam)
    private static void tickBeam(ServerPlayer p, State s, ServerLevel level) {
        int a = s.attackTicks;
        Vec3 look = p.getLookAngle();
        Vec3 origin = p.getEyePosition().add(look.scale(2.4)).subtract(0, 1.1, 0);

        if (a < 10) {
            if (a == 1) {
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1.5F, 0.6F);
            }
            if (a % 2 == 0) {
                double r = 3.0 * (1.0 - a / 10.0) + 0.4;
                for (int i = 0; i < 10; i++) {
                    Vec3 dir = new Vec3(p.getRandom().nextGaussian(), p.getRandom().nextGaussian(), p.getRandom().nextGaussian()).normalize();
                    Vec3 c = origin.add(dir.scale(r));
                    level.sendParticles(ModParticles.PURPLE_FLAME.get(), c.x, c.y, c.z, 1, 0, 0, 0, 0);
                }
                level.sendParticles(ModParticles.PURPLE_FLAME_BIG.get(), origin.x, origin.y, origin.z, 2, 0.2, 0.2, 0.2, 0);
            }
            return;
        }
        if (a == 10) {
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 2.0F, 0.6F);
        }
        if (a % 15 == 0) {
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8F, 1.5F);
        }

        double range = 36.0;
        Vec3 target = origin.add(look.scale(range));
        BlockHitResult hit = level.clip(new ClipContext(origin, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        boolean hitBlock = hit.getType() != HitResult.Type.MISS;
        Vec3 end = hitBlock ? hit.getLocation() : target;
        double len = end.subtract(origin).length();

        int i = 0;
        for (double d = 0; d <= len; d += 1.3, i++) {
            Vec3 pt = origin.add(look.scale(d));
            level.sendParticles(ModParticles.PURPLE_FLAME_BIG.get(), pt.x, pt.y, pt.z, 1, 0.25, 0.25, 0.25, 0);
            level.sendParticles(ModParticles.PURPLE_SPARK.get(), pt.x, pt.y, pt.z, 1, 0.5, 0.5, 0.5, 0.4);
            if (i % 3 == 0) {
                level.sendParticles(ModParticles.PURPLE_FLAME.get(), pt.x, pt.y, pt.z, 1, 0.3, 0.3, 0.3, 0.02);
            }
        }
        if (hitBlock) {
            FxUtil.purpleBurst(level, end, 14, 0.8);
        }

        if (a % 5 == 0) {
            AABB box = new AABB(origin, end).inflate(2.0);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> validTarget(e, p))) {
                Vec3 to = e.getBoundingBox().getCenter().subtract(origin);
                double proj = to.dot(look);
                if (proj < 0 || proj > len + 1.0 || to.subtract(look.scale(proj)).length() > 1.8) {
                    continue;
                }
                e.invulnerableTime = 0;
                e.hurt(p.damageSources().playerAttack(p), 9.0F);
                e.setSecondsOnFire(8);
                Vec3 c = e.getBoundingBox().getCenter();
                level.sendParticles(ModParticles.PURPLE_SPARK.get(), c.x, c.y, c.z, 20, 0.4, 0.5, 0.4, 0.4);
            }
        }
        if (hitBlock && a % 10 == 0) {
            level.explode(p, end.x, end.y, end.z, 2.5F, true, Level.ExplosionInteraction.NONE);
        }
        if (a >= 70) {
            s.attack = null;
        }
    }

    // ------------------------------------------------------------------ 2. Obliterator Maximum
    private static void tickPunch(ServerPlayer p, State s, ServerLevel level) {
        int a = s.attackTicks;
        Vec3 look = p.getLookAngle();
        Vec3 fist = p.getEyePosition().add(look.scale(3.8)).subtract(0, 1.2, 0);
        if (a < 9) {
            if (a % 2 == 0) {
                level.sendParticles(ModParticles.PURPLE_FLAME.get(), fist.x, fist.y, fist.z, 6, 0.6, 0.6, 0.6, 0.02);
            }
            return;
        }
        if (a == 9) {
            cone(p, level, 8.0, 0.3, 100.0F, 5.0, 15, true);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, fist.x, fist.y, fist.z, 1, 0, 0, 0, 0);
            level.sendParticles(ModParticles.PURPLE_FLAME.get(), fist.x, fist.y, fist.z, 60, 1.0, 1.0, 1.0, 0.15);
            level.sendParticles(ModParticles.PURPLE_SPARK.get(), fist.x, fist.y, fist.z, 100, 1.5, 1.5, 1.5, 0.7);
            FxUtil.purpleBurst(level, fist, 150, 1.5);
            level.explode(p, fist.x, fist.y, fist.z, 3.0F, true, Level.ExplosionInteraction.NONE);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5F, 0.7F);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        if (a >= 13) {
            s.attack = null;
        }
    }

    // ------------------------------------------------------------------ 3. Hellfire
    private static void tickHellfire(ServerPlayer p, State s, ServerLevel level) {
        int a = s.attackTicks;
        Vec3 look = p.getLookAngle();
        Vec3 origin = p.getEyePosition().add(look.scale(3.4)).subtract(0, 1.0, 0);
        if (a < 14) {
            if (a == 1) {
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.5F, 0.5F);
            }
            if (a % 2 == 0) {
                int n = 3 + a / 3;
                level.sendParticles(ModParticles.PURPLE_FLAME_BIG.get(), origin.x, origin.y, origin.z, n, 0.3 + a * 0.03, 0.3 + a * 0.03, 0.3 + a * 0.03, 0.01);
                level.sendParticles(ModParticles.PURPLE_SPARK.get(), origin.x, origin.y, origin.z, 2, 1.0, 1.0, 1.0, 0.3);
            }
            return;
        }
        if (a == 14) {
            RageBlastEntity ball = new RageBlastEntity(level, p, look.x, look.y, look.z);
            ball.setGiant(true);
            ball.setPos(origin.x, origin.y, origin.z);
            ball.setDeltaMovement(look.scale(1.1));
            level.addFreshEntity(ball);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 2.0F, 0.5F);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 2.0F, 0.4F);
            s.attack = null;
        }
    }

    // ------------------------------------------------------------------ 4. Slash Combo
    private static void tickSlash(ServerPlayer p, State s, ServerLevel level) {
        int a = s.attackTicks;
        int idx = switch (a) {
            case 4 -> 0;
            case 8 -> 1;
            case 12 -> 2;
            case 18 -> 3;
            default -> -1;
        };
        if (idx >= 0) {
            boolean finisher = idx == 3;
            cone(p, level, 6.5, 0.15, finisher ? 45.0F : 25.0F, finisher ? 3.0 : 0.9, finisher ? 10 : 5, false);
            slashArc(p, level, idx, finisher);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5F, finisher ? 0.5F : 0.7F);
            if (finisher) {
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 1.2F);
            }
        }
        if (a >= 22) {
            s.attack = null;
        }
    }

    private static void slashArc(ServerPlayer p, ServerLevel level, int idx, boolean finisher) {
        Vec3 look = p.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 center = p.getEyePosition().subtract(0, 1.2, 0);
        double tilt = (idx % 2 == 0) ? 1.0 : -1.0;
        double scale = finisher ? 1.5 : 1.0;
        for (int i = -10; i <= 10; i++) {
            double t = i / 10.0;
            Vec3 pt = center.add(look.scale((3.2 - 1.4 * t * t) * scale))
                    .add(right.scale(t * 3.4 * scale)).add(0, tilt * t * 1.8 * scale, 0);
            level.sendParticles(ModParticles.PURPLE_FLAME_BIG.get(), pt.x, pt.y, pt.z, 1, 0.1, 0.1, 0.1, 0);
            if (i % 2 == 0) {
                level.sendParticles(ModParticles.PURPLE_SPARK.get(), pt.x, pt.y, pt.z, 2, 0.2, 0.2, 0.2, 0.3);
                level.sendParticles(ModParticles.PURPLE_FLAME.get(), pt.x, pt.y, pt.z, 1, 0.1, 0.1, 0.1, 0.02);
            }
            if (i % 5 == 0) {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
            }
        }
    }

    // ------------------------------------------------------------------ helpers
    private static boolean validTarget(LivingEntity e, ServerPlayer p) {
        return e != p && e.isAlive() && !(e instanceof FiendEntity f && f.isOwner(p));
    }

    private static void cone(ServerPlayer p, ServerLevel level, double reach, double minDot,
                             float damage, double knockback, int fireSeconds, boolean electric) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        AABB box = p.getBoundingBox().expandTowards(look.scale(reach)).inflate(2.5);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> validTarget(x, p))) {
            Vec3 center = e.getBoundingBox().getCenter();
            Vec3 to = center.subtract(eye);
            double dist = to.length();
            if (dist > reach + 2.0 || to.normalize().dot(look) < minDot) {
                continue;
            }
            e.invulnerableTime = 0;
            e.hurt(p.damageSources().playerAttack(p), damage);
            e.setSecondsOnFire(fireSeconds);
            e.knockback(knockback, p.getX() - e.getX(), p.getZ() - e.getZ());
            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.5, 0));
            e.hurtMarked = true;
            FxUtil.purpleBurst(level, center, 40, 0.5);
            if (electric) {
                level.sendParticles(ModParticles.PURPLE_SPARK.get(), center.x, center.y, center.z, 50, 0.7, 0.7, 0.7, 0.6);
            }
        }
    }
}
