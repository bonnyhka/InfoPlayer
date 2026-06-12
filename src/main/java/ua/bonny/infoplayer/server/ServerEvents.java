package ua.bonny.infoplayer.server;

import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

public final class ServerEvents {
    private ServerEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("infoplayer")
                .executes(context -> {
                    PlayerInfoService.sendList(context.getSource().getPlayerOrException());
                    return Command.SINGLE_SUCCESS;
                }));
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        PlayerDataStore.load(event.getServer());
        event.getServer().getPlayerList().getPlayers().forEach(player -> PlayerDataStore.capture(player, false));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerDataStore.capture(player, false);
            PlayerDataStore.save();
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerDataStore.capture(player, true);
            PlayerDataStore.save();
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        event.getServer().getPlayerList().getPlayers().forEach(player -> PlayerDataStore.capture(player, false));
        PlayerDataStore.save();
    }
}
