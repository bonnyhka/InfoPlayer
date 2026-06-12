package ua.bonny.infoplayer.server;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import ua.bonny.infoplayer.data.PlayerSummary;
import ua.bonny.infoplayer.network.DetailResponsePayload;
import ua.bonny.infoplayer.network.ListResponsePayload;

public final class PlayerInfoService {
    private PlayerInfoService() {
    }

    public static void sendList(ServerPlayer requester) {
        if (!isAdmin(requester)) {
            requester.sendSystemMessage(Component.translatable("message.infoplayer.no_permission"));
            return;
        }

        MinecraftServer server = requester.getServer();
        server.getPlayerList().getPlayers().forEach(player -> PlayerDataStore.capture(player, false));
        List<PlayerSummary> players = PlayerDataStore.all().stream()
                .map(player -> player.summary(server.getPlayerList().getPlayer(player.playerId()) != null))
                .sorted(Comparator.comparing(PlayerSummary::online).reversed()
                        .thenComparing(summary -> summary.profile().getName(), String.CASE_INSENSITIVE_ORDER))
                .toList();
        PacketDistributor.sendToPlayer(requester, new ListResponsePayload(players));
    }

    public static void sendDetail(ServerPlayer requester, UUID playerId) {
        if (!isAdmin(requester)) {
            requester.sendSystemMessage(Component.translatable("message.infoplayer.no_permission"));
            return;
        }

        MinecraftServer server = requester.getServer();
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(playerId);
        StoredPlayer stored = onlinePlayer != null
                ? PlayerDataStore.capture(onlinePlayer, false)
                : PlayerDataStore.get(playerId);
        if (stored != null) {
            PacketDistributor.sendToPlayer(requester,
                    new DetailResponsePayload(stored.detail(onlinePlayer != null, server.registryAccess())));
        }
    }

    private static boolean isAdmin(ServerPlayer player) {
        return player.createCommandSourceStack().hasPermission(2);
    }
}
