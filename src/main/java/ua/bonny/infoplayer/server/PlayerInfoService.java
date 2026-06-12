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
import ua.bonny.infoplayer.data.PlayerListEntry;
import ua.bonny.infoplayer.data.PlayerSummary;
import ua.bonny.infoplayer.data.PrivacyOption;
import ua.bonny.infoplayer.network.DetailResponsePayload;
import ua.bonny.infoplayer.network.ListResponsePayload;
import ua.bonny.infoplayer.network.PrivacyChangedPayload;

public final class PlayerInfoService {
    private PlayerInfoService() {
    }

    public static void sendList(ServerPlayer requester) {
        MinecraftServer server = requester.getServer();
        server.getPlayerList().getPlayers().forEach(player -> PlayerDataStore.capture(player, false));
        boolean administrator = isAdmin(requester);
        List<PlayerListEntry> players = PlayerDataStore.all().stream()
                .map(player -> {
                    PlayerSummary summary = player.summary(server.getPlayerList().getPlayer(player.playerId()) != null);
                    long visibleMask = visibleMask(requester, player.playerId(), administrator);
                    return new PlayerListEntry(applySummaryVisibility(summary, visibleMask), visibleMask);
                })
                .sorted(Comparator
                        .comparing((PlayerListEntry entry) -> entry.summary().online()).reversed()
                        .thenComparing(
                                entry -> entry.summary().profile().getName(),
                                String.CASE_INSENSITIVE_ORDER))
                .toList();
        PacketDistributor.sendToPlayer(requester, new ListResponsePayload(
                administrator,
                InfoPlayerSettings.visibleMask(requester.getUUID()),
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
            long visibleMask = visibleMask(requester, playerId, administrator);
            PlayerDetail detail = applyVisibility(
                    stored.detail(onlinePlayer != null, server.registryAccess()),
                    visibleMask);
            PacketDistributor.sendToPlayer(requester,
                    new DetailResponsePayload(
                            administrator,
                            visibleMask,
                            detail));
        }
    }

    public static void updateSettings(ServerPlayer requester, long visibleMask) {
        InfoPlayerSettings.update(requester.getUUID(), visibleMask);
        PacketDistributor.sendToAllPlayers(new PrivacyChangedPayload(requester.getUUID()));
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

    private static long visibleMask(ServerPlayer requester, UUID targetId, boolean administrator) {
        return administrator || requester.getUUID().equals(targetId)
                ? PrivacyOption.ALL_VISIBLE
                : InfoPlayerSettings.visibleMask(targetId);
    }

    private static PlayerSummary applySummaryVisibility(PlayerSummary summary, long visibleMask) {
        return new PlayerSummary(
                summary.profile(),
                PrivacyOption.ONLINE_STATUS.visible(visibleMask) && summary.online(),
                PrivacyOption.LAST_ACTIVITY.visible(visibleMask) ? summary.lastSeen() : 0,
                PrivacyOption.EXPERIENCE.visible(visibleMask) ? summary.experienceLevel() : 0,
                PrivacyOption.EXPERIENCE.visible(visibleMask) ? summary.totalExperience() : 0,
                PrivacyOption.PLAY_TIME.visible(visibleMask) ? summary.playTimeTicks() : 0,
                PrivacyOption.ADVANCEMENTS.visible(visibleMask) ? summary.advancementsDone() : 0,
                PrivacyOption.ADVANCEMENTS.visible(visibleMask) ? summary.advancementsTotal() : 0);
    }

    private static PlayerDetail applyVisibility(PlayerDetail detail, long visibleMask) {
        List<ItemStack> inventory = detail.inventory();
        boolean inventoryVisible = PrivacyOption.INVENTORY.visible(visibleMask);
        boolean curiosVisible = PrivacyOption.CURIOS.visible(visibleMask);
        if (!inventoryVisible) {
            List<ItemStack> hiddenInventory = new ArrayList<>(41);
            for (int index = 0; index < 41; index++) {
                hiddenInventory.add(ItemStack.EMPTY);
            }
            inventory = List.copyOf(hiddenInventory);
        }
        return new PlayerDetail(
                applySummaryVisibility(detail.summary(), visibleMask),
                PrivacyOption.FIRST_SEEN.visible(visibleMask) ? detail.firstSeen() : 0,
                PrivacyOption.HEALTH.visible(visibleMask) ? detail.health() : 0,
                PrivacyOption.FOOD.visible(visibleMask) ? detail.foodLevel() : 0,
                PrivacyOption.GAME_MODE.visible(visibleMask) ? detail.gameMode() : "",
                PrivacyOption.DIMENSION.visible(visibleMask) ? detail.dimension() : "",
                PrivacyOption.COORDINATES.visible(visibleMask) ? detail.x() : 0,
                PrivacyOption.COORDINATES.visible(visibleMask) ? detail.y() : 0,
                PrivacyOption.COORDINATES.visible(visibleMask) ? detail.z() : 0,
                inventoryVisible ? detail.selectedSlot() : 0,
                inventory,
                curiosVisible ? detail.curios() : List.of());
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
