package com.huige233.transcend.client;

import com.huige233.transcend.TranscendAttributes;
import com.huige233.transcend.Transcend;
import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.AscensionVow;
import com.huige233.transcend.ascension.PlayerAscensionData;
import com.huige233.transcend.ascension.VowRegistry;
import com.huige233.transcend.client.magic.MagicCrystalHelper;
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
 * HUD overlay: innate mana pool + regen rate + bound-vow indicators.
 *
 * <p>Shown only when MAX_MANA attribute > 0 OR any vow is bound. Hidden in spectator/hideGui.
 *
 * <p>Layout (top-left):
 * <pre>
 *   [================  ]               ← bar
 *   ✦ 670 / 770  +0.66/s  +6.4/s       ← 自体(绿) + 环境吸收(蓝)
 *   1:专注 2:静心 3:玻璃                ← bound vows (only if any)
 * </pre>
 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InnateManaHudOverlay {

    private static final int BAR_WIDTH = 80;
    private static final int BAR_HEIGHT = 4;
    private static final int X_OFFSET = 4;
    private static final int Y_OFFSET = 4;
    private static final int COLOR_BORDER = 0xFF202048;
    private static final int COLOR_BG = 0xFF101028;
    private static final int COLOR_FILL = 0xFF5CC8FF;
    private static final int COLOR_TEXT = 0xFFE0F0FF;
    /** 自体回复速率显示色（亮绿） */
    private static final int COLOR_SELF_REGEN = 0xFF55FF55;
    /** 环境吸收速率显示色（亮蓝） */
    private static final int COLOR_ABSORB = 0xFF5599FF;

    private static final ChatFormatting[] STAGE_COLORS = {
            ChatFormatting.GOLD,        // stage 1
            ChatFormatting.YELLOW,      // stage 2
            ChatFormatting.AQUA,        // stage 3
            ChatFormatting.LIGHT_PURPLE // stage 4
    };

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.PLAYER_HEALTH.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (mc.player.isSpectator()) return;

        Player player = mc.player;
        int maxInnate = MagicCrystalHelper.getInnateMaxMana(player);
        // FIX: 客户端无法直接读 player.getPersistentData() — 必须从 ClientInnateManaCache 取
        // 服务端通过 S2CInnateManaSync 推送，缓存定时刷新
        int currentInnate = ClientInnateManaCache.getCurrentMana();
        float absorbPerSec = ClientInnateManaCache.getAbsorbPerSec();
        PlayerAscensionData data = AscensionCapability.get(player);
        boolean anyVow = data.hasVowForStage(1) || data.hasVowForStage(2)
                || data.hasVowForStage(3) || data.hasVowForStage(4);

        if (maxInnate <= 0 && !anyVow) return; // nothing to show

        double regenPerSec = player.getAttributeValue(TranscendAttributes.MANA_REGEN.get());

        GuiGraphics gfx = event.getGuiGraphics();
        int x = X_OFFSET;
        int y = Y_OFFSET;

        // ── Mana bar (only if MAX_MANA > 0) ────────────────────
        if (maxInnate > 0) {
            // Border + bg
            gfx.fill(x, y, x + BAR_WIDTH + 2, y + BAR_HEIGHT + 2, COLOR_BORDER);
            gfx.fill(x + 1, y + 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, COLOR_BG);
            // Fill
            float ratio = (float) currentInnate / (float) maxInnate;
            if (ratio > 1f) ratio = 1f;
            int fillWidth = Math.max(0, (int) (BAR_WIDTH * ratio));
            if (fillWidth > 0) {
                gfx.fill(x + 1, y + 1, x + 1 + fillWidth, y + BAR_HEIGHT + 1, COLOR_FILL);
            }

            // 主标签：当前/上限（青色）
            String label = "✦ " + currentInnate + " / " + maxInnate;
            int labelY = y + BAR_HEIGHT + 4;
            gfx.drawString(mc.font, Component.literal(label)
                            .withStyle(ChatFormatting.AQUA),
                    x, labelY, COLOR_TEXT, true);
            int cursorX = x + mc.font.width(label) + mc.font.width("  ");

            // 自体回复（绿）和环境吸收（蓝）紧贴显示： +x.xx+yyyy/s
            // 仅在至少一个 > 0 时才绘制
            boolean hasSelf = regenPerSec > 0.005;
            boolean hasAbsorb = absorbPerSec > 0.005f;
            if (hasSelf || hasAbsorb) {
                if (hasSelf) {
                    String selfTxt = String.format("+%.2f", regenPerSec);
                    gfx.drawString(mc.font, selfTxt, cursorX, labelY, COLOR_SELF_REGEN, true);
                    cursorX += mc.font.width(selfTxt);
                }
                if (hasAbsorb) {
                    // 吸收 0~8 范围用 1 位小数足够
                    String absTxt = String.format("+%.1f", absorbPerSec);
                    gfx.drawString(mc.font, absTxt, cursorX, labelY, COLOR_ABSORB, true);
                    cursorX += mc.font.width(absTxt);
                }
                // 共用 "/s" 单位（采用最后一个数字的色，更"贴"的视觉效果）
                int unitColor = hasAbsorb ? COLOR_ABSORB : COLOR_SELF_REGEN;
                gfx.drawString(mc.font, "/s", cursorX, labelY, unitColor, true);
            }
        }

        // ── Bound vows compact display ─────────────────────────
        boolean hasPact = data.hasElementPact();
        if (!anyVow && !hasPact) return;
        int vowY = (maxInnate > 0) ? (y + BAR_HEIGHT + 4 + mc.font.lineHeight + 2) : y;
        if (anyVow) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (int stage = 1; stage <= 4; stage++) {
                String vowId = data.getVowForStage(stage);
                if (vowId == null || vowId.isEmpty()) continue;
                AscensionVow vow = VowRegistry.get(vowId);
                if (vow == null) continue;
                if (!first) sb.append(" ");
                first = false;
                sb.append(STAGE_COLORS[stage - 1]).append(stage).append(":")
                        .append(Component.translatable(vow.getTranslationKey()).getString())
                        .append(ChatFormatting.RESET);
            }
            if (sb.length() > 0) {
                Component vowLine = Component.literal(sb.toString());
                gfx.drawString(mc.font, vowLine, x, vowY, 0xFFFFFFFF, true);
                vowY += mc.font.lineHeight + 1;
            }
        }

        // R79: 元素灵契显示（紫色，独立一行）
        if (hasPact) {
            com.huige233.transcend.spell.SpellElement pactEl = data.getElementPact();
            if (pactEl != null) {
                Component pactLine = Component.translatable(
                                "hud.transcend.element_pact",
                                Component.literal(pactEl.id).withStyle(ChatFormatting.LIGHT_PURPLE))
                        .withStyle(ChatFormatting.LIGHT_PURPLE);
                gfx.drawString(mc.font, pactLine, x, vowY, 0xFFFFFFFF, true);
            }
        }
    }
}
