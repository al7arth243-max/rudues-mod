package com.rudues.mod.registry;

import com.rudues.mod.RuduesMod;
import com.rudues.mod.entity.FiendEntity;
import com.rudues.mod.entity.RageBlastEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RuduesMod.MODID);

    public static final RegistryObject<EntityType<RageBlastEntity>> RAGE_BLAST = ENTITIES.register("rage_blast",
            () -> EntityType.Builder.<RageBlastEntity>of(RageBlastEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F).fireImmune().clientTrackingRange(4).updateInterval(10)
                    .build("rage_blast"));

    public static final RegistryObject<EntityType<FiendEntity>> FIEND = ENTITIES.register("fire_fiend_minion",
            () -> EntityType.Builder.<FiendEntity>of(FiendEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.7F).fireImmune().clientTrackingRange(8).updateInterval(2)
                    .build("fire_fiend_minion"));
}
