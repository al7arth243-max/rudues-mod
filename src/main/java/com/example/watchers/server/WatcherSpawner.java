package com.example.watchers.server;

import com.example.watchers.WatchersConfig;
import com.example.watchers.entity.WatcherEntity;
import com.example.watchers.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** Finds good places for watchers and creates them. */
public final class WatcherSpawner {
    private WatcherSpawner() {
    }

    /** Creates a watcher at the position, already turned to face its player. */
    @Nullable
    public static WatcherEntity spawn(ServerLevel level, ServerPlayer owner, double x, double y, double z) {
        WatcherEntity watcher = ModEntities.WATCHER.get().create(level);
        if (watcher == null) {
            return null;
        }
        float yaw = (float) (Mth.atan2(owner.getZ() - z, owner.getX() - x) * (180.0D / Math.PI)) - 90.0F;
        watcher.moveTo(x, y, z, yaw, 0.0F);
        watcher.setYHeadRot(yaw);
        watcher.yBodyRot = yaw;
        watcher.setWatchedPlayer(owner.getUUID());
        watcher.setPersistenceRequired();
        level.addFreshEntity(watcher);
        return watcher;
    }

    /**
     * A spot 20-30 blocks (configurable) from the player on solid ground. Spots outside the player's
     * field of view are preferred so a watcher is never seen popping into existence.
     */
    @Nullable
    public static Vec3 findFarSpot(ServerLevel level, ServerPlayer player, List<WatcherEntity> others) {
        RandomSource random = level.getRandom();
        double min = WatchersConfig.SPAWN_MIN_DISTANCE.get();
        double max = Math.max(min + 1.0D, WatchersConfig.SPAWN_MAX_DISTANCE.get());

        Vec3 look = player.getLookAngle();
        double lx = look.x;
        double lz = look.z;
        double len = Math.sqrt(lx * lx + lz * lz);
        if (len > 1.0E-3D) {
            lx /= len;
            lz /= len;
        }

        for (int attempt = 0; attempt < 40; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double dist = min + random.nextDouble() * (max - min);
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);

            // For the first attempts, stay out of the player's view.
            if (attempt < 28 && dx * lx + dz * lz > 0.45D) {
                continue;
            }

            Vec3 ground = findGround(level, player.getX() + dx * dist, player.getZ() + dz * dist, player.getY());
            if (ground == null) {
                continue;
            }

            boolean crowded = false;
            for (WatcherEntity other : others) {
                if (other.distanceToSqr(ground.x, ground.y, ground.z) < 36.0D) {
                    crowded = true;
                    break;
                }
            }
            if (!crowded) {
                return ground;
            }
        }
        return null;
    }

    /**
     * Finds somewhere to stand at (x, z), searching from a little above to well below the given height.
     * The floor must be solid (and not leaves), with no water at the feet and room for the head.
     */
    @Nullable
    public static Vec3 findGround(ServerLevel level, double x, double z, double aroundY) {
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        if (!level.hasChunk(bx >> 4, bz >> 4)) {
            return null;
        }

        int top = Math.min(Mth.floor(aroundY) + 12, level.getMaxBuildHeight() - 3);
        int bottom = Math.max(Mth.floor(aroundY) - 16, level.getMinBuildHeight() + 1);

        BlockPos.MutableBlockPos feet = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos head = new BlockPos.MutableBlockPos();

        for (int y = top; y >= bottom; y--) {
            feet.set(bx, y, bz);
            below.set(bx, y - 1, bz);
            head.set(bx, y + 1, bz);

            BlockState feetState = level.getBlockState(feet);
            if (!feetState.getCollisionShape(level, feet).isEmpty() || !feetState.getFluidState().isEmpty()) {
                continue;
            }
            if (!level.getBlockState(head).getCollisionShape(level, head).isEmpty()) {
                continue;
            }
            BlockState belowState = level.getBlockState(below);
            if (belowState.getCollisionShape(level, below).isEmpty()
                    || !belowState.getFluidState().isEmpty()
                    || belowState.is(BlockTags.LEAVES)) {
                continue;
            }
            return new Vec3(bx + 0.5D, y, bz + 0.5D);
        }
        return null;
    }
}
