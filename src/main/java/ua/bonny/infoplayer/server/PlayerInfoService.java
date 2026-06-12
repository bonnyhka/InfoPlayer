package ua.bonny.infoplayer.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import ua.bonny.infoplayer.data.PlayerDetail;
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
        PacketDistributor.sendToPlayer(requester, new ListResponsePayload(
                isAdmin(requester),
                InfoPlayerSettings.showCoordinatesToPlayers(),
                InfoPlayerSettings.showInventoryToPlayers(),
                players));
    }

    public static void sendDetail(ServerPlayer requester, UUID playerId) {
        MinecraftServer server = requester.getServer();
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(playerId);
        StoredPlayer stored = onlinePlayer != null
                ? PlayerDataStore.capture(onlinePlayer, false)
                : PlayerDataStore.get(playerId);
        if (stored != null) {
            boolean administrator = isAdmin(requester);
            boolean ownProfile = requester.getUUID().equals(playerId);
            boolean coordinatesVisible = administrator
                    || ownProfile
                    || InfoPlayerSettings.showCoordinatesToPlayers();
            boolean inventoryVisible = administrator
                    || ownProfile
                    || InfoPlayerSettings.showInventoryToPlayers();
            PlayerDetail detail = applyVisibility(
                    stored.detail(onlinePlayer != null, server.registryAccess()),
                    coordinatesVisible,
                    inventoryVisible);
            PacketDistributor.sendToPlayer(requester,
                    new DetailResponsePayload(
                            administrator,
                            coordinatesVisible,
                            inventoryVisible,
                            detail));
        }
    }

    public static void updateSettings(
            ServerPlayer requester,
            boolean showCoordinatesToPlayers,
            boolean showInventoryToPlayers) {
        if (!isAdmin(requester)) {
            requester.sendSystemMessage(Component.translatable("message.infoplayer.no_permission_settings"));
            return;
        }
        InfoPlayerSettings.update(showCoordinatesToPlayers, showInventoryToPlayers);
        requester.sendSystemMessage(Component.translatable("message.infoplayer.settings_saved"));
        sendList(requester);
    }

    public static void takeItem(
            ServerPlayer requester,
            UUID playerId,
            int slotIndex,
            String curiosType,
            boolean cosmetic) {
        if (!isAdmin(requester)) {
            requester.sendSystemMessage(Component.translatable("message.infoplayer.no_permission_action"));
            return;
        }
        boolean curioSlot = curiosType != null && !curiosType.isBlank();
        if (slotIndex < 0 || (!curioSlot && slotIndex >= 41)) {
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

        ItemStack source;
        if (curioSlot) {
            if (!ModList.get().isLoaded("curios")) {
                return;
            }
            source = CuriosCompatibility.getStack(target, curiosType, slotIndex, cosmetic);
        } else {
            source = target.getInventory().getItem(slotIndex);
        }
        if (source.isEmpty()) {
            sendDetail(requester, playerId);
            return;
        }

        int movedCount = Math.min(source.getCount(), availableInventorySpace(requester, source));
        if (movedCount <= 0) {
            requester.sendSystemMessage(Component.translatable("message.infoplayer.inventory_full"));
            return;
        }

        ItemStack removed = curioSlot
                ? CuriosCompatibility.extractStack(
                        target, curiosType, slotIndex, cosmetic, movedCount, false)
                : target.getInventory().removeItem(slotIndex, movedCount);
        if (removed.isEmpty()) {
            requester.sendSystemMessage(Component.translatable("message.infoplayer.curio_locked"));
            sendDetail(requester, playerId);
            return;
        }
        ItemStack received = removed.copy();
        moveToInventory(requester, removed);
        requester.containerMenu.broadcastChanges();
        target.containerMenu.broadcastChanges();
        PlayerDataStore.capture(requester, false);
        PlayerDataStore.capture(target, false);
        PlayerDataStore.save();

        requester.sendSystemMessage(Component.translatable(
                "message.infoplayer.item_taken",
                received.getHoverName(),
                received.getCount(),
                target.getGameProfile().getName()));
        sendDetail(requester, playerId);
    }

    private static int availableInventorySpace(ServerPlayer player, ItemStack source) {
        int available = 0;
        for (ItemStack existing : player.getInventory().items) {
            if (existing.isEmpty()) {
                available += source.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(existing, source)) {
                available += Math.max(0, existing.getMaxStackSize() - existing.getCount());
            }
            if (available >= source.getCount()) {
                return source.getCount();
            }
        }
        return available;
    }

    private static PlayerDetail applyVisibility(
            PlayerDetail detail,
            boolean coordinatesVisible,
            boolean inventoryVisible) {
        List<ItemStack> inventory = detail.inventory();
        if (!inventoryVisible) {
            List<ItemStack> hiddenInventory = new ArrayList<>(41);
            for (int index = 0; index < 41; index++) {
                hiddenInventory.add(ItemStack.EMPTY);
            }
            inventory = List.copyOf(hiddenInventory);
        }
        return new PlayerDetail(
                detail.summary(),
                detail.firstSeen(),
                detail.health(),
                detail.foodLevel(),
                detail.gameMode(),
                detail.dimension(),
                coordinatesVisible ? detail.x() : 0,
                coordinatesVisible ? detail.y() : 0,
                coordinatesVisible ? detail.z() : 0,
                inventoryVisible ? detail.selectedSlot() : 0,
                inventory,
                inventoryVisible ? detail.curios() : List.of());
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
