package ua.bonny.infoplayer.server;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.UUID;
import ua.bonny.infoplayer.data.PlayerDetail;
import ua.bonny.infoplayer.data.PlayerSummary;

final class StoredPlayer {
    String uuid;
    String name;
    String textureValue;
    String textureSignature;
    long firstSeen;
    long lastSeen;
    int experienceLevel;
    int totalExperience;
    int playTimeTicks;
    int advancementsDone;
    int advancementsTotal;
    float health;
    int foodLevel;
    String gameMode;
    String dimension;
    double x;
    double y;
    double z;

    StoredPlayer() {
    }

    UUID playerId() {
        return UUID.fromString(uuid);
    }

    GameProfile profile() {
        GameProfile profile = new GameProfile(playerId(), name);
        if (textureValue != null && !textureValue.isBlank()) {
            profile.getProperties().put("textures", new Property("textures", textureValue, textureSignature));
        }
        return profile;
    }

    PlayerSummary summary(boolean online) {
        return new PlayerSummary(
                profile(),
                online,
                lastSeen,
                experienceLevel,
                totalExperience,
                playTimeTicks,
                advancementsDone,
                advancementsTotal);
    }

    PlayerDetail detail(boolean online) {
        return new PlayerDetail(
                summary(online),
                firstSeen,
                health,
                foodLevel,
                gameMode == null ? "unknown" : gameMode,
                dimension == null ? "minecraft:overworld" : dimension,
                x,
                y,
                z);
    }
}
