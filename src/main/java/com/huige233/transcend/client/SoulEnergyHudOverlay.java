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

/**
 * R77: 灵魂能（Soul Currency / X）HUD overlay.
 *
 * <p>仅在玩家 stage ≥ 1（getMaxSoulEnergy() &gt; 0）时显示，否则隐藏。
 * 位置：屏幕左上角 InnateManaHudOverlay 之下，硬偏移 y=36（避开魔力条 + 标签 + 誓约行）。
 *
 * <p>布局：
 * <pre>
 *   [================  ]      ← 紫色条
 *   ✧ 47 / 50                  ← 数值（紫色文字）
 * </pre>
 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SoulEnergyHudOverlay {

    private static final int BAR_WIDTH = 80;
    private static final int BAR_HEIGHT = 4;
    private static final int X_OFFSET = 4;
    /** 硬编码 Y 偏移 — 让出 InnateManaHudOverlay 占用的顶部 ~32px 空间。 */
    private static final int Y_OFFSET = 36;
    private static final int COLOR_BORDER = 0xFF301848;
    private static final int COLOR_BG = 0xFF180828;
    /** 紫色填充 — 与法术 mana（蓝）区分开 */
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
        if (max <= 0) return; // stage 0 — 隐藏

        long current = data.getSoulEnergy();

        GuiGraphics gfx = event.getGuiGraphics();
        int x = X_OFFSET;
        int y = Y_OFFSET;

        // 边框 + 背景
        gfx.fill(x, y, x + BAR_WIDTH + 2, y + BAR_HEIGHT + 2, COLOR_BORDER);
        gfx.fill(x + 1, y + 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, COLOR_BG);

        // 填充
        float ratio = (float) current / (float) max;
        if (ratio > 1f) ratio = 1f;
        int fillWidth = Math.max(0, (int) (BAR_WIDTH * ratio));
        if (fillWidth > 0) {
            gfx.fill(x + 1, y + 1, x + 1 + fillWidth, y + BAR_HEIGHT + 1, COLOR_FILL);
        }

        // 数值（"✧ 47 / 50" 紫色）
        String label = "✧ " + current + " / " + max;
        int labelY = y + BAR_HEIGHT + 4;
        gfx.drawString(mc.font, Component.literal(label)
                        .withStyle(ChatFormatting.LIGHT_PURPLE),
                x, labelY, COLOR_TEXT, true);
    }
}
