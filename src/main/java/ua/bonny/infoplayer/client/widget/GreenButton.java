package ua.bonny.infoplayer.client.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public final class GreenButton extends AbstractButton {
    private static final int PRIMARY = 0xFF267A4B;
    private static final int PRIMARY_HOVER = 0xFF319A5E;
    private static final int DARK = 0xFF17231D;
    private static final int DARK_HOVER = 0xFF20342A;
    private static final int BORDER = 0xFF3B5D4B;
    private static final int DISABLED = 0xFF222D27;
    private static final int TEXT = 0xFFF4FFF7;
    private static final int MUTED = 0xFF77877D;
    private static final int ACCENT = 0xFF55DC86;

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
            textColor = MUTED;
        } else if (primary) {
            fill = highlighted ? PRIMARY_HOVER : PRIMARY;
            border = highlighted ? ACCENT : 0xFF39965F;
            textColor = TEXT;
        } else if (selected) {
            fill = highlighted ? DARK_HOVER : DARK;
            border = ACCENT;
            textColor = TEXT;
        } else {
            fill = highlighted ? DARK_HOVER : DARK;
            border = highlighted ? ACCENT : BORDER;
            textColor = TEXT;
        }

        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), fill);
        graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), border);
        if (selected) {
            graphics.fill(getX() + 1, getY() + getHeight() - 3, getX() + getWidth() - 1, getY() + getHeight() - 1, ACCENT);
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
