package com.example.watchers.registry;

import com.example.watchers.WatchersMod;
import com.example.watchers.entity.WatcherEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, WatchersMod.MODID);

    /** 1.5 wide, 4.8 tall. Never saved to disk and cannot be summoned by hand. */
    public static final RegistryObject<EntityType<WatcherEntity>> WATCHER = ENTITIES.register("watcher",
            () -> EntityType.Builder.of(WatcherEntity::new, MobCategory.MISC)
                    .sized(1.5F, 4.8F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .fireImmune()
                    .noSave()
                    .noSummon()
                    .build("watcher"));

    private ModEntities() {
    }
}
