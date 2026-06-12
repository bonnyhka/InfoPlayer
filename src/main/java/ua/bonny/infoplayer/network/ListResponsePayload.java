package ua.bonny.infoplayer.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import ua.bonny.infoplayer.InfoPlayerMod;
import ua.bonny.infoplayer.data.PlayerListEntry;

public record ListResponsePayload(
        boolean administrator,
        long ownVisibleMask,
        List<PlayerListEntry> players) implements CustomPacketPayload {
    private static final int MAX_PLAYERS = 10_000;
    public static final Type<ListResponsePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(InfoPlayerMod.MOD_ID, "list_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ListResponsePayload> STREAM_CODEC =
            StreamCodec.of(ListResponsePayload::encode, ListResponsePayload::decode);

    private static ListResponsePayload decode(RegistryFriendlyByteBuf buffer) {
        boolean administrator = buffer.readBoolean();
        long ownVisibleMask = buffer.readVarLong();
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_PLAYERS) {
            throw new IllegalArgumentException("Invalid InfoPlayer list size: " + size);
        }
        List<PlayerListEntry> players = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            players.add(PlayerListEntry.decode(buffer));
        }
        return new ListResponsePayload(
                administrator,
                ownVisibleMask,
                List.copyOf(players));
    }

    private static void encode(RegistryFriendlyByteBuf buffer, ListResponsePayload payload) {
        buffer.writeBoolean(payload.administrator);
        buffer.writeVarLong(payload.ownVisibleMask);
        buffer.writeVarInt(payload.players.size());
        payload.players.forEach(player -> player.encode(buffer));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
