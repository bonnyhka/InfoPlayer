package ua.bonny.infoplayer.server;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import ua.bonny.infoplayer.data.PlayerSummary;
import ua.bonny.infoplayer.network.DetailResponsePayload;
import ua.bonny.infoplayer.network.ListResponsePayload;

public final class PlayerInfoService {
    private PlayerInfoService() {
    }

    public static void sendList(ServerPlayer requester) {
        MinecraftServer server = requester.getServer();
        server.getPlayerList().getPlayers().forEach(player -> PlayerDataStore.capture(player, false));
        List<PlayerSummary> players = PlayerDataStore.all().stream()
                .map(player -> player.summary(server.getPlayerList().getPlayer(player.playerId()) != null))
                .sorted(Comparator.comparing(PlayerSummary::online).reversed()
                        .thenComparing(summary -> summary.profile().getName(), String.CASE_INSENSITIVE_ORDER))
                .toList();
        PacketDistributor.sendToPlayer(requester, new ListResponsePayload(isAdmin(requester), players));
    }

    public static void sendDetail(ServerPlayer requester, UUID playerId) {
        MinecraftServer server = requester.getServer();
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(playerId);
        StoredPlayer stored = onlinePlayer != null
                ? PlayerDataStore.capture(onlinePlayer, false)
                : PlayerDataStore.get(playerId);
        if (stored != null) {
            PacketDistributor.sendToPlayer(requester,
                    new DetailResponsePayload(
                            isAdmin(requester),
                            stored.detail(onlinePlayer != null, server.registryAccess())));
        }
    }

    public static void takeItem(ServerPlayer requester, UUID playerId, int slotIndex) {
        if (!isAdmin(requester)) {
            requester.sendSystemMessage(Component.translatable("message.infoplayer.no_permission_action"));
            return;
        }
        if (slotIndex < 0 || slotIndex >= 41) {
            return;
        }

        ServerPlayer target = requester.getServer().getPlayerList().getPlayer(playerId);
        if (target == null) {
            requester.sendSystemMessage(Component.translatable("message.infoplayer.player_offline"));
            return;
        }
        if (target == requester) {
            requester.sendSystemMessage(Component.translatable("message.infoplayer.cannot_take_self"));
            return;
        }

        ItemStack source = target.getInventory().getItem(slotIndex);
        if (source.isEmpty()) {
            sendDetail(requester, playerId);
            return;
        }

        int originalCount = source.getCount();
        ItemStack moving = source.copy();
        moveToInventory(requester, moving);
        int movedCount = originalCount - moving.getCount();
        if (movedCount <= 0) {
            requester.sendSystemMessage(Component.translatable("message.infoplayer.inventory_full"));
            return;
        }

        ItemStack removed = target.getInventory().removeItem(slotIndex, movedCount);
        requester.containerMenu.broadcastChanges();
        target.containerMenu.broadcastChanges();
        PlayerDataStore.capture(requester, false);
        PlayerDataStore.capture(target, false);
        PlayerDataStore.save();

        requester.sendSystemMessage(Component.translatable(
                "message.infoplayer.item_taken",
                removed.getHoverName(),
                movedCount,
                target.getGameProfile().getName()));
        sendDetail(requester, playerId);
    }

    private static void moveToInventory(ServerPlayer player, ItemStack remaining) {
        for (ItemStack existing : player.getInventory().items) {
            if (remaining.isEmpty()) {
                return;
            }
            if (!existing.isEmpty()
                    && ItemStack.isSameItemSameComponents(existing, remaining)
                    && existing.getCount() < existing.getMaxStackSize()) {
                int moved = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(moved);
                remaining.shrink(moved);
            }
        }

        for (int index = 0; index < player.getInventory().items.size() && !remaining.isEmpty(); index++) {
            if (player.getInventory().items.get(index).isEmpty()) {
                int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                ItemStack inserted = remaining.copy();
                inserted.setCount(moved);
                player.getInventory().items.set(index, inserted);
                remaining.shrink(moved);
            }
        }
    }

    private static boolean isAdmin(ServerPlayer player) {
        return player.createCommandSourceStack().hasPermission(2);
    }
}
