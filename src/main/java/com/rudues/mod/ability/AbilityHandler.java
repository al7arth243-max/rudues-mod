package com.rudues.mod.ability;

import com.rudues.mod.entity.FiendEntity;
import com.rudues.mod.entity.RageBlastEntity;
import com.rudues.mod.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-side execution of all abilities. */
public class AbilityHandler {
    private static final Map<UUID, long[]> COOLDOWNS = new HashMap<>();

    public static void clear(UUID id) {
        COOLDOWNS.remove(id);
    }

    public static void tryCast(ServerPlayer player, int index) {
        AbilityType type = AbilityType.byIndex(index);
        if (type == null || !PowerData.has(player)) {
            return;
        }
        long now = player.level().getGameTime();
        long[] cds = COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new long[AbilityType.values().length]);
        if (now < cds[index]) {
            long secs = (cds[index] - now + 19) / 20;
            player.displayClientMessage(Component.translatable("message.rudues.cooldown",
                    type.displayName(), String.valueOf(secs)), true);
            return;
        }
        cds[index] = now + type.cooldown;

        switch (type) {
            case RAGE_BLAST -> rageBlast(player);
            case OBLITERATOR -> obliterator(player);
            case FIEND_SUMMON -> fiendSummon(player);
        }
    }

    // ---------------------------------------------------------------- Rage Blast
    private static void rageBlast(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        RageBlastEntity blast = new RageBlastEntity(level, player, look.x, look.y, look.z);
        blast.setPos(player.getX() + look.x * 1.2, player.getEyeY() - 0.15 + look.y * 1.2, player.getZ() + look.z * 1.2);
        blast.setDeltaMovement(look.scale(1.4));
        level.addFreshEntity(blast);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2F, 0.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.5F);
        player.swing(InteractionHand.MAIN_HAND, true);
    }

    // ---------------------------------------------------------------- Obliterator
    private static void obliterator(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double reach = 4.0;
        AABB box = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.5);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !(e instanceof FiendEntity f && f.isOwner(player)))) {
            Vec3 to = target.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist > reach + 1.0 || to.normalize().dot(look) < 0.55) {
                continue;
            }
            target.hurt(player.damageSources().playerAttack(player), 30.0F);
            target.setSecondsOnFire(10);
            target.knockback(2.6, player.getX() - target.getX(), player.getZ() - target.getZ());
            target.setDeltaMovement(target.getDeltaMovement().add(0, 0.45, 0));
            target.hurtMarked = true;
            FxUtil.purpleBurst(level, target.getBoundingBox().getCenter(), 30, 0.4);
        }

        Vec3 fist = eye.add(look.scale(2.2));
        level.sendParticles(ParticleTypes.EXPLOSION, fist.x, fist.y, fist.z, 2, 0.2, 0.2, 0.2, 0);
        level.sendParticles(ParticleTypes.FLAME, fist.x, fist.y, fist.z, 25, 0.3, 0.3, 0.3, 0.1);
        FxUtil.purpleBurst(level, fist, 40, 0.5);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.3F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.6F);
        player.swing(InteractionHand.MAIN_HAND, true);
    }

    // ---------------------------------------------------------------- Fiend Summon
    private static void fiendSummon(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < 3; i++) {
            FiendEntity fiend = ModEntities.FIEND.get().create(level);
            if (fiend == null) {
                continue;
            }
            fiend.setOwner(player);
            double angle = (Math.PI * 2.0 / 3.0) * i + player.getRandom().nextDouble();
            fiend.moveTo(player.getX() + Math.cos(angle) * 1.5, player.getY() + 1.6, player.getZ() + Math.sin(angle) * 1.5,
                    player.getYRot(), 0.0F);
            level.addFreshEntity(fiend);
            FxUtil.purpleBurst(level, fiend.position().add(0, 0.4, 0), 12, 0.3);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.7F);
        player.swing(InteractionHand.MAIN_HAND, true);
    }
}
