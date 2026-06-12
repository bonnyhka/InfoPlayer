package ua.bonny.infoplayer.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import ua.bonny.infoplayer.InfoPlayerMod;

public record DetailRequestPayload(UUID playerId) implements CustomPacketPayload {
    public static final Type<DetailRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(InfoPlayerMod.MOD_ID, "detail_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DetailRequestPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> buffer.writeUUID(payload.playerId),
                    buffer -> new DetailRequestPayload(buffer.readUUID()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
