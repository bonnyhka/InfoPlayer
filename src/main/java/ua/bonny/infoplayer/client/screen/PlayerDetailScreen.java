package ua.bonny.infoplayer.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import ua.bonny.infoplayer.client.ClientFormat;
import ua.bonny.infoplayer.data.PlayerDetail;
import ua.bonny.infoplayer.data.PlayerSummary;
import ua.bonny.infoplayer.network.DetailRequestPayload;

public final class PlayerDetailScreen extends Screen {
    private static final int BACKGROUND = 0xFF0E1511;
    private static final int HEADER = 0xFF14271C;
    private static final int PANEL = 0xFF19241E;
    private static final int BORDER = 0xFF335242;
    private static final int GREEN = 0xFF48D17A;
    private static final int TEXT = 0xFFF2F6F3;
    private static final int MUTED = 0xFFA8B8AE;
    private static final int GOLD = 0xFFF2C14E;

    private final Screen parent;
    private final PlayerDetail player;
    private PlayerSkinWidget skinWidget;
    private int scrollOffset;
    private int maxScroll;

    public PlayerDetailScreen(Screen parent, PlayerDetail player) {
        super(Component.translatable("screen.infoplayer.details"));
        this.parent = parent;
        this.player = player;
    }

    public Screen parentScreen() {
        return parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("screen.infoplayer.back"), button -> onClose())
                .bounds(18, 10, 82, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.infoplayer.refresh"), button -> {
            button.active = false;
            PacketDistributor.sendToServer(new DetailRequestPayload(player.summary().profile().getId()));
        }).bounds(width - 120, 10, 102, 20).build());

        int modelWidth = width < 500 ? 82 : 132;
        int modelHeight = width < 500 ? 116 : Math.min(230, height - 94);
        skinWidget = new PlayerSkinWidget(
                modelWidth,
                modelHeight,
                minecraft.getEntityModels(),
                minecraft.getSkinManager().lookupInsecure(player.summary().profile()));
        skinWidget.setX(width < 500 ? 22 : 40);
        skinWidget.setY(width < 500 ? 70 : 72);
        addRenderableWidget(skinWidget);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, BACKGROUND);
        graphics.fill(0, 0, width, 40, HEADER);
        graphics.fill(0, 39, width, 40, GREEN);

        PlayerSummary summary = player.summary();
        int contentX = width < 500 ? 124 : 198;
        int contentY = 62;
        int contentWidth = Math.max(150, width - contentX - 24);

        graphics.fill(18, 54, Math.min(contentX - 14, width - 18), height - 18, PANEL);
        graphics.renderOutline(18, 54, Math.min(contentX - 32, width - 36), height - 72, BORDER);

        PlayerFaceRenderer.draw(
                graphics,
                minecraft.getSkinManager().getInsecureSkin(summary.profile()),
                contentX,
                contentY,
                40);
        graphics.drawString(font, summary.profile().getName(), contentX + 52, contentY + 3, TEXT, false);
        graphics.drawString(font, ClientFormat.lastSeen(summary.online(), summary.lastSeen()), contentX + 52,
                contentY + 20, summary.online() ? GREEN : 0xFFE35D6A, false);
        graphics.drawString(font, summary.profile().getId().toString(), contentX + 52, contentY + 35, MUTED, false);

        int gridY = contentY + 62;
        int gap = 8;
        int columns = contentWidth >= 430 ? 2 : 1;
        int itemWidth = (contentWidth - gap * (columns - 1)) / columns;
        List<StatLine> stats = stats();
        int rows = (stats.size() + columns - 1) / columns;
        int viewportHeight = Math.max(0, height - gridY - 18);
        maxScroll = Math.max(0, rows * 38 - viewportHeight);
        scrollOffset = Math.min(scrollOffset, maxScroll);
        graphics.enableScissor(contentX, gridY, width - 18, height - 18);
        for (int index = 0; index < stats.size(); index++) {
            int column = index % columns;
            int row = index / columns;
            int x = contentX + column * (itemWidth + gap);
            int y = gridY + row * 38 - scrollOffset;
            if (y + 31 < gridY || y > height - 18) {
                continue;
            }
            graphics.fill(x, y, x + itemWidth, y + 31, PANEL);
            graphics.renderOutline(x, y, itemWidth, 31, BORDER);
            StatLine stat = stats.get(index);
            String label = font.plainSubstrByWidth(stat.label.getString(), itemWidth - 18);
            String value = font.plainSubstrByWidth(stat.value, itemWidth - 18);
            graphics.drawString(font, label, x + 9, y + 6, MUTED, false);
            graphics.drawString(font, value, x + 9, y + 18, stat.color, false);
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int trackHeight = Math.max(10, height - gridY - 18);
            int thumbHeight = Math.max(18, trackHeight * trackHeight / (trackHeight + maxScroll));
            int thumbY = gridY + (trackHeight - thumbHeight) * scrollOffset / maxScroll;
            graphics.fill(width - 9, gridY, width - 6, height - 18, 0xFF26332B);
            graphics.fill(width - 9, thumbY, width - 6, thumbY + thumbHeight, GREEN);
        }

        for (Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private List<StatLine> stats() {
        PlayerSummary summary = player.summary();
        List<StatLine> stats = new ArrayList<>();
        stats.add(line("screen.infoplayer.stat.level", Integer.toString(summary.experienceLevel()), GOLD));
        stats.add(line("screen.infoplayer.stat.total_xp", Integer.toString(summary.totalExperience()), GOLD));
        stats.add(line("screen.infoplayer.stat.advancements",
                summary.advancementsDone() + " / " + summary.advancementsTotal(), GREEN));
        stats.add(line("screen.infoplayer.stat.playtime", ClientFormat.playTime(summary.playTimeTicks()).getString(), TEXT));
        stats.add(line("screen.infoplayer.stat.health", String.format("%.1f / 20", player.health()), 0xFFE46A72));
        stats.add(line("screen.infoplayer.stat.food", player.foodLevel() + " / 20", 0xFFF1A95B));
        stats.add(line("screen.infoplayer.stat.gamemode", player.gameMode(), TEXT));
        stats.add(line("screen.infoplayer.stat.dimension", player.dimension(), 0xFF72B8F2));
        stats.add(line("screen.infoplayer.stat.position",
                String.format("%.1f, %.1f, %.1f", player.x(), player.y(), player.z()), TEXT));
        stats.add(line("screen.infoplayer.stat.first_seen", ClientFormat.date(player.firstSeen()), TEXT));
        stats.add(line("screen.infoplayer.stat.last_seen", ClientFormat.date(summary.lastSeen()), TEXT));
        return stats;
    }

    private StatLine line(String translationKey, String value, int color) {
        return new StatLine(Component.translatable(translationKey), value, color);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll > 0 && mouseX >= (width < 500 ? 124 : 198)) {
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.round(verticalAmount * 28)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record StatLine(Component label, String value, int color) {
    }
}
