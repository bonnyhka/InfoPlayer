package ua.bonny.infoplayer.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import ua.bonny.infoplayer.InfoPlayerMod;
import ua.bonny.infoplayer.data.PlayerDetail;

public record DetailResponsePayload(boolean administrator, PlayerDetail player) implements CustomPacketPayload {
    public static final Type<DetailResponsePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(InfoPlayerMod.MOD_ID, "detail_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DetailResponsePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeBoolean(payload.administrator);
                        payload.player.encode(buffer);
                    },
                    buffer -> new DetailResponsePayload(buffer.readBoolean(), PlayerDetail.decode(buffer)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
