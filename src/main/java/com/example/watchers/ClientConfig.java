package com.example.watchers;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Visual and audio settings, only used on the player's own game. File: config/watchers-client.toml
 */
public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue FOG_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue DARKNESS_ALPHA;
    public static final ForgeConfigSpec.DoubleValue FADE_IN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue FADE_OUT_SECONDS;
    public static final ForgeConfigSpec.DoubleValue TRACK_VOLUME;
    public static final ForgeConfigSpec.BooleanValue STOP_VANILLA_MUSIC;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        FOG_DISTANCE = b.comment("How far you can see (in blocks) when the fog is at full strength. Keep it above the watcher spawn distance so you can still see them.")
                .defineInRange("fogDistance", 45.0D, 20.0D, 300.0D);
        DARKNESS_ALPHA = b.comment("How dark the screen gets at full strength (0.0 = not at all, 1.0 = black).")
                .defineInRange("darknessAlpha", 0.35D, 0.0D, 0.9D);
        FADE_IN_SECONDS = b.comment("Seconds for the fog and darkness to reach full strength.")
                .defineInRange("fadeInSeconds", 30.0D, 0.5D, 300.0D);
        FADE_OUT_SECONDS = b.comment("Seconds for the fog and darkness to disappear after leaving the zone.")
                .defineInRange("fadeOutSeconds", 10.0D, 0.5D, 300.0D);
        TRACK_VOLUME = b.comment("Volume of the haunting track (multiplied with your master volume).")
                .defineInRange("trackVolume", 1.0D, 0.0D, 1.0D);
        STOP_VANILLA_MUSIC = b.comment("Silence Minecraft's own music while the haunting is active.")
                .define("stopVanillaMusic", true);

        SPEC = b.build();
    }

    private ClientConfig() {
    }
}
