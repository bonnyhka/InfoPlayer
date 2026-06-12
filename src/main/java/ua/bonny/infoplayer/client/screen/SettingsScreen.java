package ua.bonny.infoplayer.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import ua.bonny.infoplayer.client.widget.GreenButton;
import ua.bonny.infoplayer.client.widget.GreenToggle;
import ua.bonny.infoplayer.data.PlayerListEntry;
import ua.bonny.infoplayer.data.PrivacyOption;
import ua.bonny.infoplayer.network.SettingsUpdatePayload;

public final class SettingsScreen extends Screen {
    private static final int BACKGROUND = 0xFF0E1511;
    private static final int HEADER = 0xFF14271C;
    private static final int PANEL = 0xFF19241E;
    private static final int BORDER = 0xFF335242;
    private static final int GREEN = 0xFF48D17A;
    private static final int TEXT = 0xFFF2F6F3;
    private static final int MUTED = 0xFFA8B8AE;
    private static final int ROW_HEIGHT = 54;

    private final PlayerListScreen parent;
    private final List<ToggleRow> rows = new ArrayList<>();
    private long visibleMask;
    private int scrollOffset;
    private int maxScroll;

    public SettingsScreen(PlayerListScreen parent, long visibleMask) {
        super(Component.literal("Моя приватность"));
        this.parent = parent;
        this.visibleMask = PrivacyOption.sanitize(visibleMask);
    }

    @Override
    protected void init() {
        rows.clear();
        addRenderableWidget(new GreenButton(
                18, 10, 82, 24, Component.literal("< Назад"), false, this::onClose));
        for (PrivacyOption option : PrivacyOption.values()) {
            SettingText text = text(option);
            GreenToggle toggle = new GreenToggle(
                    0,
                    0,
                    option.visible(visibleMask),
                    Component.literal(text.label()),
                    () -> toggle(option));
            rows.add(new ToggleRow(option, text, toggle));
            addRenderableWidget(toggle);
        }
        updateTogglePositions();
    }

    public void updateSettings(
            boolean administrator,
            long ownVisibleMask,
            List<PlayerListEntry> players) {
        parent.updatePlayers(administrator, ownVisibleMask, players);
        visibleMask = PrivacyOption.sanitize(ownVisibleMask);
        for (ToggleRow row : rows) {
            row.toggle().setValue(row.option().visible(visibleMask));
        }
    }

    private void toggle(PrivacyOption option) {
        GreenToggle toggle = rows.stream()
                .filter(row -> row.option() == option)
                .findFirst()
                .orElseThrow()
                .toggle();
        visibleMask = option.setVisible(visibleMask, toggle.value());
        PacketDistributor.sendToServer(new SettingsUpdatePayload(visibleMask));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, BACKGROUND);
        graphics.fillGradient(0, 0, width, 42, 0xFF173223, HEADER);
        graphics.fill(0, 41, width, 42, GREEN);
        graphics.drawCenteredString(font, title, width / 2, 17, TEXT);

        int panelX = panelX();
        int panelWidth = panelWidth();
        int panelTop = 58;
        int panelBottom = height - 16;
        graphics.fill(panelX, panelTop, panelX + panelWidth, panelBottom, PANEL);
        graphics.renderOutline(panelX, panelTop, panelWidth, panelBottom - panelTop, BORDER);

        int contentTop = panelTop + 12;
        int contentBottom = panelBottom - 12;
        graphics.enableScissor(panelX + 1, contentTop, panelX + panelWidth - 1, contentBottom);
        for (int index = 0; index < rows.size(); index++) {
            ToggleRow row = rows.get(index);
            int y = contentTop + index * ROW_HEIGHT - scrollOffset;
            if (y + ROW_HEIGHT < contentTop || y > contentBottom) {
                continue;
            }
            graphics.drawString(font, row.text().label(), panelX + 18, y + 7, TEXT, false);
            String description = font.plainSubstrByWidth(row.text().description(), panelWidth - 112);
            graphics.drawString(font, description, panelX + 18, y + 23, MUTED, false);
            String state = row.option().visible(visibleMask) ? "Показывается" : "Скрыто";
            graphics.drawString(
                    font,
                    state,
                    panelX + 18,
                    y + 38,
                    row.option().visible(visibleMask) ? GREEN : MUTED,
                    false);
            if (index < rows.size() - 1) {
                graphics.fill(
                        panelX + 18,
                        y + ROW_HEIGHT - 1,
                        panelX + panelWidth - 18,
                        y + ROW_HEIGHT,
                        BORDER);
            }
        }
        graphics.disableScissor();
        for (Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
        renderScrollbar(graphics, contentTop, contentBottom);
    }

    private void updateTogglePositions() {
        int contentTop = 70;
        int contentBottom = height - 28;
        int available = Math.max(1, contentBottom - contentTop);
        maxScroll = Math.max(0, rows.size() * ROW_HEIGHT - available);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        int x = panelX() + panelWidth() - 66;
        for (int index = 0; index < rows.size(); index++) {
            GreenToggle toggle = rows.get(index).toggle();
            int y = contentTop + index * ROW_HEIGHT + 15 - scrollOffset;
            toggle.setX(x);
            toggle.setY(y);
            toggle.visible = y >= contentTop && y + toggle.getHeight() <= contentBottom;
        }
    }

    private void renderScrollbar(GuiGraphics graphics, int top, int bottom) {
        if (maxScroll <= 0) {
            return;
        }
        int trackHeight = bottom - top;
        int thumbHeight = Math.max(18, trackHeight * trackHeight / (trackHeight + maxScroll));
        int thumbY = top + (trackHeight - thumbHeight) * scrollOffset / maxScroll;
        int x = panelX() + panelWidth() - 6;
        graphics.fill(x, top, x + 3, bottom, 0xFF26332B);
        graphics.fill(x, thumbY, x + 3, thumbY + thumbHeight, GREEN);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll > 0 && mouseY >= 58 && mouseY <= height - 16) {
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.round(verticalAmount * 30)));
            updateTogglePositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private int panelWidth() {
        return Math.min(620, width - 36);
    }

    private int panelX() {
        return (width - panelWidth()) / 2;
    }

    private SettingText text(PrivacyOption option) {
        return switch (option) {
            case ONLINE_STATUS -> new SettingText("Статус в сети", "Показывать, находитесь ли вы сейчас на сервере");
            case LAST_ACTIVITY -> new SettingText("Последняя активность", "Показывать время последнего выхода");
            case EXPERIENCE -> new SettingText("Уровень и опыт", "Показывать уровень и общее количество опыта");
            case ADVANCEMENTS -> new SettingText("Достижения", "Показывать прогресс выполненных достижений");
            case PLAY_TIME -> new SettingText("Время игры", "Показывать время, проведённое на сервере");
            case HEALTH -> new SettingText("Здоровье", "Показывать текущее количество здоровья");
            case FOOD -> new SettingText("Голод", "Показывать текущий уровень голода");
            case GAME_MODE -> new SettingText("Режим игры", "Показывать выживание, творчество и другие режимы");
            case DIMENSION -> new SettingText("Измерение", "Показывать мир, Незер или Энд");
            case COORDINATES -> new SettingText("Координаты", "Показывать ваше точное местоположение");
            case FIRST_SEEN -> new SettingText("Первый вход", "Показывать дату первого входа на сервер");
            case INVENTORY -> new SettingText("Инвентарь", "Показывать предметы, броню и содержимое рук");
            case CURIOS -> new SettingText("Curios", "Показывать аксессуары из слотов Curios");
        };
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record SettingText(String label, String description) {
    }

    private record ToggleRow(PrivacyOption option, SettingText text, GreenToggle toggle) {
    }
}
