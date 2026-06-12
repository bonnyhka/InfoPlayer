package ua.bonny.infoplayer.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import ua.bonny.infoplayer.data.CurioSlot;

final class CuriosCompatibility {
    private CuriosCompatibility() {
    }

    static List<CurioSlot> capture(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> {
                    List<CurioSlot> result = new ArrayList<>();
                    inventory.getCurios().entrySet().stream()
                            .sorted(Comparator.comparing(entry -> entry.getKey(), String.CASE_INSENSITIVE_ORDER))
                            .forEach(entry -> appendSlots(result, entry.getKey(), entry.getValue()));
                    return List.copyOf(result);
                })
                .orElseGet(List::of);
    }

    static ItemStack getStack(ServerPlayer player, String identifier, int index, boolean cosmetic) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(inventory -> inventory.getStacksHandler(identifier))
                .map(handler -> stackHandler(handler, cosmetic))
                .filter(handler -> index >= 0 && index < handler.getSlots())
                .map(handler -> handler.getStackInSlot(index))
                .orElse(ItemStack.EMPTY);
    }

    static ItemStack extractStack(
            ServerPlayer player,
            String identifier,
            int index,
            boolean cosmetic,
            int amount,
            boolean simulate) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(inventory -> inventory.getStacksHandler(identifier))
                .map(handler -> stackHandler(handler, cosmetic))
                .filter(handler -> index >= 0 && index < handler.getSlots())
                .map(handler -> handler.extractItem(index, amount, simulate))
                .orElse(ItemStack.EMPTY);
    }

    private static void appendSlots(
            List<CurioSlot> result,
            String identifier,
            ICurioStacksHandler handler) {
        IDynamicStackHandler stacks = handler.getStacks();
        for (int index = 0; index < stacks.getSlots(); index++) {
            result.add(new CurioSlot(identifier, index, false, stacks.getStackInSlot(index).copy()));
        }

        if (handler.hasCosmetic()) {
            IDynamicStackHandler cosmetics = handler.getCosmeticStacks();
            for (int index = 0; index < cosmetics.getSlots(); index++) {
                ItemStack stack = cosmetics.getStackInSlot(index);
                if (!stack.isEmpty()) {
                    result.add(new CurioSlot(identifier, index, true, stack.copy()));
                }
            }
        }
    }

    private static IDynamicStackHandler stackHandler(ICurioStacksHandler handler, boolean cosmetic) {
        return cosmetic ? handler.getCosmeticStacks() : handler.getStacks();
    }
}
