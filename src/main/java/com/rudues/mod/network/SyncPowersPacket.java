package com.rudues.mod.network;

import com.rudues.mod.client.ClientPowerState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncPowersPacket {
    private final boolean has;

    public SyncPowersPacket(boolean has) {
        this.has = has;
    }

    public static void encode(SyncPowersPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.has);
    }

    public static SyncPowersPacket decode(FriendlyByteBuf buf) {
        return new SyncPowersPacket(buf.readBoolean());
    }

    public static void handle(SyncPowersPacket msg, Supplier<NetworkEvent.Context> ctx) {
        // plain static field, no client-only classes touched -> safe on any side
        ClientPowerState.hasPowers = msg.has;
        ctx.get().setPacketHandled(true);
    }
}
