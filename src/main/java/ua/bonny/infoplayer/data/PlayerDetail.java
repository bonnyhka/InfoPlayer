package ua.bonny.infoplayer.data;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record PlayerDetail(
        PlayerSummary summary,
        long firstSeen,
        float health,
        int foodLevel,
        String gameMode,
        String dimension,
        double x,
        double y,
        double z,
        int selectedSlot,
        List<ItemStack> inventory) {

    private static final int INVENTORY_SIZE = 41;

    public static PlayerDetail decode(RegistryFriendlyByteBuf buffer) {
        PlayerSummary summary = PlayerSummary.decode(buffer);
        long firstSeen = buffer.readLong();
        float health = buffer.readFloat();
        int foodLevel = buffer.readVarInt();
        String gameMode = buffer.readUtf(32);
        String dimension = buffer.readUtf(128);
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        int selectedSlot = buffer.readVarInt();
        int size = buffer.readVarInt();
        if (size < 0 || size > INVENTORY_SIZE) {
            throw new IllegalArgumentException("Invalid InfoPlayer inventory size: " + size);
        }
        List<ItemStack> inventory = new ArrayList<>(INVENTORY_SIZE);
        for (int i = 0; i < size; i++) {
            inventory.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
        }
        while (inventory.size() < INVENTORY_SIZE) {
            inventory.add(ItemStack.EMPTY);
        }
        return new PlayerDetail(
                summary, firstSeen, health, foodLevel, gameMode, dimension, x, y, z,
                selectedSlot, List.copyOf(inventory));
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        summary.encode(buffer);
        buffer.writeLong(firstSeen);
        buffer.writeFloat(health);
        buffer.writeVarInt(foodLevel);
        buffer.writeUtf(gameMode, 32);
        buffer.writeUtf(dimension, 128);
        buffer.writeDouble(x);
        buffer.writeDouble(y);
        buffer.writeDouble(z);
        buffer.writeVarInt(selectedSlot);
        buffer.writeVarInt(Math.min(inventory.size(), INVENTORY_SIZE));
        inventory.stream().limit(INVENTORY_SIZE).forEach(stack -> ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack));
    }
}
