package ua.bonny.infoplayer.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.authlib.properties.Property;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PlayerDataStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerDataStore.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, StoredPlayer> PLAYERS = new HashMap<>();
    private static Path dataFile;

    private PlayerDataStore() {
    }

    static void load(MinecraftServer server) {
        dataFile = server.getWorldPath(LevelResource.ROOT).resolve("infoplayer_players.json");
        PLAYERS.clear();
        if (!Files.isRegularFile(dataFile)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(dataFile)) {
            StoreFile store = GSON.fromJson(reader, StoreFile.class);
            if (store != null && store.players != null) {
                for (StoredPlayer player : store.players) {
                    if (player.uuid != null && player.name != null) {
                        PLAYERS.put(player.playerId(), player);
                    }
                }
            }
        } catch (IOException | JsonParseException | IllegalArgumentException exception) {
            LOGGER.error("Could not load InfoPlayer data from {}", dataFile, exception);
        }
    }

    static void save() {
        if (dataFile == null) {
            return;
        }
        Path temporary = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");
        try {
            Files.createDirectories(dataFile.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                StoreFile store = new StoreFile();
                store.players = new ArrayList<>(PLAYERS.values());
                GSON.toJson(store, writer);
            }
            try {
                Files.move(temporary, dataFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temporary, dataFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.error("Could not save InfoPlayer data to {}", dataFile, exception);
        }
    }

    static StoredPlayer capture(ServerPlayer player, boolean leaving) {
        UUID id = player.getGameProfile().getId();
        long now = System.currentTimeMillis();
        StoredPlayer stored = PLAYERS.computeIfAbsent(id, ignored -> {
            StoredPlayer created = new StoredPlayer();
            created.uuid = id.toString();
            created.firstSeen = now;
            return created;
        });

        stored.name = player.getGameProfile().getName();
        stored.lastSeen = leaving ? now : Math.max(stored.lastSeen, now);
        Property texture = player.getGameProfile().getProperties().get("textures").stream().findFirst().orElse(null);
        if (texture != null) {
            stored.textureValue = texture.value();
            stored.textureSignature = texture.signature();
        }
        stored.experienceLevel = player.experienceLevel;
        stored.totalExperience = player.totalExperience;
        stored.playTimeTicks = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
        stored.health = player.getHealth();
        stored.foodLevel = player.getFoodData().getFoodLevel();
        stored.gameMode = player.gameMode.getGameModeForPlayer().getName();
        stored.dimension = player.level().dimension().location().toString();
        stored.x = player.getX();
        stored.y = player.getY();
        stored.z = player.getZ();
        stored.selectedSlot = player.getInventory().selected;
        stored.inventory = new ArrayList<>(41);
        for (ItemStack stack : player.getInventory().items) {
            stored.inventory.add(stack.saveOptional(player.registryAccess()).toString());
        }
        for (ItemStack stack : player.getInventory().armor) {
            stored.inventory.add(stack.saveOptional(player.registryAccess()).toString());
        }
        for (ItemStack stack : player.getInventory().offhand) {
            stored.inventory.add(stack.saveOptional(player.registryAccess()).toString());
        }

        int done = 0;
        int total = 0;
        for (AdvancementHolder advancement : player.getServer().getAdvancements().getAllAdvancements()) {
            total++;
            if (player.getAdvancements().getOrStartProgress(advancement).isDone()) {
                done++;
            }
        }
        stored.advancementsDone = done;
        stored.advancementsTotal = total;
        return stored;
    }

    static List<StoredPlayer> all() {
        return List.copyOf(PLAYERS.values());
    }

    static StoredPlayer get(UUID id) {
        return PLAYERS.get(id);
    }

    private static final class StoreFile {
        List<StoredPlayer> players = new ArrayList<>();
    }
}
