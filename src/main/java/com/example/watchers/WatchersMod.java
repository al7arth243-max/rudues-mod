package com.example.watchers;

import com.example.watchers.entity.WatcherEntity;
import com.example.watchers.network.ModNetwork;
import com.example.watchers.registry.ModEntities;
import com.example.watchers.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * The Watchers.
 *
 * Invisible haunted zones are scattered through the world. Stay inside one long enough and
 * watchers start to appear and stare at you. When the last one arrives, you die.
 */
@Mod(WatchersMod.MODID)
public class WatchersMod {
    public static final String MODID = "watchers";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WatchersMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntities.ENTITIES.register(modBus);
        ModSounds.SOUNDS.register(modBus);

        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onEntityAttributes);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, WatchersConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }

    private void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.WATCHER.get(), WatcherEntity.createAttributes().build());
    }
}
