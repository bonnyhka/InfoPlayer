package ua.bonny.infoplayer.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.bonny.infoplayer.data.PrivacyOption;

final class InfoPlayerSettings {
    private static final Logger LOGGER = LoggerFactory.getLogger(InfoPlayerSettings.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static SettingsFile settings = new SettingsFile();
    private static Path settingsFile;

    private InfoPlayerSettings() {
    }

    static void load(MinecraftServer server) {
        settingsFile = server.getWorldPath(LevelResource.ROOT).resolve("infoplayer_settings.json");
        settings = new SettingsFile();
        if (!Files.isRegularFile(settingsFile)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(settingsFile)) {
            SettingsFile loaded = GSON.fromJson(reader, SettingsFile.class);
            if (loaded != null) {
                settings = loaded;
            }
        } catch (IOException | JsonParseException exception) {
            LOGGER.error("Could not load InfoPlayer settings from {}", settingsFile, exception);
        }
    }

    static void update(UUID playerId, long visibleMask) {
        if (settings.playerPrivacy == null) {
            settings.playerPrivacy = new HashMap<>();
        }
        settings.playerPrivacy.put(playerId.toString(), PrivacyOption.sanitize(visibleMask));
        save();
    }

    static long visibleMask(UUID playerId) {
        if (settings.playerPrivacy == null) {
            settings.playerPrivacy = new HashMap<>();
        }
        return PrivacyOption.sanitize(
                settings.playerPrivacy.getOrDefault(playerId.toString(), PrivacyOption.ALL_VISIBLE));
    }

    static void save() {
        if (settingsFile == null) {
            return;
        }
        Path temporary = settingsFile.resolveSibling(settingsFile.getFileName() + ".tmp");
        try {
            Files.createDirectories(settingsFile.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(settings, writer);
            }
            try {
                Files.move(temporary, settingsFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temporary, settingsFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.error("Could not save InfoPlayer settings to {}", settingsFile, exception);
        }
    }

    private static final class SettingsFile {
        Map<String, Long> playerPrivacy = new HashMap<>();
    }
}
