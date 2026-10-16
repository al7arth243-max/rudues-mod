package com.rudues.mod.client;

import com.rudues.mod.RuduesMod;
import com.rudues.mod.network.CastAbilityPacket;
import com.rudues.mod.network.Network;
import com.rudues.mod.registry.ModEffects;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RuduesMod.MODID, value = Dist.CLIENT)
public class ClientForgeEvents {
    private static boolean wheelWasDown = false;
    private static boolean wasDemon = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            wasDemon = false;
            return;
        }

        if (ClientPowerState.heroTicks > 0) {
            ClientPowerState.heroTicks--;
        }

        // when the local player turns into (or back from) the demon: resize hitbox, show them the demon
        boolean demon = mc.player.hasEffect(ModEffects.HERO_OF_HELL.get());
        if (demon != wasDemon) {
            wasDemon = demon;
            mc.player.refreshDimensions();
            if (demon) {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
        }

        // hold R -> skill wheel (rising edge only, so it does not reopen while R is still held)
        boolean rDown = ClientKeys.isHeld(ClientKeys.OPEN_WHEEL);
        if (rDown && !wheelWasDown && mc.screen == null && ClientPowerState.hasPowers) {
            mc.setScreen(new SkillWheelScreen());
        }
        wheelWasDown = rDown;
        while (ClientKeys.OPEN_WHEEL.consumeClick()) {
            // drain
        }

        // universal cast button
        while (ClientKeys.CAST_SELECTED.consumeClick()) {
            if (mc.screen == null && ClientPowerState.hasPowers) {
                Network.CHANNEL.sendToServer(new CastAbilityPacket(ClientPowerState.getSelected().ordinal()));
            }
        }
    }

    /** The player is drawn as the GeckoLib demon instead of the normal player model. */
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (event.getEntity().hasEffect(ModEffects.HERO_OF_HELL.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.hasEffect(ModEffects.HERO_OF_HELL.get())) {
            event.setCanceled(true);
        }
    }
}
