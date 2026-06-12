package ua.bonny.infoplayer.client;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import net.minecraft.network.chat.Component;

public final class ClientFormat {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    private ClientFormat() {
    }

    public static Component lastSeen(boolean online, long timestamp) {
        if (online) {
            return Component.translatable("screen.infoplayer.online");
        }
        long seconds = Math.max(0, (System.currentTimeMillis() - timestamp) / 1000);
        if (seconds < 60) {
            return Component.translatable("screen.infoplayer.offline.now");
        }
        if (seconds < 3600) {
            return Component.translatable("screen.infoplayer.offline.minutes", seconds / 60);
        }
        if (seconds < 86_400) {
            return Component.translatable("screen.infoplayer.offline.hours", seconds / 3600);
        }
        return Component.translatable("screen.infoplayer.offline.days", seconds / 86_400);
    }

    public static String date(long timestamp) {
        return DATE_FORMAT.format(Instant.ofEpochMilli(timestamp));
    }

    public static Component playTime(int ticks) {
        long totalMinutes = Math.max(0, ticks / 20L / 60L);
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return Component.translatable("screen.infoplayer.playtime.value", hours, minutes);
    }
}
