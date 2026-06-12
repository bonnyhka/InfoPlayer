package ua.bonny.infoplayer.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import ua.bonny.infoplayer.InfoPlayerMod;

public record TakeItemRequestPayload(
        UUID playerId,
        int slotIndex,
        String curiosType,
        boolean cosmetic) implements CustomPacketPayload {
    public static final Type<TakeItemRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(InfoPlayerMod.MOD_ID, "take_item_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TakeItemRequestPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeUUID(payload.playerId);
                        buffer.writeVarInt(payload.slotIndex);
                        buffer.writeUtf(payload.curiosType, 64);
                        buffer.writeBoolean(payload.cosmetic);
                    },
                    buffer -> new TakeItemRequestPayload(
                            buffer.readUUID(),
                            buffer.readVarInt(),
                            buffer.readUtf(64),
                            buffer.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
