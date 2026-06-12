package ua.bonny.infoplayer.client.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public final class GreenToggle extends AbstractButton {
    private static final int GREEN = 0xFF48D17A;
    private static final int TRACK_OFF = 0xFF26352D;
    private static final int BORDER = 0xFF466052;
    private static final int KNOB = 0xFFF2F6F3;

    private final Runnable action;
    private boolean value;

    public GreenToggle(int x, int y, boolean value, Component narration, Runnable action) {
        super(x, y, 44, 22, narration);
        this.value = value;
        this.action = action;
    }

    public boolean value() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    @Override
    public void onPress() {
        value = !value;
        action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int fill = value ? GREEN : TRACK_OFF;
        int border = isHoveredOrFocused() ? 0xFF74E69B : BORDER;
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), fill);
        graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), border);
        int knobX = value ? getX() + 24 : getX() + 3;
        graphics.fill(knobX, getY() + 3, knobX + 17, getY() + 19, KNOB);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
