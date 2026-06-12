package ua.bonny.infoplayer.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import ua.bonny.infoplayer.client.ClientFormat;
import ua.bonny.infoplayer.client.widget.GreenButton;
import ua.bonny.infoplayer.data.PlayerDetail;
import ua.bonny.infoplayer.data.PlayerSummary;
import ua.bonny.infoplayer.network.DetailRequestPayload;
import ua.bonny.infoplayer.network.TakeItemRequestPayload;

public final class PlayerDetailScreen extends Screen {
    private static final int BACKGROUND = 0xFF0E1511;
    private static final int HEADER = 0xFF14271C;
    private static final int PANEL = 0xFF19241E;
    private static final int BORDER = 0xFF335242;
    private static final int GREEN = 0xFF48D17A;
    private static final int TEXT = 0xFFF2F6F3;
    private static final int MUTED = 0xFFA8B8AE;
    private static final int GOLD = 0xFFF2C14E;
    private static final int DANGER = 0xFFE35D6A;
    private static final int SLOT = 22;

    private final Screen parent;
    private final boolean administrator;
    private final PlayerDetail player;
    private final boolean startWithInventory;
    private final List<SlotBounds> inventorySlotBounds = new ArrayList<>();
    private RemotePlayer previewPlayer;
    private GreenButton overviewButton;
    private GreenButton inventoryButton;
    private GreenButton refreshButton;
    private Tab tab;
    private int scrollOffset;
    private int maxScroll;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    public PlayerDetailScreen(Screen parent, boolean administrator, PlayerDetail player) {
        this(parent, administrator, player, false);
    }

    public PlayerDetailScreen(Screen parent, boolean administrator, PlayerDetail player, boolean startWithInventory) {
        super(Component.literal("Данные игрока"));
        this.parent = parent;
        this.administrator = administrator;
        this.player = player;
        this.startWithInventory = startWithInventory;
        this.tab = startWithInventory ? Tab.INVENTORY : Tab.OVERVIEW;
    }

    public Screen parentScreen() {
        return parent;
    }

    public boolean inventoryOpen() {
        return tab == Tab.INVENTORY;
    }

    @Override
    protected void init() {
        addRenderableWidget(new GreenButton(
                18, 10, 82, 24, Component.literal("< Назад"), false, this::onClose));

        refreshButton = new GreenButton(
                width - 126,
                10,
                108,
                24,
                Component.literal("Обновить"),
                true,
                () -> {
                    refreshButton.active = false;
                    PacketDistributor.sendToServer(new DetailRequestPayload(player.summary().profile().getId()));
                });
        addRenderableWidget(refreshButton);

        int tabsX = Math.max(18, width / 2 - 116);
        int tabsY = compact() ? 76 : 104;
        overviewButton = new GreenButton(
                tabsX, tabsY, 112, 26, Component.literal("Обзор"), false, () -> setTab(Tab.OVERVIEW));
        inventoryButton = new GreenButton(
                tabsX + 120, tabsY, 112, 26, Component.literal("Инвентарь"), false, () -> setTab(Tab.INVENTORY));
        addRenderableWidget(overviewButton);
        addRenderableWidget(inventoryButton);

        createPreviewPlayer();
        setTab(startWithInventory ? Tab.INVENTORY : Tab.OVERVIEW);
    }

    private void createPreviewPlayer() {
        if (minecraft.level == null) {
            previewPlayer = null;
            return;
        }

        previewPlayer = new RemotePlayer(minecraft.level, player.summary().profile());
        List<ItemStack> items = player.inventory();
        for (int index = 0; index < 36; index++) {
            previewPlayer.getInventory().items.set(index, items.get(index).copy());
        }
        for (int index = 0; index < 4; index++) {
            previewPlayer.getInventory().armor.set(index, items.get(36 + index).copy());
        }
        previewPlayer.getInventory().offhand.set(0, items.get(40).copy());
        previewPlayer.getInventory().selected = Math.max(0, Math.min(8, player.selectedSlot()));
    }

    private void setTab(Tab newTab) {
        tab = newTab;
        scrollOffset = 0;
        if (overviewButton != null) {
            overviewButton.setSelected(tab == Tab.OVERVIEW);
            inventoryButton.setSelected(tab == Tab.INVENTORY);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hoveredStack = ItemStack.EMPTY;
        graphics.fill(0, 0, width, height, BACKGROUND);
        graphics.fillGradient(0, 0, width, 42, 0xFF173223, HEADER);
        graphics.fill(0, 41, width, 42, GREEN);
        renderPlayerHeader(graphics);

        if (tab == Tab.OVERVIEW) {
            renderOverview(graphics, mouseX, mouseY);
        } else {
            renderInventory(graphics, mouseX, mouseY);
        }

        for (Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
        if (!hoveredStack.isEmpty()) {
            graphics.renderTooltip(font, hoveredStack, mouseX, mouseY);
        }
    }

    private void renderPlayerHeader(GuiGraphics graphics) {
        PlayerSummary summary = player.summary();
        int cardX = 18;
        int cardY = compact() ? 46 : 52;
        int cardWidth = width - 36;
        int cardHeight = compact() ? 24 : 42;
        graphics.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, PANEL);
        graphics.renderOutline(cardX, cardY, cardWidth, cardHeight, BORDER);
        graphics.fill(cardX, cardY, cardX + 4, cardY + cardHeight, summary.online() ? GREEN : 0xFFE35D6A);
        int faceSize = compact() ? 18 : 32;
        PlayerFaceRenderer.draw(
                graphics,
                minecraft.getSkinManager().getInsecureSkin(summary.profile()),
                cardX + 12,
                cardY + (cardHeight - faceSize) / 2,
                faceSize);
        int textX = cardX + (compact() ? 38 : 54);
        graphics.drawString(font, summary.profile().getName(), textX, cardY + (compact() ? 8 : 7), TEXT, false);
        if (!compact()) {
            graphics.drawString(font, ClientFormat.lastSeen(summary.online(), summary.lastSeen()), textX,
                    cardY + 23, summary.online() ? GREEN : 0xFFE35D6A, false);
            String rightText = "Уровень " + summary.experienceLevel() + "  |  " + player.gameMode();
            graphics.drawString(font, rightText, cardX + cardWidth - 12 - font.width(rightText), cardY + 17, MUTED, false);
        }
    }

    private void renderOverview(GuiGraphics graphics, int mouseX, int mouseY) {
        int contentY = compact() ? 108 : 146;
        int availableHeight = Math.max(80, height - contentY - 18);
        int modelSize = Math.min(availableHeight, compact() ? 96 : Math.min(250, Math.max(120, width / 3)));
        int modelLeft = 18;
        int modelTop = contentY - 4;
        int modelRight = modelLeft + modelSize;
        int modelBottom = modelTop + modelSize;
        int contentX = modelRight + 14;
        int contentWidth = Math.max(150, width - contentX - 24);

        graphics.fill(modelLeft, modelTop, modelRight, modelBottom, PANEL);
        graphics.renderOutline(modelLeft, modelTop, modelSize, modelSize, BORDER);
        if (previewPlayer != null) {
            int inset = compact() ? 5 : 10;
            int scale = Math.max(34, Math.round(modelSize * 0.31F));
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics,
                    modelLeft + inset,
                    modelTop + inset,
                    modelRight - inset,
                    modelBottom - inset,
                    scale,
                    0.0625F,
                    mouseX,
                    mouseY,
                    previewPlayer);
        }

        int gap = 8;
        int columns = contentWidth >= 430 ? 2 : 1;
        int itemWidth = (contentWidth - gap * (columns - 1)) / columns;
        List<StatLine> stats = stats();
        int rows = (stats.size() + columns - 1) / columns;
        int viewportHeight = Math.max(0, height - contentY - 18);
        maxScroll = Math.max(0, rows * 42 - viewportHeight);
        scrollOffset = Math.min(scrollOffset, maxScroll);
        graphics.enableScissor(contentX, contentY, width - 18, height - 18);
        for (int index = 0; index < stats.size(); index++) {
            int column = index % columns;
            int row = index / columns;
            int x = contentX + column * (itemWidth + gap);
            int y = contentY + row * 42 - scrollOffset;
            if (y + 35 < contentY || y > height - 18) {
                continue;
            }
            graphics.fill(x, y, x + itemWidth, y + 35, PANEL);
            graphics.renderOutline(x, y, itemWidth, 35, BORDER);
            graphics.fill(x, y, x + 3, y + 35, stats.get(index).color);
            StatLine stat = stats.get(index);
            String label = font.plainSubstrByWidth(stat.label, itemWidth - 18);
            String value = font.plainSubstrByWidth(stat.value, itemWidth - 18);
            graphics.drawString(font, label, x + 10, y + 7, MUTED, false);
            graphics.drawString(font, value, x + 10, y + 21, stat.color, false);
        }
        graphics.disableScissor();
        renderScrollbar(graphics, contentY);
    }

    private void renderInventory(GuiGraphics graphics, int mouseX, int mouseY) {
        inventorySlotBounds.clear();
        List<ItemStack> items = player.inventory();
        int step = compact() ? 20 : SLOT;
        int inventoryWidth = 9 * step;
        int panelWidth = inventoryWidth + (compact() ? 158 : 210);
        int panelX = Math.max(18, (width - panelWidth) / 2);
        int panelY = compact() ? 106 : 142;
        int panelHeight = Math.max(1, Math.min(height - panelY - 8, compact() ? 142 : 210));
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL);
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, BORDER);

        int occupied = (int) items.stream().filter(stack -> !stack.isEmpty()).count();
        int mainX = panelX + 14;
        int mainY = panelY + (compact() ? 42 : 60);
        graphics.drawString(font, Component.literal("Инвентарь игрока"), mainX, panelY + 10, TEXT, false);
        String occupancy = "Занято " + occupied + " из 41";
        graphics.drawString(font, occupancy, panelX + panelWidth - 14 - font.width(occupancy), panelY + 10,
                occupied >= 36 ? GOLD : GREEN, false);
        int occupancyWidth = panelWidth - 28;
        graphics.fill(mainX, panelY + 25, mainX + occupancyWidth, panelY + 29, 0xFF2C3931);
        graphics.fill(mainX, panelY + 25,
                mainX + Math.round(occupancyWidth * (occupied / 41.0F)), panelY + 29, GREEN);
        String actionHint = font.plainSubstrByWidth(inventoryActionHint(), inventoryWidth);
        graphics.drawString(font, actionHint, mainX, panelY + 34, canTakeItems() ? GREEN : MUTED, false);

        graphics.drawString(font, Component.literal("Рюкзак"), mainX, mainY - 13, MUTED, false);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int index = 9 + row * 9 + column;
                renderSlot(graphics, items.get(index), mainX + column * step, mainY + row * step,
                        mouseX, mouseY, false, index);
            }
        }

        int hotbarY = mainY + 3 * step + (compact() ? 12 : 20);
        graphics.drawString(font, Component.literal("Панель быстрого доступа"), mainX, hotbarY - 12, MUTED, false);
        for (int column = 0; column < 9; column++) {
            renderSlot(graphics, items.get(column), mainX + column * step, hotbarY,
                    mouseX, mouseY, column == player.selectedSlot(), column);
        }

        int dividerX = mainX + inventoryWidth + 16;
        graphics.fill(dividerX, panelY + 38, dividerX + 1, panelY + panelHeight - 12, BORDER);
        int equipmentX = dividerX + 16;
        graphics.drawString(font, Component.literal("Экипировка"), equipmentX, mainY - 13, MUTED, false);
        int[] armorSlots = {39, 38, 37, 36};
        String[] armorLabels = {"Шлем", "Нагрудник", "Поножи", "Ботинки"};
        for (int row = 0; row < armorSlots.length; row++) {
            int y = mainY + row * step;
            renderSlot(graphics, items.get(armorSlots[row]), equipmentX, y, mouseX, mouseY, false, armorSlots[row]);
            graphics.drawString(font, armorLabels[row], equipmentX + SLOT + 7, y + 7, TEXT, false);
        }
        int offhandY = mainY + 4 * step + (compact() ? 7 : 12);
        renderSlot(graphics, items.get(40), equipmentX, offhandY, mouseX, mouseY, false, 40);
        graphics.drawString(font, Component.literal("Левая рука"), equipmentX + SLOT + 7, offhandY + 7, TEXT, false);

        if (!player.summary().online()) {
            String snapshot = "Снимок на " + ClientFormat.date(player.summary().lastSeen());
            graphics.drawString(font, snapshot, panelX + panelWidth - 14 - font.width(snapshot),
                    panelY + panelHeight - 12, GOLD, false);
        }
    }

    private void renderSlot(
            GuiGraphics graphics,
            ItemStack stack,
            int x,
            int y,
            int mouseX,
            int mouseY,
            boolean selected,
            int slotIndex) {
        int fill = selected ? 0xFF264D36 : 0xFF111A15;
        int border = selected ? GREEN : 0xFF466052;
        graphics.fill(x, y, x + 20, y + 20, fill);
        graphics.renderOutline(x, y, 20, 20, border);
        if (selected) {
            graphics.fill(x + 2, y + 17, x + 18, y + 19, GREEN);
        }
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x + 2, y + 2);
            graphics.renderItemDecorations(font, stack, x + 2, y + 2);
        }
        if (mouseX >= x && mouseX < x + 20 && mouseY >= y && mouseY < y + 20) {
            int hoverColor = canTakeItems() && !stack.isEmpty() ? 0x66E35D6A : 0x553FE57A;
            graphics.fill(x + 1, y + 1, x + 19, y + 19, hoverColor);
            if (canTakeItems() && !stack.isEmpty()) {
                graphics.renderOutline(x, y, 20, 20, DANGER);
            }
            hoveredStack = stack;
        }
        inventorySlotBounds.add(new SlotBounds(x, y, 20, 20, slotIndex, stack));
    }

    private String inventoryActionHint() {
        if (!administrator) {
            return "Только просмотр";
        }
        if (!player.summary().online()) {
            return "Офлайн: только просмотр";
        }
        if (minecraft.player != null
                && minecraft.player.getUUID().equals(player.summary().profile().getId())) {
            return "Свой инвентарь: только просмотр";
        }
        return "ЛКМ по предмету: забрать";
    }

    private boolean canTakeItems() {
        return administrator
                && player.summary().online()
                && minecraft.player != null
                && !minecraft.player.getUUID().equals(player.summary().profile().getId());
    }

    private void renderScrollbar(GuiGraphics graphics, int contentY) {
        if (maxScroll <= 0) {
            return;
        }
        int trackHeight = Math.max(10, height - contentY - 18);
        int thumbHeight = Math.max(18, trackHeight * trackHeight / (trackHeight + maxScroll));
        int thumbY = contentY + (trackHeight - thumbHeight) * scrollOffset / maxScroll;
        graphics.fill(width - 9, contentY, width - 6, height - 18, 0xFF26332B);
        graphics.fill(width - 9, thumbY, width - 6, thumbY + thumbHeight, GREEN);
    }

    private List<StatLine> stats() {
        PlayerSummary summary = player.summary();
        List<StatLine> stats = new ArrayList<>();
        stats.add(line("Уровень", Integer.toString(summary.experienceLevel()), GOLD));
        stats.add(line("Всего опыта", Integer.toString(summary.totalExperience()), GOLD));
        stats.add(line("Достижения", summary.advancementsDone() + " / " + summary.advancementsTotal(), GREEN));
        stats.add(line("Время на сервере", ClientFormat.playTime(summary.playTimeTicks()).getString(), TEXT));
        stats.add(line("Здоровье", String.format("%.1f / 20", player.health()), 0xFFE46A72));
        stats.add(line("Голод", player.foodLevel() + " / 20", 0xFFF1A95B));
        stats.add(line("Режим игры", player.gameMode(), TEXT));
        stats.add(line("Измерение", player.dimension(), 0xFF72B8F2));
        stats.add(line("Координаты", String.format("%.1f, %.1f, %.1f", player.x(), player.y(), player.z()), TEXT));
        stats.add(line("Первый вход", ClientFormat.date(player.firstSeen()), TEXT));
        stats.add(line("Последняя активность", ClientFormat.date(summary.lastSeen()), TEXT));
        return stats;
    }

    private StatLine line(String label, String value, int color) {
        return new StatLine(label, value, color);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0 && tab == Tab.INVENTORY && canTakeItems()) {
            for (SlotBounds bounds : inventorySlotBounds) {
                if (bounds.contains(mouseX, mouseY) && !bounds.stack.isEmpty()) {
                    PacketDistributor.sendToServer(new TakeItemRequestPayload(
                            player.summary().profile().getId(),
                            bounds.slotIndex));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (tab == Tab.OVERVIEW && maxScroll > 0 && mouseY >= (compact() ? 108 : 142)) {
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.round(verticalAmount * 28)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean compact() {
        return height < 300;
    }

    private enum Tab {
        OVERVIEW,
        INVENTORY
    }

    private record StatLine(String label, String value, int color) {
    }

    private record SlotBounds(int x, int y, int width, int height, int slotIndex, ItemStack stack) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
