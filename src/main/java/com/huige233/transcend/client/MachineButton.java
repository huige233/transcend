package com.huige233.transcend.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;

/** A compact button with a visible focus outline and bounded, fully narrated text. */
final class MachineButton extends Button {
    boolean selected;
    private Tooltip labelTooltip;
    MachineButton(Builder builder) {
        super(builder);
        labelTooltip = Tooltip.create(getMessage());
        setTooltip(labelTooltip);
    }

    @Override public void setMessage(net.minecraft.network.chat.Component message) {
        super.setMessage(message);
        if (getTooltip() == labelTooltip) {
            labelTooltip = Tooltip.create(message);
            setTooltip(labelTooltip);
        }
    }

    @Override public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        int x = getX(), y = getY();
        boolean highlighted = active && isHoveredOrFocused();
        graphics.fill(x, y, x + getWidth(), y + getHeight(),
                !active ? 0xff111a27 : selected ? 0xff244650 : highlighted ? 0xff294458 : 0xff1b2c3d);
        graphics.renderOutline(x, y, getWidth(), getHeight(),
                isFocused() ? 0xffe2f4ff : selected || highlighted ? MachinePanelStyle.ACCENT : MachinePanelStyle.BORDER);
        if (active) graphics.hLine(x + 1, x + getWidth() - 2, y + 1, 0xff354c60);
        var text = MachinePanelStyle.clipped(font, getMessage(), getWidth() - 8);
        graphics.drawString(font, text, x + (getWidth() - font.width(text)) / 2,
                y + (getHeight() - font.lineHeight) / 2, active ? MachinePanelStyle.TEXT : 0xff8292a3, false);
    }
}
