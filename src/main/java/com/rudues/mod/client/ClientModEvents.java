package com.rudues.mod.client;

import com.rudues.mod.RuduesMod;
import com.rudues.mod.registry.ModEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RuduesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void onKeys(RegisterKeyMappingsEvent event) {
        event.register(ClientKeys.OPEN_WHEEL);
        event.register(ClientKeys.CAST_SELECTED);
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Rage Blast and Violent Gale are drawn purely with particles
        event.registerEntityRenderer(ModEntities.RAGE_BLAST.get(), ctx -> new NoopRenderer<>(ctx));
        event.registerEntityRenderer(ModEntities.WIND_SLASH.get(), ctx -> new NoopRenderer<>(ctx));
        event.registerEntityRenderer(ModEntities.FIEND.get(), FiendRenderer::new);
        event.registerEntityRenderer(ModEntities.DEMON.get(), DemonRenderer::new);
    }

    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FiendModel.LAYER, FiendModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.ARMOR_LEVEL.id(), "hero_gauge", new HeroGaugeOverlay());
    }
}
