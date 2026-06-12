package ua.bonny.infoplayer.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ua.bonny.infoplayer.client.screen.PlayerDetailScreen;
import ua.bonny.infoplayer.client.screen.PlayerListScreen;
import ua.bonny.infoplayer.network.DetailResponsePayload;
import ua.bonny.infoplayer.network.ListResponsePayload;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {
    }

    public static void handleList(ListResponsePayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof PlayerListScreen screen) {
            screen.updatePlayers(payload.administrator(), payload.players());
        } else {
            minecraft.setScreen(new PlayerListScreen(payload.administrator(), payload.players()));
        }
    }

    public static void handleDetail(DetailResponsePayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof PlayerDetailScreen screen) {
            minecraft.setScreen(new PlayerDetailScreen(
                    screen.parentScreen(),
                    payload.administrator(),
                    payload.player(),
                    screen.selectedTab()));
        } else {
            minecraft.setScreen(new PlayerDetailScreen(minecraft.screen, payload.administrator(), payload.player()));
        }
    }
}
