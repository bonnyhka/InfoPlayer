package ua.bonny.infoplayer.data;

import net.minecraft.network.RegistryFriendlyByteBuf;

public record PlayerListEntry(PlayerSummary summary, long visibleMask) {
    public static PlayerListEntry decode(RegistryFriendlyByteBuf buffer) {
        return new PlayerListEntry(PlayerSummary.decode(buffer), buffer.readVarLong());
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        summary.encode(buffer);
        buffer.writeVarLong(visibleMask);
    }
}
