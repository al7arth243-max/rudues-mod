package com.example.watchers.registry;

import com.example.watchers.WatchersMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

public final class ModDamage {
    /**
     * Defined in data/watchers/damage_type/watched.json and added to the "bypasses_*" tags in
     * data/minecraft/tags/damage_type, so totems of undying, armor and effects cannot stop it.
     */
    public static final ResourceKey<DamageType> WATCHED =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(WatchersMod.MODID, "watched"));

    public static DamageSource watched(Level level) {
        return new DamageSource(level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(WATCHED));
    }

    private ModDamage() {
    }
}
