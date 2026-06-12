package ua.bonny.infoplayer.data;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record CurioSlot(String identifier, int index, boolean cosmetic, ItemStack stack) {
    public static CurioSlot decode(RegistryFriendlyByteBuf buffer) {
        return new CurioSlot(
                buffer.readUtf(64),
                buffer.readVarInt(),
                buffer.readBoolean(),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(identifier, 64);
        buffer.writeVarInt(index);
        buffer.writeBoolean(cosmetic);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack);
    }
}
