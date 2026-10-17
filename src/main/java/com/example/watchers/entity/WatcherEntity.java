package com.example.watchers.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * A watcher. It never moves, never attacks and cannot be hurt. It only turns to face the one
 * player it was created for, and only that player can see it.
 *
 * Watchers are created and removed by {@link com.example.watchers.server.ZoneManager}; they are
 * never saved to disk. One that has lost its player removes itself after two seconds.
 */
public class WatcherEntity extends Mob {
    /** Height of the face above the entity's feet, in blocks (matches the model). */
    public static final double FACE_HEIGHT = 4.19D;

    private static final EntityDataAccessor<Optional<UUID>> WATCHED_PLAYER =
            SynchedEntityData.defineId(WatcherEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private boolean pendingRemoval;
    private int removalTicks;
    private int orphanTicks;

    public WatcherEntity(EntityType<? extends WatcherEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(WATCHED_PLAYER, Optional.empty());
    }

    public Optional<UUID> getWatchedPlayer() {
        return this.entityData.get(WATCHED_PLAYER);
    }

    public void setWatchedPlayer(UUID id) {
        this.entityData.set(WATCHED_PLAYER, Optional.of(id));
    }

    /**
     * Asks the watcher to disappear. It waits until its player looks away (or five seconds pass)
     * so it never vanishes in front of their eyes.
     */
    public void markForRemoval() {
        this.pendingRemoval = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }

        Player found = null;
        Optional<UUID> id = getWatchedPlayer();
        if (id.isPresent()) {
            found = this.level().getPlayerByUUID(id.get());
        }
        if (!(found instanceof ServerPlayer player) || !player.isAlive()) {
            if (++this.orphanTicks > 40) {
                this.discard();
            }
            return;
        }
        this.orphanTicks = 0;

        // Stare at the player's eyes.
        Vec3 eye = player.getEyePosition();
        double dx = eye.x - this.getX();
        double dz = eye.z - this.getZ();
        double dy = eye.y - (this.getY() + FACE_HEIGHT);
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float pitch = (float) (-(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180.0D / Math.PI)));
        this.setRot(yaw, pitch);
        this.setYHeadRot(yaw);
        this.yBodyRot = yaw;

        if (this.pendingRemoval) {
            this.removalTicks++;
            if (this.removalTicks > 100 || !isInViewOf(player)) {
                this.discard();
            }
        }
    }

    private boolean isInViewOf(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 toMe = new Vec3(this.getX() - player.getX(),
                this.getY() + 2.0D - player.getEyeY(),
                this.getZ() - player.getZ());
        if (toMe.lengthSqr() < 1.0E-6D) {
            return true;
        }
        return look.dot(toMe.normalize()) > 0.6D;
    }

    // ------------------------------------------------------------------ makes it untouchable

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Only things that ignore invulnerability (/kill, the void) can remove it.
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
        // never pushed
    }

    @Override
    protected void doPush(Entity entity) {
        // never pushes anything either
    }
}
