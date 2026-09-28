package com.huige233.transcend.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.ArrayList;
import java.util.List;

/** Shared fitting, slot interaction and full-text hints for machine screens. */
abstract class MachineScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private UiViewport viewport = new UiViewport(1, 1, 1);
    private final List<TextHint> clippedLabels = new ArrayList<>();

    protected MachineScreen(T menu, Inventory inventory, Component title) { super(menu, inventory, title); }

    @Override protected void init() {
        viewport = UiViewport.fit(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight(), imageWidth + 16, imageHeight + 16);
        width = viewport.width();
        height = viewport.height();
        super.init();
    }

    @Override public void setTooltipForNextRenderPass(
            java.util.List<net.minecraft.util.FormattedCharSequence> lines,
            net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner positioner, boolean override) {
        super.setTooltipForNextRenderPass(lines, viewport.scale() < 1
                ? net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE : positioner, override);
    }

    @Override public final void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int logicalX = viewport.pixel(mouseX), logicalY = viewport.pixel(mouseY);
        clippedLabels.clear();
        graphics.pose().pushPose();
        try {
            graphics.pose().scale((float) viewport.scale(), (float) viewport.scale(), 1);
            super.render(graphics, logicalX, logicalY, partialTick);
        } finally {
            graphics.pose().popPose();
        }
        // Tooltips stay at native text size and are clamped to the real window.
        renderTooltip(graphics, mouseX, mouseY);
        if (hoveredSlot != null && hoveredSlot.hasItem()) return;
        if (renderHints(graphics, logicalX, logicalY, mouseX, mouseY)) return;
        for (TextHint hint : clippedLabels) {
            if (isHovering(hint.x(), hint.y(), hint.width(), font.lineHeight, logicalX, logicalY)) {
                graphics.renderTooltip(font, font.split(hint.text(), Math.min(260, graphics.guiWidth() - 16)), mouseX, mouseY);
                break;
            }
        }
    }

    protected boolean renderHints(GuiGraphics graphics, int mouseX, int mouseY, int tooltipX, int tooltipY) { return false; }

    protected void drawLabel(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        MachinePanelStyle.label(graphics, font, text, x, y, width, color);
        if (font.width(text) > width) clippedLabels.add(new TextHint(x, y, width, text));
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        return clickContent(viewport.logical(x), viewport.logical(y), button);
    }
    protected boolean clickContent(double x, double y, int button) { return super.mouseClicked(x, y, button); }
    @Override public boolean mouseReleased(double x, double y, int button) {
        return releaseContent(viewport.logical(x), viewport.logical(y), button);
    }
    protected boolean releaseContent(double x, double y, int button) { return super.mouseReleased(x, y, button); }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return dragContent(viewport.logical(x), viewport.logical(y), button,
                viewport.logical(dx), viewport.logical(dy));
    }
    protected boolean dragContent(double x, double y, int button, double dx, double dy) { return super.mouseDragged(x, y, button, dx, dy); }
    @Override public final boolean mouseScrolled(double x, double y, double delta) {
        return scrollContent(viewport.logical(x), viewport.logical(y), delta);
    }
    protected boolean scrollContent(double x, double y, double delta) { return super.mouseScrolled(x, y, delta); }
    @Override public void mouseMoved(double x, double y) { super.mouseMoved(viewport.logical(x), viewport.logical(y)); }

    private record TextHint(int x, int y, int width, Component text) { }
}
