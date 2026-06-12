package ua.bonny.infoplayer.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import ua.bonny.infoplayer.InfoPlayerMod;

public record PrivacyChangedPayload(UUID playerId) implements CustomPacketPayload {
    public static final Type<PrivacyChangedPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(InfoPlayerMod.MOD_ID, "privacy_changed"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PrivacyChangedPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> buffer.writeUUID(payload.playerId),
                    buffer -> new PrivacyChangedPayload(buffer.readUUID()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
