package com.example.watchers.zone;

import com.example.watchers.WatchersConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import javax.annotation.Nullable;

/**
 * Decides where the invisible haunted zones are.
 *
 * Nothing is stored. The world is cut into square cells, and a hash of (world seed, dimension, cell)
 * decides whether a cell holds a zone and where it sits. The same seed always gives the same zones,
 * and zones ignore biomes completely.
 *
 * Each zone is an irregular blob that always fits inside its own cell, so zones never overlap and
 * only one cell ever needs checking for a given position.
 */
public final class HauntedZones {
    /** A zone's edge can stretch to this multiple of its nominal radius. */
    private static final double EDGE = 1.2D;

    public record Zone(double cx, double cz, double radius, double phase1, double phase2) {

        /** Radius of the zone's edge in the direction of the given angle. Ranges from 0.8 to 1.2 of the nominal radius. */
        public double edgeRadius(double angle) {
            return radius * (1.0D + 0.12D * Math.sin(3.0D * angle + phase1) + 0.08D * Math.sin(5.0D * angle + phase2));
        }

        public boolean contains(double x, double z) {
            double dx = x - cx;
            double dz = z - cz;
            double d2 = dx * dx + dz * dz;
            double outer = radius * EDGE;
            if (d2 > outer * outer) {
                return false;
            }
            double inner = radius * 0.8D;
            if (d2 <= inner * inner) {
                return true;
            }
            return Math.sqrt(d2) <= edgeRadius(Math.atan2(dz, dx));
        }

        public double distanceToCenter(double x, double z) {
            return Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
        }
    }

    private HauntedZones() {
    }

    public static boolean isEnabled(ServerLevel level) {
        return WatchersConfig.DIMENSIONS.get().contains(level.dimension().location().toString());
    }

    /** True if the position is inside a haunted zone. Cheap enough to call every tick. */
    public static boolean isHaunted(ServerLevel level, double x, double z) {
        if (!isEnabled(level)) {
            return false;
        }
        int cell = cellSize();
        int cellX = Math.floorDiv((int) Math.floor(x), cell);
        int cellZ = Math.floorDiv((int) Math.floor(z), cell);
        Zone zone = zoneOfCell(level, cellX, cellZ, cell);
        if (zone == null || !zone.contains(x, z)) {
            return false;
        }
        return !nearWorldSpawn(level, x, z);
    }

    /** The zone containing this position, or null. */
    @Nullable
    public static Zone zoneAt(ServerLevel level, double x, double z) {
        if (!isEnabled(level)) {
            return null;
        }
        int cell = cellSize();
        Zone zone = zoneOfCell(level, Math.floorDiv((int) Math.floor(x), cell), Math.floorDiv((int) Math.floor(z), cell), cell);
        return zone != null && zone.contains(x, z) ? zone : null;
    }

    /** The zone whose centre is closest to the position (searching a few cells around), or null. */
    @Nullable
    public static Zone nearest(ServerLevel level, double x, double z) {
        if (!isEnabled(level)) {
            return null;
        }
        int cell = cellSize();
        int baseX = Math.floorDiv((int) Math.floor(x), cell);
        int baseZ = Math.floorDiv((int) Math.floor(z), cell);
        Zone best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                Zone zone = zoneOfCell(level, baseX + dx, baseZ + dz, cell);
                if (zone == null || nearWorldSpawn(level, zone.cx(), zone.cz())) {
                    continue;
                }
                double dist = zone.distanceToCenter(x, z);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = zone;
                }
            }
        }
        return best;
    }

    private static boolean nearWorldSpawn(ServerLevel level, double x, double z) {
        int safe = WatchersConfig.SAFE_SPAWN_RADIUS.get();
        if (safe <= 0) {
            return false;
        }
        BlockPos spawn = level.getSharedSpawnPos();
        double dx = x - spawn.getX();
        double dz = z - spawn.getZ();
        return dx * dx + dz * dz < (double) safe * safe;
    }

    /** Cell size, grown if needed so that the biggest possible zone always fits inside one cell. */
    private static int cellSize() {
        int maxRadius = Math.max(WatchersConfig.MIN_RADIUS.get(), WatchersConfig.MAX_RADIUS.get());
        int needed = (int) Math.ceil(maxRadius * 2.0D * EDGE) + 16;
        return Math.max(WatchersConfig.ZONE_CELL_SIZE.get(), needed);
    }

    @Nullable
    private static Zone zoneOfCell(ServerLevel level, int cellX, int cellZ, int cell) {
        long dimHash = level.dimension().location().toString().hashCode();
        long h = mix(level.getSeed() ^ mix(dimHash * 0x9E3779B97F4A7C15L
                + cellX * 0xC2B2AE3D27D4EB4FL
                + cellZ * 0x165667B19E3779F9L));

        if (unit(h, 1) >= WatchersConfig.ZONE_CHANCE.get()) {
            return null;
        }

        double minR = Math.min(WatchersConfig.MIN_RADIUS.get(), WatchersConfig.MAX_RADIUS.get());
        double maxR = Math.max(WatchersConfig.MIN_RADIUS.get(), WatchersConfig.MAX_RADIUS.get());
        double radius = minR + (maxR - minR) * unit(h, 2);
        double extent = radius * EDGE;
        double room = cell - 2.0D * extent;
        double cx = cellX * (double) cell + extent + unit(h, 3) * room;
        double cz = cellZ * (double) cell + extent + unit(h, 4) * room;
        return new Zone(cx, cz, radius, unit(h, 5) * Math.PI * 2.0D, unit(h, 6) * Math.PI * 2.0D);
    }

    /** SplitMix64 finaliser. */
    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** A repeatable pseudo-random number in [0, 1) for the given hash and salt. */
    private static double unit(long hash, int salt) {
        long v = mix(hash + salt * 0x9E3779B97F4A7C15L);
        return (v >>> 11) * 0x1.0p-53D;
    }
}
