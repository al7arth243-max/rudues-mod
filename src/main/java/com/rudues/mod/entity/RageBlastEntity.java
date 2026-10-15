package com.rudues.mod.entity;

import com.rudues.mod.ability.FxUtil;
import com.rudues.mod.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector3f;

public class RageBlastEntity extends AbstractHurtingProjectile {
    private static final DustParticleOptions PURPLE = new DustParticleOptions(new Vector3f(0.65F, 0.1F, 0.95F), 2.2F);

    public RageBlastEntity(EntityType<? extends RageBlastEntity> type, Level level) {
        super(type, level);
    }

    public RageBlastEntity(Level level, LivingEntity owner, double dx, double dy, double dz) {
        super(ModEntities.RAGE_BLAST.get(), owner, dx, dy, dz, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            for (int i = 0; i < 4; i++) {
                level().addParticle(PURPLE,
                        getX() + (random.nextDouble() - 0.5) * 0.5,
                        getY() + 0.3 + (random.nextDouble() - 0.5) * 0.5,
                        getZ() + (random.nextDouble() - 0.5) * 0.5, 0, 0, 0);
            }
            level().addParticle(ParticleTypes.WITCH, getX(), getY() + 0.3, getZ(), 0, 0, 0);
        } else if (tickCount > 120) {
            discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) {
            return;
        }
        Entity owner = getOwner();
        // Moderate explosion that sets fires and destroys blocks.
        level().explode(this, getX(), getY(), getZ(), 3.2F, true, Level.ExplosionInteraction.BLOCK);

        AABB area = getBoundingBox().inflate(4.5);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != owner && !(e instanceof FiendEntity f && owner != null && f.isOwner((LivingEntity) owner)))) {
            e.setSecondsOnFire(10);
        }
        if (level() instanceof ServerLevel sl) {
            FxUtil.purpleBurst(sl, position(), 80, 1.5);
        }
        discard();
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.DRAGON_BREATH;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }
}
