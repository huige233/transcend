package com.huige233.transcend.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Scales both drawing and pointer input for screens with a minimum usable layout. */
abstract class FittedScreen extends Screen {
    private UiViewport viewport = new UiViewport(1, 1, 1);

    protected FittedScreen(Component title) { super(title); }
    protected abstract int minimumWidth();
    protected abstract int minimumHeight();

    @Override protected void init() {
        viewport = UiViewport.fit(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight(), minimumWidth(), minimumHeight());
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
        graphics.pose().pushPose();
        try {
            graphics.pose().scale((float) viewport.scale(), (float) viewport.scale(), 1);
            renderContent(graphics, viewport.pixel(mouseX), viewport.pixel(mouseY), partialTick);
        } finally {
            graphics.pose().popPose();
        }
        renderHints(graphics, viewport.pixel(mouseX), viewport.pixel(mouseY), mouseX, mouseY);
    }

    protected void renderHints(GuiGraphics graphics, int mouseX, int mouseY, int tooltipX, int tooltipY) { }

    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public final boolean mouseClicked(double x, double y, int button) {
        boolean handled = clickContent(viewport.logical(x), viewport.logical(y), button);
        // A tab action may replace all widgets during the click callback.
        if (getFocused() != null && !children().contains(getFocused())) setFocused(null);
        return handled;
    }
    protected boolean clickContent(double x, double y, int button) { return super.mouseClicked(x, y, button); }
    @Override public final boolean mouseScrolled(double x, double y, double delta) {
        return scrollContent(viewport.logical(x), viewport.logical(y), delta);
    }
    protected boolean scrollContent(double x, double y, double delta) { return super.mouseScrolled(x, y, delta); }
    @Override public boolean mouseReleased(double x, double y, int button) {
        return super.mouseReleased(viewport.logical(x), viewport.logical(y), button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return super.mouseDragged(viewport.logical(x), viewport.logical(y), button,
                viewport.logical(dx), viewport.logical(dy));
    }
    @Override public void mouseMoved(double x, double y) { super.mouseMoved(viewport.logical(x), viewport.logical(y)); }
}
