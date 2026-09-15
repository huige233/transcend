package com.huige233.transcend.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;


/** 为机器界面统一绘制面板边框、槽位背景、限宽文字与进度条。 */
final class MachinePanelStyle {
    private MachinePanelStyle() { }

    static void frame(GuiGraphics g, int x, int y, int width, int height, int accent) {
        g.fill(x + 3, y + 3, x + width + 3, y + height + 3, 0x70000000);
        g.fillGradient(x, y, x + width, y + height, 0xff172333, 0xff090f1a);
        g.renderOutline(x, y, width, height, 0xff42546a);
        g.fill(x + 1, y + 1, x + width - 1, y + 3, accent);
    }

    static void panel(GuiGraphics g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, 0xff0c1522);
        g.renderOutline(x, y, width, height, 0xff293a50);
    }

    static void slot(GuiGraphics g, int x, int y, Slot slot, int accent) {
        int sx = x + slot.x - 1, sy = y + slot.y - 1;
        g.fill(sx, sy, sx + 18, sy + 18, 0xff070d16);
        g.renderOutline(sx, sy, 18, 18, accent);
        g.hLine(sx + 1, sx + 16, sy + 17, 0xff40526a);
    }

    static void label(GuiGraphics g, Font font, Component text, int x, int y, int width, int color) {
        g.drawString(font, net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(text, width)), x, y, color, false);
    }

    static void bar(GuiGraphics g, int x, int y, int width, double fraction, int color) {
        g.fill(x, y, x + width, y + 5, 0xff253246);
        int fill = (int) (width * Math.max(0, Math.min(1, fraction)));
        g.fill(x, y, x + fill, y + 5, color);
        if (fill > 0) g.hLine(x, x + fill - 1, y, 0xffb9d9ed);
    }
}
