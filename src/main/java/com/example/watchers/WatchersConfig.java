package com.example.watchers;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * Gameplay settings. File: config/watchers-common.toml
 * On a server these are read from the server's config folder.
 */
public final class WatchersConfig {
    public static final ForgeConfigSpec SPEC;

    // zones
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DIMENSIONS;
    public static final ForgeConfigSpec.IntValue ZONE_CELL_SIZE;
    public static final ForgeConfigSpec.DoubleValue ZONE_CHANCE;
    public static final ForgeConfigSpec.IntValue MIN_RADIUS;
    public static final ForgeConfigSpec.IntValue MAX_RADIUS;
    public static final ForgeConfigSpec.IntValue SAFE_SPAWN_RADIUS;

    // haunting
    public static final ForgeConfigSpec.DoubleValue CLUE_DELAY_SECONDS;
    public static final ForgeConfigSpec.DoubleValue WATCHER_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.IntValue MAX_WATCHERS;
    public static final ForgeConfigSpec.DoubleValue DEATH_DELAY_SECONDS;
    public static final ForgeConfigSpec.DoubleValue DRAIN_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SPAWN_MIN_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue SPAWN_MAX_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue FINAL_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue FORCE_LOOK_AT_FINAL;
    public static final ForgeConfigSpec.BooleanValue EXEMPT_CREATIVE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Where the invisible haunted zones are").push("zones");
        DIMENSIONS = b.comment("Dimensions that contain haunted zones.")
                .defineListAllowEmpty("dimensions", List.of("minecraft:overworld"), o -> o instanceof String);
        ZONE_CELL_SIZE = b.comment("The world is split into square cells of this size (in blocks). Each cell holds at most one zone.")
                .defineInRange("cellSize", 768, 256, 16384);
        ZONE_CHANCE = b.comment("Chance (0.0 - 1.0) that a cell contains a zone.")
                .defineInRange("chance", 0.30D, 0.0D, 1.0D);
        MIN_RADIUS = b.comment("Smallest zone radius in blocks. Zone edges are slightly irregular.")
                .defineInRange("minRadius", 200, 40, 4000);
        MAX_RADIUS = b.comment("Largest zone radius in blocks.")
                .defineInRange("maxRadius", 300, 40, 4000);
        SAFE_SPAWN_RADIUS = b.comment("No zone is ever active this close to the world spawn, so new players are not haunted straight away.")
                .defineInRange("safeSpawnRadius", 300, 0, 100000);
        b.pop();

        b.comment("What happens to a player inside a zone").push("haunting");
        CLUE_DELAY_SECONDS = b.comment("Seconds inside a zone before the first clue: the track starts, fog rolls in and watcher #1 appears.")
                .defineInRange("clueDelaySeconds", 10.0D, 1.0D, 600.0D);
        WATCHER_INTERVAL_SECONDS = b.comment("Seconds between each further watcher.")
                .defineInRange("watcherIntervalSeconds", 20.0D, 1.0D, 600.0D);
        MAX_WATCHERS = b.comment("The player dies when this many watchers have appeared (the last one appears right in front of them).")
                .defineInRange("maxWatchers", 5, 2, 12);
        DEATH_DELAY_SECONDS = b.comment("Seconds between the last watcher appearing and the player dying.")
                .defineInRange("deathDelaySeconds", 1.0D, 0.0D, 30.0D);
        DRAIN_MULTIPLIER = b.comment("While outside a zone, exposure drains this many times faster than it builds up. Stops players resetting everything by stepping over the edge for a moment.")
                .defineInRange("drainMultiplier", 2.0D, 0.1D, 100.0D);
        SPAWN_MIN_DISTANCE = b.comment("Closest a normal watcher can appear to the player (blocks).")
                .defineInRange("spawnMinDistance", 20.0D, 6.0D, 100.0D);
        SPAWN_MAX_DISTANCE = b.comment("Furthest a normal watcher can appear from the player (blocks).")
                .defineInRange("spawnMaxDistance", 30.0D, 8.0D, 128.0D);
        FINAL_DISTANCE = b.comment("How far in front of the player the last watcher appears (blocks).")
                .defineInRange("finalDistance", 4.0D, 2.0D, 16.0D);
        FORCE_LOOK_AT_FINAL = b.comment("Turn the player's view towards the last watcher's face when it appears.")
                .define("forceLookAtFinal", true);
        EXEMPT_CREATIVE = b.comment("Creative mode players are never haunted. Spectators are always exempt.")
                .define("exemptCreative", true);
        b.pop();

        SPEC = b.build();
    }

    private WatchersConfig() {
    }
}
