package ua.bonny.infoplayer.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import ua.bonny.infoplayer.client.screen.PlayerDetailScreen;
import ua.bonny.infoplayer.client.screen.PlayerListScreen;
import ua.bonny.infoplayer.client.screen.SettingsScreen;
import ua.bonny.infoplayer.network.DetailResponsePayload;
import ua.bonny.infoplayer.network.ListResponsePayload;
import ua.bonny.infoplayer.network.ListRequestPayload;
import ua.bonny.infoplayer.network.DetailRequestPayload;
import ua.bonny.infoplayer.network.PrivacyChangedPayload;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {
    }

    public static void handleList(ListResponsePayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof PlayerListScreen screen) {
            screen.updatePlayers(
                    payload.administrator(),
                    payload.ownVisibleMask(),
                    payload.players());
        } else if (minecraft.screen instanceof SettingsScreen screen) {
            screen.updateSettings(
                    payload.administrator(),
                    payload.ownVisibleMask(),
                    payload.players());
        } else if (minecraft.screen instanceof PlayerDetailScreen screen
                && screen.parentScreen() instanceof PlayerListScreen parent) {
            parent.updatePlayers(
                    payload.administrator(),
                    payload.ownVisibleMask(),
                    payload.players());
        } else {
            minecraft.setScreen(new PlayerListScreen(
                    payload.administrator(),
                    payload.ownVisibleMask(),
                    payload.players()));
        }
    }

    public static void handleDetail(DetailResponsePayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof PlayerDetailScreen screen) {
            minecraft.setScreen(new PlayerDetailScreen(
                    screen.parentScreen(),
                    payload.administrator(),
                    payload.visibleMask(),
                    payload.player(),
                    screen.selectedTab()));
        } else {
            minecraft.setScreen(new PlayerDetailScreen(
                    minecraft.screen,
                    payload.administrator(),
                    payload.visibleMask(),
                    payload.player()));
        }
    }

    public static void handlePrivacyChanged(PrivacyChangedPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof PlayerDetailScreen screen
                && screen.playerId().equals(payload.playerId())) {
            PacketDistributor.sendToServer(new DetailRequestPayload(payload.playerId()));
            PacketDistributor.sendToServer(new ListRequestPayload());
        } else if (minecraft.screen instanceof PlayerListScreen
                || minecraft.screen instanceof SettingsScreen) {
            PacketDistributor.sendToServer(new ListRequestPayload());
        }
    }
}
