package ua.bonny.infoplayer.client.screen;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import ua.bonny.infoplayer.client.ClientFormat;
import ua.bonny.infoplayer.data.PlayerListEntry;
import ua.bonny.infoplayer.data.PlayerSummary;
import ua.bonny.infoplayer.data.PrivacyOption;
import ua.bonny.infoplayer.network.DetailRequestPayload;
import ua.bonny.infoplayer.network.ListRequestPayload;
import ua.bonny.infoplayer.client.widget.GreenButton;

public final class PlayerListScreen extends Screen {
    private static final int BACKGROUND = 0xFF0E1511;
    private static final int HEADER = 0xFF14271C;
    private static final int CARD = 0xFF19241E;
    private static final int CARD_HOVER = 0xFF223329;
    private static final int BORDER = 0xFF335242;
    private static final int GREEN = 0xFF48D17A;
    private static final int TEXT = 0xFFF2F6F3;
    private static final int MUTED = 0xFFA8B8AE;
    private static final int OFFLINE = 0xFFE35D6A;
    private static final int GOLD = 0xFFF2C14E;
    private static final int CARD_HEIGHT = 82;
    private static final int GAP = 10;
    private static final int CONTENT_TOP = 102;

    private final List<PlayerListEntry> allPlayers = new ArrayList<>();
    private final List<PlayerListEntry> filteredPlayers = new ArrayList<>();
    private final List<CardBounds> cardBounds = new ArrayList<>();
    private boolean administrator;
    private long ownVisibleMask;
    private EditBox searchBox;
    private GreenButton refreshButton;
    private GreenButton settingsButton;
    private int scrollOffset;
    private int maxScroll;

    public PlayerListScreen(
            boolean administrator,
            long ownVisibleMask,
            List<PlayerListEntry> players) {
        super(Component.translatable("screen.infoplayer.title"));
        updatePlayers(administrator, ownVisibleMask, players);
    }

    public void updatePlayers(
            boolean administrator,
            long ownVisibleMask,
            List<PlayerListEntry> players) {
        this.administrator = administrator;
        this.ownVisibleMask = ownVisibleMask;
        allPlayers.clear();
        allPlayers.addAll(players);
        applyFilter(searchBox == null ? "" : searchBox.getValue());
        if (refreshButton != null) {
            refreshButton.active = true;
        }
    }

    @Override
    protected void init() {
        boolean compactToolbar = width < 500;
        int fieldWidth = compactToolbar
                ? Math.max(100, width - 48)
                : Math.min(250, Math.max(100, width - 350));
        searchBox = new EditBox(font, 82, 66, fieldWidth - 52, 18, Component.literal("Имя игрока"));
        searchBox.setHint(Component.literal("Имя игрока..."));
        searchBox.setMaxLength(32);
        searchBox.setBordered(false);
        searchBox.setTextColor(TEXT);
        searchBox.setResponder(this::applyFilter);
        addRenderableWidget(searchBox);

        refreshButton = new GreenButton(
                compactToolbar ? 140 : width - 132,
                compactToolbar ? 94 : 56,
                108,
                30,
                Component.translatable("screen.infoplayer.refresh"),
                true,
                () -> {
            refreshButton.active = false;
            PacketDistributor.sendToServer(new ListRequestPayload());
        });
        addRenderableWidget(refreshButton);

        settingsButton = new GreenButton(
                compactToolbar ? 24 : width - 248,
                compactToolbar ? 94 : 56,
                108,
                30,
                Component.literal("Приватность"),
                false,
                () -> minecraft.setScreen(new SettingsScreen(this, ownVisibleMask)));
        addRenderableWidget(settingsButton);
    }

    private void applyFilter(String query) {
        String normalized = query.toLowerCase(Locale.ROOT).trim();
        filteredPlayers.clear();
        filteredPlayers.addAll(allPlayers.stream()
                .filter(entry -> normalized.isEmpty()
                        || entry.summary().profile().getName().toLowerCase(Locale.ROOT).contains(normalized))
                .toList());
        scrollOffset = 0;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, BACKGROUND);
        graphics.fillGradient(0, 0, width, 42, 0xFF173223, HEADER);
        graphics.fill(0, 41, width, 42, GREEN);
        graphics.drawString(font, title, 24, 10, TEXT, false);
        graphics.drawString(font, Component.literal("Статистика игроков сервера"), 24, 24, MUTED, false);

        long online = allPlayers.stream()
                .filter(entry -> PrivacyOption.ONLINE_STATUS.visible(entry.visibleMask()))
                .map(PlayerListEntry::summary)
                .filter(PlayerSummary::online)
                .count();
        Component counter = Component.translatable("screen.infoplayer.counter", online, allPlayers.size());
        graphics.drawString(font, counter, width - 24 - font.width(counter), 17, TEXT, false);

        int fieldWidth = width < 500
                ? Math.max(100, width - 48)
                : Math.min(250, Math.max(100, width - 350));
        graphics.fill(24, 56, 24 + fieldWidth, 86, CARD);
        graphics.renderOutline(24, 56, fieldWidth, 30, searchBox.isFocused() ? GREEN : BORDER);
        graphics.fill(24, 56, 28, 86, searchBox.isFocused() ? GREEN : 0xFF3C6650);
        graphics.drawString(font, Component.literal("Найти:"), 36, 67, MUTED, false);

        renderCards(graphics, mouseX, mouseY);
        for (Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }

        if (filteredPlayers.isEmpty()) {
            Component empty = Component.translatable("screen.infoplayer.empty");
            graphics.drawCenteredString(font, empty, width / 2, contentTop() + 32, MUTED);
        }
    }

    private void renderCards(GuiGraphics graphics, int mouseX, int mouseY) {
        cardBounds.clear();
        int contentTop = contentTop();
        int availableWidth = Math.max(120, width - 48);
        int columns = Math.max(1, Math.min(4, (availableWidth + GAP) / 280));
        int cardWidth = Math.min(300, (availableWidth - GAP * (columns - 1)) / columns);
        int rows = (filteredPlayers.size() + columns - 1) / columns;
        int viewportHeight = Math.max(0, height - contentTop - 18);
        maxScroll = Math.max(0, rows * (CARD_HEIGHT + GAP) - GAP - viewportHeight);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        graphics.enableScissor(0, contentTop, width, height - 10);
        for (int index = 0; index < filteredPlayers.size(); index++) {
            int column = index % columns;
            int row = index / columns;
            int x = 24 + column * (cardWidth + GAP);
            int y = contentTop + row * (CARD_HEIGHT + GAP) - scrollOffset;
            if (y + CARD_HEIGHT < contentTop || y > height) {
                continue;
            }

            PlayerListEntry entry = filteredPlayers.get(index);
            PlayerSummary player = entry.summary();
            long visibleMask = entry.visibleMask();
            boolean hovered = mouseX >= x && mouseX < x + cardWidth && mouseY >= y
                    && mouseY < y + CARD_HEIGHT && mouseY >= contentTop;
            graphics.fill(x, y, x + cardWidth, y + CARD_HEIGHT, hovered ? CARD_HOVER : CARD);
            graphics.renderOutline(x, y, cardWidth, CARD_HEIGHT, hovered ? GREEN : BORDER);
            int statusColor = PrivacyOption.ONLINE_STATUS.visible(visibleMask)
                    ? (player.online() ? GREEN : OFFLINE)
                    : BORDER;
            graphics.fill(x, y, x + 3, y + CARD_HEIGHT, statusColor);

            drawPlayerHead(graphics, player.profile(), x + 12, y + 14, 36);
            graphics.drawString(font, player.profile().getName(), x + 58, y + 12, TEXT, false);
            String activity = activityText(player, visibleMask);
            graphics.drawString(font, activity, x + 58, y + 27, statusColor, false);

            Component level = PrivacyOption.EXPERIENCE.visible(visibleMask)
                    ? Component.translatable("screen.infoplayer.level.short", player.experienceLevel())
                    : Component.literal("Уровень скрыт");
            graphics.drawString(
                    font,
                    level,
                    x + 58,
                    y + 45,
                    PrivacyOption.EXPERIENCE.visible(visibleMask) ? GOLD : MUTED,
                    false);
            Component advancements = PrivacyOption.ADVANCEMENTS.visible(visibleMask)
                    ? Component.literal("Достижения: " + player.advancementsDone() + "/" + player.advancementsTotal())
                    : Component.literal("Достижения скрыты");
            graphics.drawString(font, advancements, x + cardWidth - 12 - font.width(advancements), y + 45, MUTED, false);
            int progressX = x + 58;
            int progressY = y + 65;
            int progressWidth = Math.max(20, cardWidth - 70);
            float progress = !PrivacyOption.ADVANCEMENTS.visible(visibleMask) || player.advancementsTotal() == 0
                    ? 0
                    : (float) player.advancementsDone() / player.advancementsTotal();
            graphics.fill(progressX, progressY, progressX + progressWidth, progressY + 5, 0xFF2C3931);
            graphics.fill(progressX, progressY, progressX + Math.round(progressWidth * progress), progressY + 5, GREEN);
            cardBounds.add(new CardBounds(x, y, cardWidth, CARD_HEIGHT, entry));
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int trackY = contentTop;
            int trackHeight = Math.max(10, height - contentTop - 12);
            int thumbHeight = Math.max(18, trackHeight * trackHeight / (trackHeight + maxScroll));
            int thumbY = trackY + (trackHeight - thumbHeight) * scrollOffset / maxScroll;
            graphics.fill(width - 7, trackY, width - 4, trackY + trackHeight, 0xFF26332B);
            graphics.fill(width - 7, thumbY, width - 4, thumbY + thumbHeight, GREEN);
        }
    }

    private void drawPlayerHead(GuiGraphics graphics, GameProfile profile, int x, int y, int size) {
        PlayerFaceRenderer.draw(graphics, minecraft.getSkinManager().getInsecureSkin(profile), x, y, size);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0) {
            for (CardBounds bounds : cardBounds) {
                if (bounds.contains(mouseX, mouseY)) {
                    PacketDistributor.sendToServer(new DetailRequestPayload(
                            bounds.player.summary().profile().getId()));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll > 0 && mouseY >= contentTop()) {
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.round(verticalAmount * 28)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String activityText(PlayerSummary player, long visibleMask) {
        boolean statusVisible = PrivacyOption.ONLINE_STATUS.visible(visibleMask);
        boolean activityVisible = PrivacyOption.LAST_ACTIVITY.visible(visibleMask);
        if (statusVisible && player.online()) {
            return "В сети";
        }
        if (activityVisible && player.lastSeen() > 0) {
            return statusVisible
                    ? ClientFormat.lastSeen(false, player.lastSeen()).getString()
                    : "Активность: " + ClientFormat.date(player.lastSeen());
        }
        return statusVisible ? "Не в сети" : "Статус скрыт";
    }

    private record CardBounds(int x, int y, int width, int height, PlayerListEntry player) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private int contentTop() {
        return width < 500 ? 138 : CONTENT_TOP;
    }
}
