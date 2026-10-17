package com.example.watchers.client;

import com.example.watchers.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * What this player's screen and speakers are doing. Driven by messages from the server (see
 * {@link com.example.watchers.network.StatePacket}) and advanced once per client tick.
 */
public final class ClientState {
    /** True while the clue is active (inside a zone, past the first clue). */
    public static boolean active;
    /** True once the last watcher has appeared: everything stays at full strength. */
    public static boolean finalMode;
    /** 0 = nothing, 1 = full fog and darkness. Used by the fog and the darkening overlay. */
    public static float intensity;

    private static ClueSound sound;
    private static boolean soundEnded;
    private static int soundAge;

    private ClientState() {
    }

    public static void onPacket(int mode) {
        switch (mode) {
            case 0 -> {
                active = false;
                finalMode = false;
            }
            case 1 -> {
                if (!active) {
                    active = true;
                    startSound();
                }
            }
            case 2 -> {
                boolean wasActive = active;
                active = true;
                finalMode = true;
                intensity = 1.0F;
                if (!wasActive) {
                    startSound();
                }
            }
            case 3 -> reset();
            default -> {
            }
        }
    }

    public static void tick() {
        if (finalMode) {
            intensity = 1.0F;
        } else if (active) {
            intensity = Mth.clamp(intensity + 1.0F / (float) (ClientConfig.FADE_IN_SECONDS.get() * 20.0D), 0.0F, 1.0F);
        } else {
            intensity = Mth.clamp(intensity - 1.0F / (float) (ClientConfig.FADE_OUT_SECONDS.get() * 20.0D), 0.0F, 1.0F);
        }

        Minecraft mc = Minecraft.getInstance();
        if (sound != null) {
            soundAge++;
            if (sound.isStopped()) {
                sound = null;
            } else if (soundAge > 40 && !mc.getSoundManager().isActive(sound)) {
                // The track reached its end. Do not restart it until the haunting starts again.
                soundEnded = true;
            }
        }

        if (intensity > 0.001F && ClientConfig.STOP_VANILLA_MUSIC.get()) {
            mc.getMusicManager().stopPlaying();
        }
    }

    public static void reset() {
        active = false;
        finalMode = false;
        intensity = 0.0F;
        if (sound != null) {
            Minecraft.getInstance().getSoundManager().stop(sound);
            sound = null;
        }
        soundEnded = false;
    }

    private static void startSound() {
        if (sound != null && !sound.isStopped() && !soundEnded) {
            return; // still playing (it was only fading out)
        }
        soundEnded = false;
        soundAge = 0;
        sound = new ClueSound();
        Minecraft.getInstance().getSoundManager().play(sound);
    }
}
