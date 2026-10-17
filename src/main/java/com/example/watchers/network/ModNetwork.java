package com.example.watchers.network;

import com.example.watchers.WatchersMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(WatchersMod.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(StatePacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StatePacket::encode)
                .decoder(StatePacket::decode)
                .consumerMainThread(StatePacket::handle)
                .add();
    }

    public static void sendState(ServerPlayer player, int mode) {
        if (player.connection != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StatePacket(mode));
        }
    }
}
