package ua.bonny.infoplayer.client.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public final class GreenButton extends AbstractButton {
    private static final int GREEN = 0xFF48D17A;
    private static final int GREEN_HOVER = 0xFF65E394;
    private static final int DARK = 0xFF18251E;
    private static final int DARK_HOVER = 0xFF22372B;
    private static final int BORDER = 0xFF3C6650;
    private static final int DISABLED = 0xFF26332B;
    private static final int TEXT = 0xFFF4FFF7;
    private static final int DARK_TEXT = 0xFF102018;

    private final Runnable action;
    private final boolean primary;
    private boolean selected;

    public GreenButton(int x, int y, int width, int height, Component message, boolean primary, Runnable action) {
        super(x, y, width, height, message);
        this.primary = primary;
        this.action = action;
    }

    public GreenButton selected(boolean selected) {
        this.selected = selected;
        return this;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void onPress() {
        action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean highlighted = isHoveredOrFocused();
        int fill;
        int border;
        int textColor;
        if (!active) {
            fill = DISABLED;
            border = 0xFF34433A;
            textColor = 0xFF7F9186;
        } else if (primary || selected) {
            fill = highlighted ? GREEN_HOVER : GREEN;
            border = highlighted ? 0xFFA4F4BC : GREEN;
            textColor = DARK_TEXT;
        } else {
            fill = highlighted ? DARK_HOVER : DARK;
            border = highlighted ? GREEN : BORDER;
            textColor = TEXT;
        }

        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), fill);
        graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), border);
        if (selected) {
            graphics.fill(getX(), getY() + getHeight() - 2, getX() + getWidth(), getY() + getHeight(), GREEN_HOVER);
        }
        graphics.drawCenteredString(
                Minecraft.getInstance().font,
                getMessage(),
                getX() + getWidth() / 2,
                getY() + (getHeight() - 8) / 2,
                textColor);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
