package com.example.watchers.command;

import com.example.watchers.WatchersMod;
import com.example.watchers.server.ZoneManager;
import com.example.watchers.zone.HauntedZones;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Testing helpers (operators only):
 *   /watchers info            where you are, and how far the haunting has got
 *   /watchers nearest         where the closest haunted zone is
 *   /watchers tp              teleport to the centre of the closest haunted zone
 *   /watchers skip <seconds>  add time to your exposure (only works inside a zone)
 *   /watchers reset           clear the haunting right now
 */
@Mod.EventBusSubscriber(modid = WatchersMod.MODID)
public final class WatchersCommand {
    private WatchersCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("watchers")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("info").executes(ctx -> info(ctx.getSource())))
                .then(Commands.literal("nearest").executes(ctx -> nearest(ctx.getSource())))
                .then(Commands.literal("tp").executes(ctx -> teleport(ctx.getSource())))
                .then(Commands.literal("reset").executes(ctx -> reset(ctx.getSource())))
                .then(Commands.literal("skip")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                                .executes(ctx -> skip(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "seconds"))))));
    }

    private static int info(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        HauntedZones.Zone zone = ZoneManager.currentZone(player);
        double seconds = ZoneManager.getExposureTicks(player.getUUID()) / 20.0D;
        int watchers = ZoneManager.getWatcherCount(player.getUUID());
        String where = zone == null
                ? "outside any haunted zone"
                : String.format("INSIDE a haunted zone (centre %d, %d, radius about %d)",
                (int) zone.cx(), (int) zone.cz(), (int) zone.radius());
        source.sendSuccess(() -> Component.literal(String.format(
                "You are %s. Exposure: %.1fs. Watchers: %d.", where, seconds, watchers)), false);
        return 1;
    }

    private static int nearest(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        HauntedZones.Zone zone = HauntedZones.nearest(player.serverLevel(), player.getX(), player.getZ());
        if (zone == null) {
            source.sendFailure(Component.literal("No haunted zone found nearby (or zones are disabled in this dimension)."));
            return 0;
        }
        int distance = (int) zone.distanceToCenter(player.getX(), player.getZ());
        source.sendSuccess(() -> Component.literal(String.format(
                "Nearest haunted zone: centre %d, %d (radius about %d), %d blocks away. Use /watchers tp to go there.",
                (int) zone.cx(), (int) zone.cz(), (int) zone.radius(), distance)), false);
        return 1;
    }

    private static int teleport(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        HauntedZones.Zone zone = HauntedZones.nearest(level, player.getX(), player.getZ());
        if (zone == null) {
            source.sendFailure(Component.literal("No haunted zone found nearby (or zones are disabled in this dimension)."));
            return 0;
        }
        int x = (int) Math.floor(zone.cx());
        int z = (int) Math.floor(zone.cz());
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        player.teleportTo(x + 0.5D, y, z + 0.5D);
        source.sendSuccess(() -> Component.literal(String.format(
                "Teleported to the centre of a haunted zone at %d, %d. (If you landed in water, swim to land: the haunting works on any terrain.)", x, z)), false);
        return 1;
    }

    private static int skip(CommandSourceStack source, int seconds) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!ZoneManager.addExposure(player, seconds)) {
            source.sendFailure(Component.literal("You are not inside a haunted zone. Use /watchers tp first."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Added " + seconds + "s of exposure."), false);
        return 1;
    }

    private static int reset(CommandSourceStack source) throws CommandSyntaxException {
        ZoneManager.reset(source.getPlayerOrException());
        source.sendSuccess(() -> Component.literal("Haunting cleared."), false);
        return 1;
    }
}
