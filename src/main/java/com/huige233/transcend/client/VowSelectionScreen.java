package com.huige233.transcend.client;

import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.AscensionVow;
import com.huige233.transcend.ascension.PlayerAscensionData;
import com.huige233.transcend.ascension.VowRegistry;
import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.network.C2SAscensionAction;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 飞升誓约选择界面 — 由 AscensionTreeScreen 的「誓约」按钮打开。
 *
 * <p>布局：4 个阶段段，每段一行 3 个誓约按钮 + 当前绑定的清除按钮。
 * 玩家飞升阶段 &lt; 段位时整段灰显（按钮 disable）。
 */
public class VowSelectionScreen extends Screen {

    private static final int W = 384;
    private static final int H = 308;
    private static final int TITLE_H = 22;
    private static final int FOOTER_H = 26;
    private static final int STAGE_H = 60;

    private static final int C_BG       = 0xE0080810;
    private static final int C_BORDER   = 0xFF2A2A5A;
    private static final int C_TITLE    = 0xFFFFCC00;
    private static final int C_STAGE_OK = 0xFFE0F0FF;
    private static final int C_LOCKED   = 0xFF666677;
    private static final int C_BOUND    = 0xFF55FFAA;

    private PlayerAscensionData data;

    public VowSelectionScreen() {
        super(Component.translatable("screen.transcend.vow_selection"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new VowSelectionScreen());
    }

    @Override
    protected void init() {
        if (minecraft != null && minecraft.player != null) {
            data = AscensionCapability.get(minecraft.player);
        }
        if (data == null) data = new PlayerAscensionData();

        int ox = (width - W) / 2;
        int oy = (height - H) / 2;

        // ── 4 stages ──
        for (int stage = 1; stage <= 4; stage++) {
            buildStageRow(ox, oy + TITLE_H + (stage - 1) * STAGE_H, stage);
        }

        // ── Close button ──
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.transcend.vow.close"),
                        btn -> onClose())
                .bounds(ox + (W - 100) / 2, oy + H - FOOTER_H + 4, 100, 18)
                .build());
    }

    private void buildStageRow(int ox, int y, int stage) {
        boolean unlocked = data.getStage() >= stage;
        String boundVowId = data.getVowForStage(stage);
        boolean hasBound = boundVowId != null && !boundVowId.isEmpty();

        List<AscensionVow> vows = VowRegistry.getVowsForStage(stage);

        int btnW = 110;
        int btnH = 20;
        int gap = 6;
        int rowY = y + 24;  // leave space for stage header text
        int totalRowWidth = vows.size() * btnW + (vows.size() - 1) * gap;
        int startX = ox + (W - totalRowWidth) / 2;

        for (int i = 0; i < vows.size(); i++) {
            AscensionVow vow = vows.get(i);
            boolean isBound = vow.getId().equals(boundVowId);
            int x = startX + i * (btnW + gap);

            Component label;
            if (isBound) {
                label = Component.literal("✓ ").withStyle(ChatFormatting.GREEN)
                        .append(Component.translatable(vow.getTranslationKey())
                                .withStyle(ChatFormatting.AQUA));
            } else {
                label = Component.translatable(vow.getTranslationKey())
                        .withStyle(unlocked ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY);
            }

            Button b = Button.builder(label, btn -> {
                        if (!unlocked) return;
                        if (isBound) {
                            // Clicking the bound vow clears it
                            NetworkHandler.CHANNEL.sendToServer(
                                    new C2SAscensionAction(6, String.valueOf(stage)));
                        } else {
                            NetworkHandler.CHANNEL.sendToServer(
                                    new C2SAscensionAction(5, stage + "|" + vow.getId()));
                        }
                        // Re-init to refresh button states (client predicts; server may overrule)
                        rebuild();
                    })
                    .bounds(x, rowY, btnW, btnH)
                    .tooltip(Tooltip.create(buildVowTooltip(vow, unlocked, isBound)))
                    .build();
            b.active = unlocked;
            addRenderableWidget(b);
        }
    }

    private Component buildVowTooltip(AscensionVow vow, boolean unlocked, boolean isBound) {
        Component nameLine = Component.translatable(vow.getTranslationKey()).withStyle(ChatFormatting.GOLD);
        Component benefitLine = Component.literal("\n+ ")
                .append(Component.translatable(vow.getBenefitKey()).withStyle(ChatFormatting.GREEN));
        Component costLine = Component.literal("\n- ")
                .append(Component.translatable(vow.getCostKey()).withStyle(ChatFormatting.RED));
        Component statusLine;
        if (!unlocked) {
            statusLine = Component.literal("\n\n")
                    .append(Component.translatable("gui.transcend.vow.locked", vow.getStage())
                            .withStyle(ChatFormatting.DARK_GRAY));
        } else if (isBound) {
            statusLine = Component.literal("\n\n")
                    .append(Component.translatable("gui.transcend.vow.click_to_unbind")
                            .withStyle(ChatFormatting.YELLOW));
        } else {
            statusLine = Component.literal("\n\n")
                    .append(Component.translatable("gui.transcend.vow.click_to_bind")
                            .withStyle(ChatFormatting.AQUA));
        }
        return nameLine.copy().append(benefitLine).append(costLine).append(statusLine);
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g);

        int ox = (width - W) / 2;
        int oy = (height - H) / 2;

        // Panel
        g.fill(ox, oy, ox + W, oy + H, C_BG);
        // Border (1px)
        g.fill(ox, oy, ox + W, oy + 1, C_BORDER);
        g.fill(ox, oy + H - 1, ox + W, oy + H, C_BORDER);
        g.fill(ox, oy, ox + 1, oy + H, C_BORDER);
        g.fill(ox + W - 1, oy, ox + W, oy + H, C_BORDER);

        // Title
        Component title = Component.translatable("screen.transcend.vow_selection")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        int titleW = this.font.width(title);
        g.drawString(this.font, title, ox + (W - titleW) / 2, oy + 6, C_TITLE, false);

        // Current stage indicator
        Component stageInfo = Component.translatable(
                "gui.transcend.vow.current_stage", data.getStage());
        int siW = this.font.width(stageInfo);
        g.drawString(this.font, stageInfo, ox + W - siW - 6, oy + 8, 0xFFA0A0FF, false);

        // Stage labels for each row
        for (int stage = 1; stage <= 4; stage++) {
            int rowY = oy + TITLE_H + (stage - 1) * STAGE_H;
            boolean unlocked = data.getStage() >= stage;
            String stageName = stageNameKey(stage);
            Component label = Component.literal("阶段 " + stage + " — ")
                    .append(Component.translatable(stageName))
                    .withStyle(unlocked ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY);

            String boundVowId = data.getVowForStage(stage);
            if (boundVowId != null && !boundVowId.isEmpty()) {
                AscensionVow v = VowRegistry.get(boundVowId);
                if (v != null) {
                    label = label.copy().append(Component.literal("  →  ").withStyle(ChatFormatting.GRAY))
                            .append(Component.translatable(v.getTranslationKey()).withStyle(ChatFormatting.GREEN));
                }
            }

            g.drawString(this.font, label, ox + 12, rowY + 4,
                    unlocked ? C_STAGE_OK : C_LOCKED, false);
        }

        super.render(g, mouseX, mouseY, partial);
    }

    private static String stageNameKey(int stage) {
        return switch (stage) {
            case 1 -> "gui.transcend.vow.stage1";
            case 2 -> "gui.transcend.vow.stage2";
            case 3 -> "gui.transcend.vow.stage3";
            case 4 -> "gui.transcend.vow.stage4";
            default -> "gui.transcend.vow.stage_unknown";
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
