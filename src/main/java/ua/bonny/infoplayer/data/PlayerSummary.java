package ua.bonny.infoplayer.data;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;

public record PlayerSummary(
        GameProfile profile,
        boolean online,
        long lastSeen,
        int experienceLevel,
        int totalExperience,
        int playTimeTicks,
        int advancementsDone,
        int advancementsTotal) {

    public static PlayerSummary decode(RegistryFriendlyByteBuf buffer) {
        return new PlayerSummary(
                ByteBufCodecs.GAME_PROFILE.decode(buffer),
                buffer.readBoolean(),
                buffer.readLong(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt());
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        ByteBufCodecs.GAME_PROFILE.encode(buffer, profile);
        buffer.writeBoolean(online);
        buffer.writeLong(lastSeen);
        buffer.writeVarInt(experienceLevel);
        buffer.writeVarInt(totalExperience);
        buffer.writeVarInt(playTimeTicks);
        buffer.writeVarInt(advancementsDone);
        buffer.writeVarInt(advancementsTotal);
    }
}
