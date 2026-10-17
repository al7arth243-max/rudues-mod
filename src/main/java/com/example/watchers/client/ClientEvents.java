package com.example.watchers.client;

import com.example.watchers.ClientConfig;
import com.example.watchers.WatchersMod;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-side game events: ticking, fog and cleanup. */
@Mod.EventBusSubscriber(modid = WatchersMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientState.tick();
        }
    }

    /**
     * Pulls the fog in, but never closer than the configured distance (45 blocks by default),
     * so watchers 20-30 blocks away stay visible.
     */
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        float strength = ClientState.intensity;
        if (strength <= 0.001F) {
            return;
        }
        if (event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) {
            return;
        }
        if (event.getCamera().getFluidInCamera() != FogType.NONE) {
            return; // leave water and lava fog alone
        }

        float far = event.getFarPlaneDistance();
        float target = Math.min(far, ClientConfig.FOG_DISTANCE.get().floatValue());
        float newFar = Mth.lerp(strength, far, target);
        float newNear = Mth.lerp(strength, event.getNearPlaneDistance(), newFar * 0.15F);

        event.setFarPlaneDistance(newFar);
        event.setNearPlaneDistance(newNear);
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true); // required for Forge to apply the changes
    }

    /** Fades the fog (and so the far horizon) to near-black. */
    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float strength = ClientState.intensity;
        if (strength <= 0.001F) {
            return;
        }
        float t = Mth.clamp(strength * 0.9F, 0.0F, 1.0F);
        event.setRed(Mth.lerp(t, event.getRed(), 0.02F));
        event.setGreen(Mth.lerp(t, event.getGreen(), 0.02F));
        event.setBlue(Mth.lerp(t, event.getBlue(), 0.03F));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientState.reset();
    }
}
