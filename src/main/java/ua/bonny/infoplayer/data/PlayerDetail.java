package ua.bonny.infoplayer.data;

import net.minecraft.network.RegistryFriendlyByteBuf;

public record PlayerDetail(
        PlayerSummary summary,
        long firstSeen,
        float health,
        int foodLevel,
        String gameMode,
        String dimension,
        double x,
        double y,
        double z) {

    public static PlayerDetail decode(RegistryFriendlyByteBuf buffer) {
        return new PlayerDetail(
                PlayerSummary.decode(buffer),
                buffer.readLong(),
                buffer.readFloat(),
                buffer.readVarInt(),
                buffer.readUtf(32),
                buffer.readUtf(128),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble());
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
    }
}
