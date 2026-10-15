package com.rudues.mod.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** Stores the "has Fire Fiend power" flag in the player's persistent data (survives death/relog). */
public class PowerData {
    private static final String KEY = "rudues_fire_fiend_power";
    private static final int AMPLIFIER = 4; // amplifier 4 == level V
    private static final MobEffect[] EFFECTS = {MobEffects.DAMAGE_BOOST, MobEffects.DAMAGE_RESISTANCE, MobEffects.MOVEMENT_SPEED};

    public static boolean has(Player player) {
        CompoundTag root = player.getPersistentData();
        return root.contains(Player.PERSISTED_NBT_TAG, 10)
                && root.getCompound(Player.PERSISTED_NBT_TAG).getBoolean(KEY);
    }

    public static void grant(Player player) {
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putBoolean(KEY, true);
        root.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    /** Keeps Strength V, Resistance V and Speed V permanently active (infinite duration). */
    public static void applyEffects(Player player) {
        for (MobEffect effect : EFFECTS) {
            MobEffectInstance current = player.getEffect(effect);
            boolean ok = current != null && current.getAmplifier() >= AMPLIFIER && current.getDuration() == -1;
            if (!ok) {
                if (current != null) {
                    player.removeEffect(effect);
                }
                player.addEffect(new MobEffectInstance(effect, -1, AMPLIFIER, false, false, true));
            }
        }
    }
}
