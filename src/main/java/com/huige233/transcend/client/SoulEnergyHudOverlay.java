package com.huige233.transcend.client;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.PlayerAscensionData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Transcend.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
/** 灵魂能量 HUD 覆盖层。 */
public class SoulEnergyHudOverlay {

    private static final int BAR_WIDTH = 80;
    private static final int BAR_HEIGHT = 4;
    private static final int X_OFFSET = 4;

    private static final int Y_OFFSET = 36;
    private static final int COLOR_BORDER = 0xFF301848;
    private static final int COLOR_BG = 0xFF180828;

    private static final int COLOR_FILL = 0xFFAA55FF;
    private static final int COLOR_TEXT = 0xFFE5C0FF;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.PLAYER_HEALTH.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (mc.player.isSpectator()) return;

        Player player = mc.player;
        PlayerAscensionData data = AscensionCapability.get(player);
        long max = data.getMaxSoulEnergy();
        if (max <= 0) return;

        long current = data.getSoulEnergy();

        GuiGraphics gfx = event.getGuiGraphics();
        int x = X_OFFSET;
        int y = Y_OFFSET;

        gfx.fill(x, y, x + BAR_WIDTH + 2, y + BAR_HEIGHT + 2, COLOR_BORDER);
        gfx.fill(x + 1, y + 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, COLOR_BG);

        float ratio = (float) current / (float) max;
        if (ratio > 1f) ratio = 1f;
        int fillWidth = Math.max(0, (int) (BAR_WIDTH * ratio));
        if (fillWidth > 0) {
            gfx.fill(x + 1, y + 1, x + 1 + fillWidth, y + BAR_HEIGHT + 1, COLOR_FILL);
        }

        String label = "✧ " + current + " / " + max;
        int labelY = y + BAR_HEIGHT + 4;
        gfx.drawString(mc.font, Component.literal(label)
                        .withStyle(ChatFormatting.LIGHT_PURPLE),
                x, labelY, COLOR_TEXT, true);
    }
}
