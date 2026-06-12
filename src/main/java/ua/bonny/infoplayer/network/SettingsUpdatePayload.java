package ua.bonny.infoplayer.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import ua.bonny.infoplayer.InfoPlayerMod;

public record SettingsUpdatePayload(
        boolean showCoordinatesToPlayers,
        boolean showInventoryToPlayers) implements CustomPacketPayload {
    public static final Type<SettingsUpdatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(InfoPlayerMod.MOD_ID, "settings_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SettingsUpdatePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeBoolean(payload.showCoordinatesToPlayers);
                        buffer.writeBoolean(payload.showInventoryToPlayers);
                    },
                    buffer -> new SettingsUpdatePayload(buffer.readBoolean(), buffer.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
