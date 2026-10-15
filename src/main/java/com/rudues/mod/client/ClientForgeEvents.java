package com.rudues.mod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.rudues.mod.RuduesMod;
import com.rudues.mod.ability.AbilityType;
import com.rudues.mod.network.CastAbilityPacket;
import com.rudues.mod.network.Network;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RuduesMod.MODID, value = Dist.CLIENT)
public class ClientForgeEvents {
    private static boolean wheelWasDown = false;
    /** package-visible so the wheel screen can suppress an instant cast right after binding */
    static final boolean[] castWasDown = new boolean[AbilityType.values().length];

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        long window = mc.getWindow().getWindow();

        // Hold R -> skill wheel (rising edge only, so it does not reopen while R is still held)
        boolean rDown = InputConstants.isKeyDown(window, ClientKeys.OPEN_WHEEL.getKey().getValue());
        if (rDown && !wheelWasDown && mc.screen == null && ClientPowerState.hasPowers) {
            mc.setScreen(new SkillWheelScreen());
        }
        wheelWasDown = rDown;

        // Per-ability bound keys
        for (int i = 0; i < castWasDown.length; i++) {
            int key = AbilityBindings.get(i);
            boolean down = key > 0 && InputConstants.isKeyDown(window, key);
            if (down && !castWasDown[i] && mc.screen == null && ClientPowerState.hasPowers) {
                Network.CHANNEL.sendToServer(new CastAbilityPacket(i));
            }
            castWasDown[i] = down;
        }
    }
}
