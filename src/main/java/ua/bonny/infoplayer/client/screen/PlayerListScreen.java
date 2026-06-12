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
import ua.bonny.infoplayer.data.PlayerSummary;
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
    private static final int CARD_HEIGHT = 74;
    private static final int GAP = 10;
    private static final int CONTENT_TOP = 98;

    private final List<PlayerSummary> allPlayers = new ArrayList<>();
    private final List<PlayerSummary> filteredPlayers = new ArrayList<>();
    private final List<CardBounds> cardBounds = new ArrayList<>();
    private EditBox searchBox;
    private GreenButton refreshButton;
    private int scrollOffset;
    private int maxScroll;

    public PlayerListScreen(List<PlayerSummary> players) {
        super(Component.translatable("screen.infoplayer.title"));
        updatePlayers(players);
    }

    public void updatePlayers(List<PlayerSummary> players) {
        allPlayers.clear();
        allPlayers.addAll(players);
        applyFilter(searchBox == null ? "" : searchBox.getValue());
        if (refreshButton != null) {
            refreshButton.active = true;
        }
    }

    @Override
    protected void init() {
        int searchWidth = Math.min(260, Math.max(100, width - 190));
        searchBox = new EditBox(font, 34, 58, searchWidth - 20, 18, Component.translatable("screen.infoplayer.search"));
        searchBox.setHint(Component.translatable("screen.infoplayer.search"));
        searchBox.setMaxLength(32);
        searchBox.setBordered(false);
        searchBox.setResponder(this::applyFilter);
        addRenderableWidget(searchBox);

        refreshButton = new GreenButton(
                width - 132,
                52,
                108,
                28,
                Component.translatable("screen.infoplayer.refresh"),
                true,
                () -> {
            refreshButton.active = false;
            PacketDistributor.sendToServer(new ListRequestPayload());
        });
        addRenderableWidget(refreshButton);
    }

    private void applyFilter(String query) {
        String normalized = query.toLowerCase(Locale.ROOT).trim();
        filteredPlayers.clear();
        filteredPlayers.addAll(allPlayers.stream()
                .filter(player -> normalized.isEmpty()
                        || player.profile().getName().toLowerCase(Locale.ROOT).contains(normalized))
                .toList());
        scrollOffset = 0;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, BACKGROUND);
        graphics.fillGradient(0, 0, width, 42, 0xFF173223, HEADER);
        graphics.fill(0, 41, width, 42, GREEN);
        graphics.drawString(font, title, 24, 10, TEXT, false);
        graphics.drawString(font, Component.literal("Административная панель сервера"), 24, 24, MUTED, false);

        long online = allPlayers.stream().filter(PlayerSummary::online).count();
        Component counter = Component.translatable("screen.infoplayer.counter", online, allPlayers.size());
        graphics.drawString(font, counter, width - 24 - font.width(counter), 17, TEXT, false);

        graphics.fill(24, 52, 24 + Math.min(260, Math.max(100, width - 190)), 80, CARD);
        graphics.renderOutline(24, 52, Math.min(260, Math.max(100, width - 190)), 28, BORDER);
        graphics.drawString(font, Component.literal("Поиск"), 30, 48, GREEN, false);

        renderCards(graphics, mouseX, mouseY);
        for (Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }

        if (filteredPlayers.isEmpty()) {
            Component empty = Component.translatable("screen.infoplayer.empty");
            graphics.drawCenteredString(font, empty, width / 2, CONTENT_TOP + 32, MUTED);
        }
    }

    private void renderCards(GuiGraphics graphics, int mouseX, int mouseY) {
        cardBounds.clear();
        int availableWidth = Math.max(120, width - 48);
        int columns = Math.max(1, Math.min(4, (availableWidth + GAP) / 250));
        int cardWidth = (availableWidth - GAP * (columns - 1)) / columns;
        int rows = (filteredPlayers.size() + columns - 1) / columns;
        int viewportHeight = Math.max(0, height - CONTENT_TOP - 18);
        maxScroll = Math.max(0, rows * (CARD_HEIGHT + GAP) - GAP - viewportHeight);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        graphics.enableScissor(0, CONTENT_TOP, width, height - 10);
        for (int index = 0; index < filteredPlayers.size(); index++) {
            int column = index % columns;
            int row = index / columns;
            int x = 24 + column * (cardWidth + GAP);
            int y = CONTENT_TOP + row * (CARD_HEIGHT + GAP) - scrollOffset;
            if (y + CARD_HEIGHT < CONTENT_TOP || y > height) {
                continue;
            }

            PlayerSummary player = filteredPlayers.get(index);
            boolean hovered = mouseX >= x && mouseX < x + cardWidth && mouseY >= y
                    && mouseY < y + CARD_HEIGHT && mouseY >= CONTENT_TOP;
            graphics.fill(x, y, x + cardWidth, y + CARD_HEIGHT, hovered ? CARD_HOVER : CARD);
            graphics.renderOutline(x, y, cardWidth, CARD_HEIGHT, hovered ? GREEN : BORDER);
            graphics.fill(x, y, x + 3, y + CARD_HEIGHT, player.online() ? GREEN : OFFLINE);

            drawPlayerHead(graphics, player.profile(), x + 12, y + 12, 32);
            graphics.drawString(font, player.profile().getName(), x + 52, y + 12, TEXT, false);
            graphics.drawString(font, ClientFormat.lastSeen(player.online(), player.lastSeen()), x + 52, y + 27,
                    player.online() ? GREEN : OFFLINE, false);

            Component level = Component.translatable("screen.infoplayer.level.short", player.experienceLevel());
            graphics.drawString(font, level, x + 52, y + 44, GOLD, false);
            int progressX = x + 52;
            int progressY = y + 59;
            int progressWidth = Math.max(20, cardWidth - 64);
            float progress = player.advancementsTotal() == 0
                    ? 0
                    : (float) player.advancementsDone() / player.advancementsTotal();
            graphics.fill(progressX, progressY, progressX + progressWidth, progressY + 4, 0xFF2C3931);
            graphics.fill(progressX, progressY, progressX + Math.round(progressWidth * progress), progressY + 4, GREEN);
            cardBounds.add(new CardBounds(x, y, cardWidth, CARD_HEIGHT, player));
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int trackY = CONTENT_TOP;
            int trackHeight = Math.max(10, height - CONTENT_TOP - 12);
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
                    PacketDistributor.sendToServer(new DetailRequestPayload(bounds.player.profile().getId()));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll > 0 && mouseY >= CONTENT_TOP) {
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.round(verticalAmount * 28)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record CardBounds(int x, int y, int width, int height, PlayerSummary player) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
