package com.huige233.transcend.client;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.ascension.resource.ClassResourceType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Transcend.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClassResourceHudOverlay {

    private static final int BAR_WIDTH = 72;
    private static final int BAR_HEIGHT = 4;
    private static final int X_OFFSET = 4;

    private static final int Y_BELOW_MANA = 38;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.PLAYER_HEALTH.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (mc.player.isSpectator()) return;

        ClassResourceType type = ClientClassResourceCache.getType();
        if (type == null) return;

        float ratio = ClientClassResourceCache.getRatio();
        float value = ClientClassResourceCache.getValue();
        float max = ClientClassResourceCache.getMaxValue();
        boolean inWindow = ClientClassResourceCache.isInWindow();
        boolean overflowed = ClientClassResourceCache.isOverflowed();

        GuiGraphics gfx = event.getGuiGraphics();
        int x = X_OFFSET;
        int y = Y_BELOW_MANA;

        int fillColor = type.getBarColor();
        int borderColor = 0xFF202048;
        int bgColor = 0xFF101028;

        long tick = mc.player.tickCount;
        if (overflowed) {

            if ((tick / 5) % 2 == 0) {
                fillColor = blendColor(fillColor, 0xFFFF2222, 0.5f);
            }
        } else if (inWindow) {

            if ((tick / 4) % 2 == 0) {
                fillColor = blendColor(fillColor, 0xFFFFFFFF, 0.3f);
            }
        }

        gfx.fill(x, y, x + BAR_WIDTH + 2, y + BAR_HEIGHT + 2, borderColor);
        gfx.fill(x + 1, y + 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, bgColor);

        int fillWidth = Math.max(0, (int) (BAR_WIDTH * ratio));
        if (fillWidth > 0) {
            gfx.fill(x + 1, y + 1, x + 1 + fillWidth, y + BAR_HEIGHT + 1, fillColor);
        }

        float threshold = type.getThresholdRatio();
        if (threshold > 0f && threshold < 1f) {
            int threshX = x + 1 + (int) (BAR_WIDTH * threshold);
            gfx.fill(threshX, y, threshX + 1, y + BAR_HEIGHT + 2, 0xCCFFFFFF);
        }

        String icon = type.getIcon();
        String label = icon + " " + type.getDisplayId() + " " + (int) value + "/" + (int) max;
        int textColor = overflowed ? 0xFFFF4444 : (inWindow ? 0xFFFFFF88 : 0xFFE0E0E0);
        gfx.drawString(mc.font, Component.literal(label), x, y + BAR_HEIGHT + 4, textColor, true);
    }

    private static int blendColor(int c1, int c2, float t) {
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
