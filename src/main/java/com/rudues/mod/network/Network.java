package com.rudues.mod.network;

import com.rudues.mod.RuduesMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class Network {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RuduesMod.MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(CastAbilityPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CastAbilityPacket::encode).decoder(CastAbilityPacket::decode)
                .consumerMainThread(CastAbilityPacket::handle).add();
        CHANNEL.messageBuilder(SyncPowersPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncPowersPacket::encode).decoder(SyncPowersPacket::decode)
                .consumerMainThread(SyncPowersPacket::handle).add();
    }

    public static void sendPowers(ServerPlayer player, boolean has) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncPowersPacket(has));
    }
}
