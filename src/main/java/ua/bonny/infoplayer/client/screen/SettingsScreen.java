package ua.bonny.infoplayer.client.screen;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import ua.bonny.infoplayer.client.widget.GreenButton;
import ua.bonny.infoplayer.client.widget.GreenToggle;
import ua.bonny.infoplayer.data.PlayerSummary;
import ua.bonny.infoplayer.network.SettingsUpdatePayload;

public final class SettingsScreen extends Screen {
    private static final int BACKGROUND = 0xFF0E1511;
    private static final int HEADER = 0xFF14271C;
    private static final int PANEL = 0xFF19241E;
    private static final int BORDER = 0xFF335242;
    private static final int GREEN = 0xFF48D17A;
    private static final int TEXT = 0xFFF2F6F3;
    private static final int MUTED = 0xFFA8B8AE;

    private final PlayerListScreen parent;
    private boolean showCoordinatesToPlayers;
    private boolean showInventoryToPlayers;
    private GreenToggle coordinatesToggle;
    private GreenToggle inventoryToggle;

    public SettingsScreen(
            PlayerListScreen parent,
            boolean showCoordinatesToPlayers,
            boolean showInventoryToPlayers) {
        super(Component.literal("Настройки доступа"));
        this.parent = parent;
        this.showCoordinatesToPlayers = showCoordinatesToPlayers;
        this.showInventoryToPlayers = showInventoryToPlayers;
    }

    @Override
    protected void init() {
        addRenderableWidget(new GreenButton(
                18, 10, 82, 24, Component.literal("< Назад"), false, this::onClose));

        int panelWidth = Math.min(560, width - 36);
        int panelX = (width - panelWidth) / 2;
        int rowX = panelX + panelWidth - 66;
        int firstRowY = Math.max(82, height / 2 - 66);

        coordinatesToggle = new GreenToggle(
                rowX,
                firstRowY + 17,
                showCoordinatesToPlayers,
                Component.literal("Показывать координаты другим игрокам"),
                () -> {
                    showCoordinatesToPlayers = coordinatesToggle.value();
                    sendSettings();
                });
        inventoryToggle = new GreenToggle(
                rowX,
                firstRowY + 81,
                showInventoryToPlayers,
                Component.literal("Показывать инвентарь другим игрокам"),
                () -> {
                    showInventoryToPlayers = inventoryToggle.value();
                    sendSettings();
                });
        addRenderableWidget(coordinatesToggle);
        addRenderableWidget(inventoryToggle);
    }

    public void updateSettings(
            boolean administrator,
            boolean coordinatesVisible,
            boolean inventoryVisible,
            List<PlayerSummary> players) {
        parent.updatePlayers(administrator, coordinatesVisible, inventoryVisible, players);
        if (!administrator) {
            minecraft.setScreen(parent);
            return;
        }
        showCoordinatesToPlayers = coordinatesVisible;
        showInventoryToPlayers = inventoryVisible;
        if (coordinatesToggle != null) {
            coordinatesToggle.setValue(coordinatesVisible);
            inventoryToggle.setValue(inventoryVisible);
        }
    }

    private void sendSettings() {
        PacketDistributor.sendToServer(new SettingsUpdatePayload(
                showCoordinatesToPlayers,
                showInventoryToPlayers));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, BACKGROUND);
        graphics.fillGradient(0, 0, width, 42, 0xFF173223, HEADER);
        graphics.fill(0, 41, width, 42, GREEN);
        graphics.drawCenteredString(font, title, width / 2, 17, TEXT);

        int panelWidth = Math.min(560, width - 36);
        int panelX = (width - panelWidth) / 2;
        int firstRowY = Math.max(82, height / 2 - 66);
        int panelY = firstRowY - 22;
        int panelHeight = 150;
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL);
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, BORDER);

        renderSettingRow(
                graphics,
                panelX + 18,
                firstRowY,
                "Координаты игроков",
                "Разрешить игрокам видеть координаты других",
                showCoordinatesToPlayers);
        graphics.fill(panelX + 18, firstRowY + 58, panelX + panelWidth - 18, firstRowY + 59, BORDER);
        renderSettingRow(
                graphics,
                panelX + 18,
                firstRowY + 64,
                "Инвентарь игроков",
                "Разрешить просмотр инвентаря и Curios других",
                showInventoryToPlayers);

        for (Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderSettingRow(
            GuiGraphics graphics,
            int x,
            int y,
            String label,
            String description,
            boolean enabled) {
        graphics.drawString(font, label, x, y + 10, TEXT, false);
        graphics.drawString(font, description, x, y + 28, MUTED, false);
        String state = enabled ? "Включено" : "Выключено";
        graphics.drawString(
                font,
                state,
                x,
                y + 43,
                enabled ? GREEN : MUTED,
                false);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
