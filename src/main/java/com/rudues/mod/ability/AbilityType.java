package com.rudues.mod.ability;

import net.minecraft.network.chat.Component;

public enum AbilityType {
    RAGE_BLAST("rage_blast", 60),
    OBLITERATOR("obliterator", 40),
    FIEND_SUMMON("fiend_summon", 200);

    public final String id;
    /** cooldown in ticks */
    public final int cooldown;

    AbilityType(String id, int cooldown) {
        this.id = id;
        this.cooldown = cooldown;
    }

    public Component displayName() {
        return Component.translatable("ability.rudues." + id);
    }

    public Component description() {
        return Component.translatable("ability.rudues." + id + ".desc");
    }

    public static AbilityType byIndex(int i) {
        AbilityType[] v = values();
        return (i >= 0 && i < v.length) ? v[i] : null;
    }
}
