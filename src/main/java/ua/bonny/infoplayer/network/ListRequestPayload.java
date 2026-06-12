package ua.bonny.infoplayer.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import ua.bonny.infoplayer.InfoPlayerMod;

public record ListRequestPayload() implements CustomPacketPayload {
    public static final Type<ListRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(InfoPlayerMod.MOD_ID, "list_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ListRequestPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
            }, buffer -> new ListRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
