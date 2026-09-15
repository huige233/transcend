package com.huige233.transcend.client;

import com.huige233.transcend.menu.LongStorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.Locale;

/** 展示大容量储能设备的能量、容量与等级，并通过悬停提示补充精确数值。 */
public class LongStorageScreen extends AbstractContainerScreen<LongStorageMenu> {
    public LongStorageScreen(LongStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        MachinePanelStyle.frame(g, x, y, imageWidth, imageHeight, 0xff59bdc7);
        MachinePanelStyle.panel(g, x + 6, y + 23, 164, 44);
        MachinePanelStyle.bar(g, x + 10, y + 43, 156,
                menu.capacity() > 0 ? (double) menu.stored() / menu.capacity() : 0, 0xff59bdc7);
        for (var slot : menu.slots) MachinePanelStyle.slot(g, x, y, slot, 0xff293a50);
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        label(g, title, 9, 0xffe1eaf5);
        label(g, Component.translatable("gui.transcend.long_storage.energy", compact(menu.stored()), compact(menu.capacity())), 30, 0xffb8cce0);
        label(g, Component.translatable("gui.transcend.long_storage.level", menu.level()), 54, 0xff8ed0b8);
        label(g, playerInventoryTitle, 73, 0xff94a9bf);
    }

    private void label(GuiGraphics g, Component text, int y, int color) {
        MachinePanelStyle.label(g, font, text, 8, y, 160, color);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
        if (isHovering(8, 27, 160, 24, mouseX, mouseY)) {
            g.renderTooltip(font, font.split(Component.translatable("gui.transcend.long_storage.energy", menu.stored(), menu.capacity()), 240), mouseX, mouseY);
        }
    }

    private static String compact(long value) {
        if (value >= 1_000_000_000_000_000_000L) return String.format(Locale.ROOT, "%.2fE", value / 1_000_000_000_000_000_000d);
        if (value >= 1_000_000_000_000_000L) return String.format(Locale.ROOT, "%.2fP", value / 1_000_000_000_000_000d);
        if (value >= 1_000_000_000_000L) return String.format(Locale.ROOT, "%.2fT", value / 1_000_000_000_000d);
        if (value >= 1_000_000_000L) return String.format(Locale.ROOT, "%.2fG", value / 1_000_000_000d);
        if (value >= 1_000_000L) return String.format(Locale.ROOT, "%.2fM", value / 1_000_000d);
        if (value >= 1_000L) return String.format(Locale.ROOT, "%.2fk", value / 1_000d);
        return Long.toString(value);
    }
}
