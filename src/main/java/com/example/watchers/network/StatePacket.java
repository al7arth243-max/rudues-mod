package com.example.watchers.network;

import com.example.watchers.client.ClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server to client: what the haunting looks and sounds like for this player.
 * mode 0 = fade everything out, 1 = clue active (track, fog, darkness), 2 = final scare (instant full strength),
 * 3 = reset instantly (death, respawn, dimension change).
 */
public record StatePacket(int mode) {

    public static void encode(StatePacket packet, FriendlyByteBuf buf) {
        buf.writeByte(packet.mode);
    }

    public static StatePacket decode(FriendlyByteBuf buf) {
        return new StatePacket(buf.readByte());
    }

    public static void handle(StatePacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientState.onPacket(packet.mode));
    }
}
