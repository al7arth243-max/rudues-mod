package com.rudues.mod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.rudues.mod.ability.AbilityType;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

/** Per-ability key binds chosen from the skill wheel. Saved to config/rudues-keybinds.properties. */
public class AbilityBindings {
    private static final int[] KEYS = new int[AbilityType.values().length];
    private static boolean loaded = false;

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rudues-keybinds.properties");
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        Arrays.fill(KEYS, -1);
        Path f = file();
        if (Files.exists(f)) {
            try (InputStream in = Files.newInputStream(f)) {
                Properties p = new Properties();
                p.load(in);
                for (AbilityType t : AbilityType.values()) {
                    KEYS[t.ordinal()] = Integer.parseInt(p.getProperty(t.id, "-1"));
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static int get(int index) {
        ensureLoaded();
        return KEYS[index];
    }

    public static void set(int index, int key) {
        ensureLoaded();
        for (int i = 0; i < KEYS.length; i++) {
            if (KEYS[i] == key) {
                KEYS[i] = -1; // one key, one ability
            }
        }
        KEYS[index] = key;
        save();
    }

    private static void save() {
        Properties p = new Properties();
        for (AbilityType t : AbilityType.values()) {
            p.setProperty(t.id, String.valueOf(KEYS[t.ordinal()]));
        }
        try (OutputStream out = Files.newOutputStream(file())) {
            p.store(out, "Rudues Mod ability key binds (GLFW key codes, -1 = unbound)");
        } catch (Exception ignored) {
        }
    }

    public static Component keyName(int key) {
        if (key < 0) {
            return Component.translatable("gui.rudues.unbound");
        }
        return InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName();
    }
}
