package ua.bonny.infoplayer.server;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import ua.bonny.infoplayer.data.CurioSlot;
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
    int selectedSlot;
    List<String> inventory = new ArrayList<>();
    List<StoredCurioSlot> curios = new ArrayList<>();

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

    PlayerDetail detail(boolean online, HolderLookup.Provider registries) {
        return new PlayerDetail(
                summary(online),
                firstSeen,
                health,
                foodLevel,
                russianGameMode(),
                russianDimension(),
                x,
                y,
                z,
                selectedSlot,
                decodeInventory(registries),
                decodeCurios(registries));
    }

    private List<ItemStack> decodeInventory(HolderLookup.Provider registries) {
        List<ItemStack> result = new ArrayList<>(41);
        for (String encoded : inventory == null ? List.<String>of() : inventory) {
            try {
                result.add(ItemStack.parseOptional(registries, TagParser.parseTag(encoded)));
            } catch (CommandSyntaxException exception) {
                result.add(ItemStack.EMPTY);
            }
        }
        while (result.size() < 41) {
            result.add(ItemStack.EMPTY);
        }
        return List.copyOf(result.subList(0, 41));
    }

    private List<CurioSlot> decodeCurios(HolderLookup.Provider registries) {
        List<CurioSlot> result = new ArrayList<>();
        for (StoredCurioSlot stored : curios == null ? List.<StoredCurioSlot>of() : curios) {
            if (stored == null || stored.identifier == null || stored.item == null) {
                continue;
            }
            try {
                result.add(new CurioSlot(
                        stored.identifier,
                        stored.index,
                        stored.cosmetic,
                        ItemStack.parseOptional(registries, TagParser.parseTag(stored.item))));
            } catch (CommandSyntaxException exception) {
                result.add(new CurioSlot(stored.identifier, stored.index, stored.cosmetic, ItemStack.EMPTY));
            }
        }
        return List.copyOf(result);
    }

    private String russianGameMode() {
        if (gameMode == null) {
            return "Неизвестно";
        }
        return switch (gameMode) {
            case "survival" -> "Выживание";
            case "creative" -> "Творческий";
            case "adventure" -> "Приключение";
            case "spectator" -> "Наблюдатель";
            default -> gameMode;
        };
    }

    private String russianDimension() {
        if (dimension == null) {
            return "Обычный мир";
        }
        return switch (dimension) {
            case "minecraft:overworld" -> "Обычный мир";
            case "minecraft:the_nether" -> "Незер";
            case "minecraft:the_end" -> "Энд";
            default -> dimension;
        };
    }
}
