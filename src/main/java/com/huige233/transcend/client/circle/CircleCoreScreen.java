package com.huige233.transcend.client.circle;

import com.huige233.transcend.block.circle.CircleCoreMenu;
import com.huige233.transcend.block.circle.CircleCoreMenu.CircleCoreData;
import com.huige233.transcend.block.circle.MagicCircleCoreBlockEntity;
import com.huige233.transcend.circle.CircleFunctionSettings;
import com.huige233.transcend.circle.CircleFunctionSettings.SettingDef;
import com.huige233.transcend.circle.CircleFunctionType;
import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.network.C2SCircleAction;
import com.huige233.transcend.network.C2SCircleSettingChange;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 法环核心 GUI 屏幕（v3 — 修复布局错位）。
 *
 * <p><b>v2 的关键缺陷</b>：所有面板（y=26..160）覆盖了菜单实际 slot 坐标
 * (input@26,47 / output@134,47) 和玩家背包 (y=94..170)，导致用户看见
 * 「面板压在物品上、底部还有一行假槽位」的混乱。
 *
 * <p><b>v3 设计原则</b>：尊重菜单坐标，所有自绘元素必须避开:
 * <ul>
 *   <li>输入/输出槽：(26,47), (134,47) — 18×18</li>
 *   <li>玩家背包 9×3：x=8..160, y=94..148</li>
 *   <li>玩家快捷栏 9：x=8..160, y=152..170</li>
 * </ul>
 *
 * <p>布局（256×220）:
 * <pre>
 *  y=0..20   Header: 标题 + Tier 徽章
 *  y=22..23  Tier accent 横线
 *  y=24..88  顶部信息区
 *            ┌── 左面板 (4..78, 24..88) ──┐  [输入] [符印] [输出]   ┌── 右面板 (156..252) ──┐
 *            │ 法环储能 / 状态 / 结构      │   y=47   y=47   y=47   │ 功能 / 催化            │
 *            └─────────────────────────────┘                        └────────────────────────┘
 *  y=92      上下分割线
 *  y=94..170 玩家背包 + 快捷栏 (菜单原坐标，不动)
 *  y=174..196 设置条（紧凑横向，可滚）
 *  y=200..216 按钮行（启动 / 预览）
 * </pre>
 */
public class CircleCoreScreen extends AbstractContainerScreen<CircleCoreMenu> {

    // ============================================================
    // 几何常量 — 对齐菜单 slot 坐标
    // ============================================================
    private static final int IMAGE_WIDTH  = 256;
    private static final int IMAGE_HEIGHT = 220;

    private static final int HEADER_TOP   = 4;
    private static final int HEADER_H     = 14;
    private static final int ACCENT_Y     = 21;

    private static final int TOP_PANEL_TOP = 24;
    private static final int TOP_PANEL_BOT = 88;

    // 槽位坐标（菜单写死 — 不可改）
    private static final int SLOT_Y       = 47;
    private static final int IN_SLOT_X    = 26;
    private static final int SIGIL_X      = 80;   // 虚拟槽位（仅展示）
    private static final int OUT_SLOT_X   = 134;
    private static final int SLOT_LABEL_Y = SLOT_Y - 12;

    // 左信息面板
    private static final int LP_X = 4;
    private static final int LP_W = 76;

    // 右信息面板
    private static final int RP_X = 156;
    private static final int RP_W = 96;

    // 玩家背包（菜单写死: y=94..170）
    private static final int INV_TOP = 92;
    private static final int INV_BOT = 172;

    // 设置条
    private static final int SETTINGS_TOP = 174;
    private static final int SETTINGS_BOT = 196;
    private static final int SETTINGS_VISIBLE = 4;
    private static final int SETTINGS_ITEM_W  = 60;

    // 按钮行
    private static final int BUTTON_TOP = 200;
    private static final int BUTTON_H   = 14;

    // ============================================================
    // 调色板
    // ============================================================
    private static final int COLOR_BG       = 0xFF120E1A;
    private static final int COLOR_PANEL    = 0xFF231B30;
    private static final int COLOR_PANEL_HI = 0xFF332647;
    private static final int COLOR_HEADER   = 0xFF0B0710;
    private static final int COLOR_INV_BG   = 0xFF1A1426;
    private static final int COLOR_TEXT     = 0xFFE0DAFF;
    private static final int COLOR_LABEL    = 0xFF8E8796;
    private static final int COLOR_INACT    = 0xFF555065;
    private static final int COLOR_OK       = 0xFF81C784;
    private static final int COLOR_WARN     = 0xFFFFB74D;
    private static final int COLOR_ERR      = 0xFFE57373;
    private static final int COLOR_HOVER    = 0x40FFFFFF;
    private static final int COLOR_SLOT_BG  = 0xFF000000;

    private static final int[] TIER_COLOR = {
            0xFF8A8A8A, // T1 stone gray
            0xFF4DB6AC, // T2 teal
            0xFF7E57C2, // T3 arcane violet
            0xFF42A5F5, // T4 ley blue
            0xFFFFD54F  // T5 prismatic gold
    };

    private Button toggleButton;
    private Button ghostPreviewButton;

    /** 客户端设置滚动偏移（横向，按 SETTINGS_VISIBLE 个为一页） */
    private int settingsScrollOffset = 0;

    public CircleCoreScreen(CircleCoreMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = IMAGE_WIDTH;
        this.imageHeight = IMAGE_HEIGHT;
        // 玩家背包标签在第一行 inv 上方
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 84;
        // 隐藏 vanilla 默认 title 绘制（我们自己画）
        this.titleLabelX = -1000;
        this.titleLabelY = -1000;
    }

    @Override
    protected void init() {
        super.init();

        int btnW = 56;
        int btnX = this.leftPos + LP_X;
        int btnY = this.topPos + BUTTON_TOP;
        this.toggleButton = Button.builder(getToggleButtonLabel(), btn -> onToggleClicked())
                .pos(btnX, btnY).size(btnW, BUTTON_H)
                .build();
        this.addRenderableWidget(this.toggleButton);

        int ghostX = this.leftPos + LP_X + btnW + 6;
        this.ghostPreviewButton = Button.builder(
                        Component.translatable("gui.transcend.circle_core.ghost_preview"),
                        btn -> onGhostPreviewClicked())
                .pos(ghostX, btnY).size(72, BUTTON_H)
                .build();
        this.addRenderableWidget(this.ghostPreviewButton);
    }

    private Component getToggleButtonLabel() {
        boolean active = this.menu.getData() != null && this.menu.getData().active;
        return active
                ? Component.translatable("gui.transcend.circle_core.deactivate")
                : Component.translatable("gui.transcend.circle_core.activate");
    }

    private void onToggleClicked() {
        CircleCoreData data = this.menu.getData();
        if (data == null) return;
        C2SCircleAction.ActionType act = data.active
                ? C2SCircleAction.ActionType.DEACTIVATE
                : C2SCircleAction.ActionType.ACTIVATE;
        NetworkHandler.CHANNEL.sendToServer(
                new C2SCircleAction(this.menu.getCorePos(), act));
    }

    private void onGhostPreviewClicked() {
        CircleCoreData data = this.menu.getData();
        if (data == null) return;
        int tier = Math.max(1, data.tier == 0 ? 1 : data.tier);
        NetworkHandler.CHANNEL.sendToServer(
                new C2SCircleAction(this.menu.getCorePos(),
                        C2SCircleAction.ActionType.PREVIEW_GHOST, tier));
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTick);

        // 每帧根据当前状态刷新按钮文本
        if (this.toggleButton != null) {
            this.toggleButton.setMessage(getToggleButtonLabel());
        }

        renderTooltip(gfx, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int x0 = this.leftPos;
        int y0 = this.topPos;
        CircleCoreData data = this.menu.getData();
        if (data == null) data = CircleCoreData.empty();

        int tierAccent = getTierAccent(data.tier);

        // === 整体背景 ===
        gfx.fill(x0, y0, x0 + IMAGE_WIDTH, y0 + IMAGE_HEIGHT, COLOR_BG);

        // === Header (y=2..18) ===
        gfx.fill(x0 + 2, y0 + HEADER_TOP - 2, x0 + IMAGE_WIDTH - 2,
                y0 + HEADER_TOP + HEADER_H, COLOR_HEADER);

        // Tier accent line
        gfx.fill(x0 + 2, y0 + ACCENT_Y, x0 + IMAGE_WIDTH - 2, y0 + ACCENT_Y + 1, tierAccent);

        // === 顶部 - 左面板（状态/结构）===
        drawPanel(gfx, x0 + LP_X, y0 + TOP_PANEL_TOP, LP_W,
                TOP_PANEL_BOT - TOP_PANEL_TOP, tierAccent);

        // === 顶部 - 右面板（功能/催化）===
        drawPanel(gfx, x0 + RP_X, y0 + TOP_PANEL_TOP, RP_W,
                TOP_PANEL_BOT - TOP_PANEL_TOP, tierAccent);

        // === 槽位 3 件套底色（输入/符印/输出） ===
        drawSlotFrame(gfx, x0 + IN_SLOT_X - 1, y0 + SLOT_Y - 1, tierAccent);
        drawSlotFrame(gfx, x0 + SIGIL_X    - 1, y0 + SLOT_Y - 1, tierAccent);
        drawSlotFrame(gfx, x0 + OUT_SLOT_X - 1, y0 + SLOT_Y - 1, tierAccent);

        // === 上下分割线 (y=92) ===
        gfx.fill(x0 + 4, y0 + 92, x0 + IMAGE_WIDTH - 4, y0 + 93, tierAccent & 0x80FFFFFF);

        // === 玩家背包底色 (y=94..170) ===
        gfx.fill(x0 + 4, y0 + INV_TOP, x0 + IMAGE_WIDTH - 4, y0 + INV_BOT, COLOR_INV_BG);

        // === 设置条底色 (y=174..196) ===
        gfx.fill(x0 + 2, y0 + SETTINGS_TOP, x0 + IMAGE_WIDTH - 2,
                y0 + SETTINGS_BOT, COLOR_PANEL);
        gfx.fill(x0 + 2, y0 + SETTINGS_TOP, x0 + IMAGE_WIDTH - 2,
                y0 + SETTINGS_TOP + 1, tierAccent);

        // === 按钮行底色 ===
        gfx.fill(x0 + 2, y0 + BUTTON_TOP - 2, x0 + IMAGE_WIDTH - 2,
                y0 + BUTTON_TOP + BUTTON_H + 2, COLOR_HEADER);
    }

    /** 绘制单个面板：底色 + 1px Tier 配色边框。 */
    private static void drawPanel(GuiGraphics gfx, int x, int y, int w, int h, int border) {
        gfx.fill(x, y, x + w, y + h, COLOR_PANEL);
        gfx.fill(x, y,             x + w, y + 1,     border);
        gfx.fill(x, y + h - 1,     x + w, y + h,     border);
        gfx.fill(x, y,             x + 1, y + h,     border);
        gfx.fill(x + w - 1, y,     x + w, y + h,     border);
    }

    /** 槽位 18x18 + 1px 描边底色。 */
    private static void drawSlotFrame(GuiGraphics gfx, int x, int y, int border) {
        gfx.fill(x, y, x + 20, y + 20, border);
        gfx.fill(x + 1, y + 1, x + 19, y + 19, COLOR_SLOT_BG);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        CircleCoreData data = this.menu.getData();
        if (data == null) data = CircleCoreData.empty();

        // ── Header 标题 ──
        Component title = Component.translatable("container.transcend.circle_core")
                .withStyle(ChatFormatting.WHITE);
        int tw = this.font.width(title);
        gfx.drawString(this.font, title, (IMAGE_WIDTH - tw) / 2, HEADER_TOP, COLOR_TEXT, false);

        // ── Tier 徽章（右上） ──
        Component tierBadge = getTierBadge(data);
        int tbW = this.font.width(tierBadge);
        int badgeX = IMAGE_WIDTH - tbW - 8;
        gfx.fill(badgeX - 4, HEADER_TOP - 2, badgeX + tbW + 4, HEADER_TOP + 9,
                getTierAccent(data.tier) & 0x80FFFFFF);
        gfx.drawString(this.font, tierBadge, badgeX, HEADER_TOP, 0xFFFFFFFF, false);

        // ── 法环储能横向条（header 下方）──
        if (data.maxMana > 0) {
            String manaText = data.storedMana + " / " + data.maxMana + " CM";
            int mtW = this.font.width(manaText);
            int barW = 80;
            int barX = (IMAGE_WIDTH - barW) / 2;
            int barY = ACCENT_Y - 8;
            // bar bg
            gfx.fill(barX - 1, barY, barX + barW + 1, barY + 4, 0xFF000000);
            gfx.fill(barX, barY + 1, barX + barW, barY + 3, 0xFF1A1A28);
            float ratio = Math.min(1f, Math.max(0f, (float) data.storedMana / (float) data.maxMana));
            int fillW = Math.round(barW * ratio);
            if (fillW > 0) {
                gfx.fill(barX, barY + 1, barX + fillW, barY + 3, getTierAccent(data.tier));
            }
            gfx.drawString(this.font, manaText, (IMAGE_WIDTH - mtW) / 2, barY - 9, COLOR_LABEL, false);
        }

        // ── 槽位上方标签 ──
        gfx.drawCenteredString(this.font,
                Component.translatable("gui.transcend.circle_core.input"),
                IN_SLOT_X + 9, SLOT_LABEL_Y, COLOR_LABEL);
        gfx.drawCenteredString(this.font,
                Component.translatable("gui.transcend.circle_core.sigil_slot"),
                SIGIL_X + 9, SLOT_LABEL_Y, COLOR_LABEL);
        gfx.drawCenteredString(this.font,
                Component.translatable("gui.transcend.circle_core.output"),
                OUT_SLOT_X + 9, SLOT_LABEL_Y, COLOR_LABEL);

        // ── 左面板：状态 / 结构 ──
        renderLeftPanel(gfx, data);

        // ── 右面板：功能 / 催化 ──
        renderRightPanel(gfx, data);

        // ── 槽位下方：维持成本 ──
        if (data.upkeepPerMin > 0) {
            String upkeep = String.format("§c-%.1f §7/min", data.upkeepPerMin);
            int uw = this.font.width(upkeep);
            gfx.drawString(this.font, upkeep,
                    (IMAGE_WIDTH - uw) / 2, SLOT_Y + 22, COLOR_WARN, false);
        }

        // ── 符印展示物品（虚拟，菜单中无对应 Slot）──
        ItemStack installedSigil = readClientSigil();
        if (!installedSigil.isEmpty()) {
            gfx.renderItem(installedSigil, SIGIL_X, SLOT_Y);
        }

        // ── 设置条 ──
        renderSettingsBar(gfx, mouseX, mouseY);

        // ── 玩家背包标题 ──
        gfx.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, COLOR_LABEL, false);
    }

    // ============================================================
    // 左 / 右面板
    // ============================================================

    private void renderLeftPanel(GuiGraphics gfx, CircleCoreData data) {
        int x = LP_X + 4;
        int y = TOP_PANEL_TOP + 4;

        // 状态
        gfx.drawString(this.font,
                Component.translatable("gui.transcend.circle_core.state_label")
                        .withStyle(ChatFormatting.GRAY),
                x, y, COLOR_LABEL, false);
        y += 10;
        Component state = getStateComponent(data);
        gfx.drawString(this.font, state, x, y, getStateColor(data), false);
        y += 14;

        // 结构
        gfx.drawString(this.font,
                Component.translatable("gui.transcend.circle_core.structure_label")
                        .withStyle(ChatFormatting.GRAY),
                x, y, COLOR_LABEL, false);
        y += 10;
        Component structure;
        int sc;
        if (data.structureValid) {
            structure = Component.translatable("gui.transcend.circle_core.structure_ok");
            sc = COLOR_OK;
        } else if (data.missingBlockCount > 0) {
            structure = Component.translatable("gui.transcend.circle_core.structure_missing",
                    data.missingBlockCount);
            sc = COLOR_ERR;
        } else {
            structure = Component.translatable("gui.transcend.circle_core.structure_invalid");
            sc = COLOR_ERR;
        }
        gfx.drawString(this.font, structure, x, y, sc, false);
        y += 12;

        // 符印锁定状态（迷你）
        String sigilLabel;
        int sigilColor;
        if (data.functionId == null || data.functionId.isEmpty()) {
            sigilLabel = "§8" + Component.translatable("gui.transcend.circle_core.sigil_none").getString();
            sigilColor = COLOR_INACT;
        } else if (data.sigilLocked) {
            sigilLabel = "§a" + Component.translatable("gui.transcend.circle_core.sigil_locked").getString();
            sigilColor = COLOR_OK;
        } else {
            sigilLabel = "§e" + Component.translatable("gui.transcend.circle_core.sigil_unlocked").getString();
            sigilColor = COLOR_WARN;
        }
        gfx.drawString(this.font, sigilLabel, x, y, sigilColor, false);
    }

    private void renderRightPanel(GuiGraphics gfx, CircleCoreData data) {
        int x = RP_X + 4;
        int y = TOP_PANEL_TOP + 4;

        // 功能
        gfx.drawString(this.font,
                Component.translatable("gui.transcend.circle_core.function_label")
                        .withStyle(ChatFormatting.GRAY),
                x, y, COLOR_LABEL, false);
        y += 10;
        Component funcName = getFunctionComponent(data);
        gfx.drawString(this.font, funcName, x, y, 0xFFE6D9FF, false);
        y += 14;

        // 催化
        gfx.drawString(this.font,
                Component.translatable("gui.transcend.circle_core.catalyst_label")
                        .withStyle(ChatFormatting.GRAY),
                x, y, COLOR_LABEL, false);
        y += 10;
        String catText = data.catalystSatisfiedCount + " / " + data.catalystCount;
        int catColor = (data.catalystCount > 0 && data.catalystSatisfiedCount == data.catalystCount)
                ? COLOR_OK : COLOR_WARN;
        if (data.catalystCount == 0) catColor = COLOR_INACT;
        gfx.drawString(this.font, catText, x, y, catColor, false);
        y += 12;

        // 催化剂列表（最多 2 项）
        List<ItemStack> cats = readClientCatalysts();
        if (!cats.isEmpty()) {
            int show = Math.min(2, cats.size());
            for (int i = 0; i < show; i++) {
                ItemStack s = cats.get(i);
                String name = s.getHoverName().getString();
                if (name.length() > 14) name = name.substring(0, 13) + "…";
                gfx.drawString(this.font, name + " §a✓", x, y, COLOR_TEXT, false);
                y += 9;
            }
            if (cats.size() > 2) {
                gfx.drawString(this.font, "§7+" + (cats.size() - 2), x, y, COLOR_LABEL, false);
            }
        } else if (data.catalystCount > 0) {
            gfx.drawString(this.font,
                    Component.translatable("gui.transcend.circle_core.catalyst_none").getString(),
                    x, y, COLOR_INACT, false);
        }
    }

    // ============================================================
    // 设置条 — 横向紧凑可滚
    // ============================================================

    private void renderSettingsBar(GuiGraphics gfx, int mouseX, int mouseY) {
        int barTop = SETTINGS_TOP;
        int barInner = barTop + 2;

        // 标签
        gfx.drawString(this.font, "§7" +
                        Component.translatable("gui.transcend.circle_core.settings_label").getString(),
                4, barInner, COLOR_LABEL, false);

        List<SettingDef> settings = getCurrentSettings();
        if (settings.isEmpty()) {
            gfx.drawString(this.font,
                    Component.translatable("gui.transcend.circle_core.settings_none").getString(),
                    4, barInner + 10, COLOR_INACT, false);
            return;
        }

        int total = settings.size();
        int maxOffset = Math.max(0, total - SETTINGS_VISIBLE);
        if (settingsScrollOffset > maxOffset) settingsScrollOffset = maxOffset;
        if (settingsScrollOffset < 0) settingsScrollOffset = 0;

        // 滚动指示
        if (total > SETTINGS_VISIBLE) {
            String pageInfo = (settingsScrollOffset + 1) + "-"
                    + Math.min(total, settingsScrollOffset + SETTINGS_VISIBLE)
                    + "/" + total;
            int piW = this.font.width(pageInfo);
            gfx.drawString(this.font, pageInfo,
                    IMAGE_WIDTH - piW - 4, barInner, COLOR_LABEL, false);
        }

        int relMouseX = mouseX - this.leftPos;
        int relMouseY = mouseY - this.topPos;

        int rowY = barInner + 10;
        int startX = 4;
        for (int i = 0; i < SETTINGS_VISIBLE; i++) {
            int idx = settingsScrollOffset + i;
            if (idx >= total) break;
            SettingDef def = settings.get(idx);
            int x = startX + i * SETTINGS_ITEM_W;
            int xEnd = x + SETTINGS_ITEM_W - 4;

            // hover 背景
            if (relMouseX >= x && relMouseX < xEnd
                    && relMouseY >= rowY - 1 && relMouseY < rowY + 9) {
                gfx.fill(x, rowY - 1, xEnd, rowY + 9, COLOR_HOVER);
            }

            // 标签（截断到 5 字内）
            String label = Component.translatable(def.translationKey()).getString();
            if (label.length() > 5) label = label.substring(0, 4) + "…";
            gfx.drawString(this.font, label, x + 1, rowY, COLOR_TEXT, false);

            // 值（右对齐于该格）
            int v = getCurrentSettingValue(def);
            String vt = formatSettingValue(def, v);
            int vW = this.font.width(vt);
            gfx.drawString(this.font, vt, xEnd - vW, rowY, 0xFFFFFFFF, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 设置条点击
        if (button == 0) {
            int relMouseX = (int) (mouseX - this.leftPos);
            int relMouseY = (int) (mouseY - this.topPos);
            int rowY = SETTINGS_TOP + 12;

            List<SettingDef> settings = getCurrentSettings();
            if (!settings.isEmpty()
                    && relMouseY >= rowY - 1 && relMouseY < rowY + 9) {
                int startX = 4;
                for (int i = 0; i < SETTINGS_VISIBLE; i++) {
                    int idx = settingsScrollOffset + i;
                    if (idx >= settings.size()) break;
                    SettingDef def = settings.get(idx);
                    int x = startX + i * SETTINGS_ITEM_W;
                    int xEnd = x + SETTINGS_ITEM_W - 4;
                    if (relMouseX >= x && relMouseX < xEnd) {
                        onSettingClicked(def, mouseX);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int relMouseX = (int) (mouseX - this.leftPos);
        int relMouseY = (int) (mouseY - this.topPos);
        if (relMouseY >= SETTINGS_TOP && relMouseY < SETTINGS_BOT) {
            List<SettingDef> settings = getCurrentSettings();
            int total = settings.size();
            if (total > SETTINGS_VISIBLE) {
                int maxOffset = total - SETTINGS_VISIBLE;
                if (delta > 0) settingsScrollOffset = Math.max(0, settingsScrollOffset - 1);
                else settingsScrollOffset = Math.min(maxOffset, settingsScrollOffset + 1);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void onSettingClicked(SettingDef def, double mouseX) {
        int current = getCurrentSettingValue(def);
        int next;
        switch (def.type()) {
            case TOGGLE, ENUM_CYCLE -> next = def.cycleNext(current);
            case SLIDER -> {
                // 在该格内点击左/右半边 ± 1
                next = def.clamp(current + 1);
            }
            default -> next = current;
        }
        if (next != current) {
            NetworkHandler.CHANNEL.sendToServer(
                    new C2SCircleSettingChange(this.menu.getCorePos(), def.id(), next));
        }
    }

    // ============================================================
    // 客户端读取辅助
    // ============================================================

    private CircleFunctionType getCurrentFunctionType() {
        CircleCoreData data = this.menu.getData();
        if (data == null || data.functionId == null || data.functionId.isEmpty()) return null;
        try {
            return CircleFunctionType.valueOf(data.functionId.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private List<SettingDef> getCurrentSettings() {
        CircleFunctionType type = getCurrentFunctionType();
        if (type == null) return Collections.emptyList();
        return CircleFunctionSettings.getSettingsFor(type);
    }

    private int getCurrentSettingValue(SettingDef def) {
        Map<String, Integer> values = readClientSettings();
        return CircleFunctionSettings.getValue(values, def);
    }

    private Map<String, Integer> readClientSettings() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return Collections.emptyMap();
        BlockEntity be = mc.level.getBlockEntity(this.menu.getCorePos());
        if (be instanceof MagicCircleCoreBlockEntity core) {
            return core.getFunctionSettings();
        }
        return Collections.emptyMap();
    }

    private List<ItemStack> readClientCatalysts() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return Collections.emptyList();
        BlockEntity be = mc.level.getBlockEntity(this.menu.getCorePos());
        if (be instanceof MagicCircleCoreBlockEntity core) {
            return core.getCachedCatalysts();
        }
        return Collections.emptyList();
    }

    private ItemStack readClientSigil() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return ItemStack.EMPTY;
        BlockEntity be = mc.level.getBlockEntity(this.menu.getCorePos());
        if (be instanceof MagicCircleCoreBlockEntity core) {
            return core.getFunctionSigil();
        }
        return ItemStack.EMPTY;
    }

    private String formatSettingValue(SettingDef def, int value) {
        return switch (def.type()) {
            case TOGGLE -> value == 1 ? "§aON" : "§cOFF";
            case SLIDER -> String.valueOf(def.clamp(value));
            case ENUM_CYCLE -> {
                List<String> values = def.enumValues();
                if (values.isEmpty()) yield String.valueOf(value);
                int idx = Math.max(0, Math.min(value, values.size() - 1));
                String s = values.get(idx);
                if (s.length() > 4) s = s.substring(0, 4);
                yield s;
            }
        };
    }

    // ============================================================
    // 颜色 / 文本辅助
    // ============================================================

    private static int getTierAccent(int tier) {
        if (tier >= 1 && tier <= 5) return TIER_COLOR[tier - 1];
        return 0xFF4A3F5C;
    }

    private Component getTierBadge(CircleCoreData data) {
        if (data.tier <= 0 || !data.structureValid) {
            return Component.literal("--").withStyle(ChatFormatting.DARK_GRAY);
        }
        return Component.literal("T" + data.tier).withStyle(ChatFormatting.WHITE);
    }

    private Component getFunctionComponent(CircleCoreData data) {
        if (data.functionId == null || data.functionId.isEmpty()) {
            return Component.translatable("gui.transcend.circle_core.no_function")
                    .withStyle(ChatFormatting.GRAY);
        }
        try {
            CircleFunctionType ft = CircleFunctionType.valueOf(data.functionId.toUpperCase());
            return Component.translatable(ft.getTranslationKey());
        } catch (IllegalArgumentException ignored) {
            return Component.translatable("circle.transcend.function." + data.functionId);
        }
    }

    private Component getStateComponent(CircleCoreData data) {
        if (!data.structureValid) {
            return Component.translatable("gui.transcend.circle_core.state.invalid");
        }
        if (data.active) {
            return Component.translatable("gui.transcend.circle_core.state.active");
        }
        return Component.translatable("gui.transcend.circle_core.state.dormant");
    }

    private int getStateColor(CircleCoreData data) {
        if (!data.structureValid) return COLOR_ERR;
        if (data.active) return COLOR_OK;
        return COLOR_INACT;
    }
}
