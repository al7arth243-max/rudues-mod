package com.example.watchers.client;

import com.example.watchers.ClientConfig;
import com.example.watchers.WatchersMod;
import com.example.watchers.registry.ModEntities;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-side registration (mod event bus). */
@Mod.EventBusSubscriber(modid = WatchersMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.WATCHER.get(), WatcherRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WatcherModel.LAYER_LOCATION, WatcherModel::createBodyLayer);
    }

    /** A dark veil over the whole view (under the hotbar and other HUD), darker towards the top and bottom edges. */
    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.VIGNETTE.id(), "darkness", (gui, graphics, partialTick, width, height) -> {
            float strength = ClientState.intensity * ClientConfig.DARKNESS_ALPHA.get().floatValue();
            if (strength <= 0.003F) {
                return;
            }
            int veil = Mth.clamp((int) (strength * 255.0F), 0, 255) << 24;
            graphics.fill(0, 0, width, height, veil);

            int edge = Mth.clamp((int) (strength * 0.9F * 255.0F), 0, 255) << 24;
            int band = height / 4;
            graphics.fillGradient(0, 0, width, band, edge, 0);
            graphics.fillGradient(0, height - band, width, height, 0, edge);
        });
    }
}
