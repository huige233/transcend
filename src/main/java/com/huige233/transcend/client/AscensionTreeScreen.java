package com.huige233.transcend.client;

import com.huige233.transcend.ascension.*;
import com.huige233.transcend.ascension.tree.NodeDefinition;
import com.huige233.transcend.ascension.tree.StatType;
import com.huige233.transcend.ascension.tree.TreeDefinition;
import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.network.C2SAscensionAction;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

/**
 * 飞升树/天赋树/概览/属性 — 四合一界面。
 *
 * v4 重构要点:
 *  - 统一 widget 追踪：所有页面级控件存入 {@link #pageWidgets}，切 tab 必清除，根治泄漏
 *  - 节点放大至 84×20，全名可见，元素色背景
 *  - 左侧 tier 标签栏 (T0..T5)，标注最低阶段
 *  - 树视图支持鼠标拖拽平移 + 滚轮
 *  - 树进度条 (已解锁/总数)
 *  - 概览页职业卡片化展示，仪式条目竖排
 */
public class AscensionTreeScreen extends Screen {

    // ─── 几何 ──────────────────────────────────────────────────────────
    // Round 40: W/H 改为 instance 字段以自适应小视口（默认 GUI 缩放 4 在 1080p 仅 480×270）
    private static final int W_DEFAULT   = 420;
    private static final int H_DEFAULT   = 300;
    private static final int TAB_H       = 16;
    private static final int BOT_H       = 18;
    private static final int TIER_LABEL_W = 28;

    /** 当前实际宽（按视口钳位） */
    private int W           = W_DEFAULT;
    /** 当前实际高（按视口钳位） */
    private int H           = H_DEFAULT;
    /** 内容区高 = H - TAB - BOT */
    private int CONTENT_H   = H_DEFAULT - TAB_H - BOT_H;

    private static final int NODE_W      = 84;
    private static final int NODE_H      = 20;
    private static final int NODE_GAP_X  = 8;
    private static final int TIER_GAP_Y  = 34;

    private static final int SCROLL_SPEED = 16;

    // ─── 调色板 ────────────────────────────────────────────────────────
    private static final int C_BG         = 0xF0050510;
    private static final int C_PANEL      = 0xC00C0C20;
    private static final int C_BORDER     = 0xFF2A2A5A;
    private static final int C_BORDER_HL  = 0xFF6464A0;
    private static final int C_GOLD       = 0xFFFFCC00;
    private static final int C_GRAY       = 0xFF8888AA;
    private static final int C_DARK_GRAY  = 0xFF444466;
    private static final int C_GREEN      = 0xFF44DD44;
    private static final int C_AVAIL      = 0xFF44AAFF;
    private static final int C_LOCKED     = 0xFF3A3A4A;
    private static final int C_TIER5      = 0xFFFF9933;
    private static final int C_TEXT       = 0xFFDDDDFF;
    private static final int C_TAB_ON     = 0xFF2A2A7A;
    private static final int C_TAB_OFF    = 0xFF101025;
    private static final int C_BAR_BG     = 0xFF222233;
    private static final int C_BAR_XP     = 0xFF22AA44;
    private static final int C_BAR_NODE   = 0xFF6688DD;

    // ─── 状态 ──────────────────────────────────────────────────────────
    private enum Tab { OVERVIEW, ASCENSION, TALENTS, STATS }
    private Tab activeTab = Tab.OVERVIEW;

    private PlayerAscensionData data;
    private NodeDefinition hoveredNode = null;

    private int scrollOffsetY = 0;
    private int panOffsetX = 0;
    private boolean isDragging = false;
    private double dragStartX = 0, dragStartY = 0;

    private final List<NodeButton>      nodeButtons = new ArrayList<>();
    private final List<AbstractWidget>  tabButtons  = new ArrayList<>();
    /** 当前 tab 专属控件 — 切 tab 必清。修复了概览按钮在切 tab 后残留的 bug */
    private final List<AbstractWidget>  pageWidgets = new ArrayList<>();

    public AscensionTreeScreen() {
        super(Component.translatable("screen.transcend.ascension_tree"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new AscensionTreeScreen());
    }

    // ─── 初始化 ────────────────────────────────────────────────────────
    @Override
    protected void init() {
        // Round 40: 按视口自适应宽高 — 默认 GUI 缩放下 1080p 视口仅 480×270，
        // 默认 420×300 会让底部按钮（仪式/誓约/重置/快速跳转）被裁掉。
        W = Math.min(W_DEFAULT, this.width - 4);
        H = Math.min(H_DEFAULT, this.height - 4);
        CONTENT_H = Math.max(60, H - TAB_H - BOT_H);

        refreshData();
        buildTabButtons();
        buildContentButtons();
    }

    private void refreshData() {
        if (minecraft != null && minecraft.player != null)
            data = AscensionCapability.get(minecraft.player);
        if (data == null) data = new PlayerAscensionData();
    }

    private void buildTabButtons() {
        tabButtons.forEach(this::removeWidget);
        tabButtons.clear();

        int ox = (width - W) / 2, oy = (height - H) / 2;
        String[] labels = { "概览", "飞升", "天赋", "属性" };
        Tab[] tabs = Tab.values();
        int tabW = W / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            final Tab t = tabs[i];
            Button b = Button.builder(Component.literal(labels[i]),
                    btn -> switchTab(t))
                    .bounds(ox + i * tabW, oy, tabW - 1, TAB_H).build();
            tabButtons.add(b);
            addRenderableWidget(b);
        }
    }

    private void switchTab(Tab tab) {
        activeTab = tab;
        scrollOffsetY = 0;
        panOffsetX = 0;
        buildContentButtons();
    }

    /** 清理上一个 tab 留下的全部控件，再为当前 tab 重建。 */
    private void buildContentButtons() {
        // 清节点 + 通用控件
        nodeButtons.forEach(nb -> removeWidget(nb.button));
        nodeButtons.clear();
        pageWidgets.forEach(this::removeWidget);
        pageWidgets.clear();

        int ox = (width - W) / 2;
        int contentTop = (height - H) / 2 + TAB_H;

        switch (activeTab) {
            case OVERVIEW  -> buildOverviewButtons(ox, contentTop);
            case ASCENSION -> buildTreeButtons(ox, contentTop, getAscensionTree());
            case TALENTS   -> buildTreeButtons(ox, contentTop, getTalentTree());
            case STATS     -> {}
        }
    }

    private TreeDefinition getAscensionTree() {
        if (ClientTreeCache.isLoaded()) return ClientTreeCache.getAscensionTree();
        return com.huige233.transcend.ascension.tree.TreeRegistry.getInstance().getAscensionTree();
    }

    private TreeDefinition getTalentTree() {
        if (!data.hasSelectedClass()) return null;
        if (ClientTreeCache.isLoaded()) return ClientTreeCache.getTalentTree(data.getMageClass());
        return com.huige233.transcend.ascension.tree.TreeRegistry.getInstance().getTalentTree(data.getMageClass());
    }

    // ─── 树视图按钮布局 ────────────────────────────────────────────────
    private void buildTreeButtons(int ox, int contentTop, TreeDefinition tree) {
        if (tree == null || data.getStage() < 2) return;

        int treeAreaLeft  = ox + TIER_LABEL_W;
        int treeAreaWidth = W - TIER_LABEL_W;
        int baseLine = contentTop + CONTENT_H - 30;

        for (NodeDefinition node : tree.getNodes().values()) {
            List<NodeDefinition> sameTier = tree.getNodesForTier(node.getTier());
            int idx = sameTier.indexOf(node);
            int count = sameTier.size();
            int totalW = count * NODE_W + (count - 1) * NODE_GAP_X;
            int nx = treeAreaLeft + (treeAreaWidth - totalW) / 2 + idx * (NODE_W + NODE_GAP_X) + panOffsetX;
            int ny = baseLine - node.getTier() * TIER_GAP_Y + scrollOffsetY;

            // FIX: 视口剔除 — 超出 gui 范围的 button 不添加,根治「飞升树超出 GUI」泄漏
            //   也禁止 button 漂进 tier label 列 (避免遮盖 T0..T5 标签)
            if (ny + NODE_H < contentTop || ny > contentTop + CONTENT_H) continue;
            if (nx < ox + TIER_LABEL_W || nx + NODE_W > ox + W - 2) continue;

            final String nodeId = node.getId();
            // FIX: 用 InvisibleNodeButton 替代 vanilla Button —
            //   vanilla Button.renderWidget 会绘制不透明 widget 纹理,
            //   覆盖我们提前画的 cost/✓/★/icon 等信息("缺少字符文本")
            //   InvisibleNodeButton 只接收点击,所有视觉在 super.render 后由 drawNodeOverlay 统一绘制
            Button btn = new InvisibleNodeButton(nx, ny, NODE_W, NODE_H,
                    b -> onNodeClick(nodeId));
            nodeButtons.add(new NodeButton(node, btn, nx, ny));
            addRenderableWidget(btn);
        }
    }

    private String fitNodeName(NodeDefinition node) {
        String s = node.getDisplayName().getString();
        // FIX: 中文字符比英文宽 ~50%, 必须用 font.width() 测像素而非 char count
        // 节点 84 像素宽,留 14 像素给 cost 徽章 + 4 像素 padding,可用 ≈ 66 像素
        final int maxPixelWidth = NODE_W - 18;
        if (font.width(s) <= maxPixelWidth) return s;
        // 二分式截断到能放下
        int lo = 1, hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (font.width(s.substring(0, mid) + "…") <= maxPixelWidth) lo = mid;
            else hi = mid - 1;
        }
        return s.substring(0, Math.max(1, lo)) + "…";
    }

    // ─── 概览页布局 ────────────────────────────────────────────────────
    private void buildOverviewButtons(int ox, int contentTop) {
        int innerLeft  = ox + 8;
        int innerRight = ox + W - 8;
        int y = contentTop + 4;

        // ── 1. 职业选择 (尚未觉醒时) ─────────────────────────────
        if (!data.hasSelectedClass()) {
            MageClass[] classes = { MageClass.PYROMANCER, MageClass.CRYOMANCER,
                    MageClass.STORMCALLER, MageClass.ARCANIST,
                    MageClass.ABYSSWALKER, MageClass.EARTHSHAPER,
                    MageClass.OMNIMANCER };
            int row = 0;
            for (int i = 0; i < classes.length; i++) {
                final MageClass mc = classes[i];
                int col = i % 3;
                row = i / 3;
                Button b = Button.builder(mc.getDisplayName(),
                        btn -> {
                            NetworkHandler.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                                    new C2SAscensionAction(0, mc.id));
                            refreshData();
                            buildContentButtons();
                        })
                        .bounds(innerLeft + col * 134, contentTop + 36 + row * 22, 130, 18).build();
                pageWidgets.add(b);
                addRenderableWidget(b);
            }
        }

        // ── 2. 仪式完成按钮 (满足条件时) ─────────────────────────
        AscensionRitual pending = data.getPendingRitual();
        if (pending != null && pending.isMet(data)) {
            Button ritualBtn = Button.builder(
                    Component.literal("▶ 完成：" + pending.getDisplayName().getString()),
                    b -> {
                        NetworkHandler.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                                new C2SAscensionAction(3, pending.name()));
                        refreshData();
                        buildContentButtons();
                    }
            ).bounds(innerLeft, contentTop + CONTENT_H - 64, W - 16, 18).build();
            pageWidgets.add(ritualBtn);
            addRenderableWidget(ritualBtn);
        }

        // ── 3. 誓约管理 (阶段 ≥ 1) ───────────────────────────────
        if (data.getStage() >= 1) {
            Button vowBtn = Button.builder(
                    Component.literal("✦ 誓约管理"),
                    b -> VowSelectionScreen.open()
            ).bounds(innerLeft, contentTop + CONTENT_H - 42, (W - 24) / 2, 18).build();
            pageWidgets.add(vowBtn);
            addRenderableWidget(vowBtn);
        }

        // ── 4. 重置天赋 (已选职业 + 有解锁) ───────────────────────
        if (data.hasSelectedClass() && (!data.getUnlockedNodes().isEmpty() || data.hasMastery())) {
            boolean hasPotion = hasRespecPotion();
            int btnX = innerLeft + (W - 24) / 2 + 8;
            int btnW = (W - 24) / 2;
            Button respecBtn = Button.builder(
                    Component.literal(hasPotion ? "⟲ 重置天赋" : "⟲ 重置(需洗点水)"),
                    b -> {
                        if (hasRespecPotion()) {
                            NetworkHandler.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                                    new C2SAscensionAction(4, ""));
                        }
                    }
            ).bounds(btnX, contentTop + CONTENT_H - 42, btnW, 18).build();
            respecBtn.active = hasPotion;
            pageWidgets.add(respecBtn);
            addRenderableWidget(respecBtn);
        }

        // ── 5. 快速跳转到飞升/天赋 (已飞升) ──────────────────────
        if (data.getStage() >= 2) {
            Button gotoAsc = Button.builder(
                    Component.literal("→ 飞升树"),
                    b -> switchTab(Tab.ASCENSION)
            ).bounds(innerLeft, contentTop + CONTENT_H - 20, (W - 24) / 2, 16).build();
            pageWidgets.add(gotoAsc);
            addRenderableWidget(gotoAsc);

            Button gotoTal = Button.builder(
                    Component.literal("→ 天赋树"),
                    b -> switchTab(Tab.TALENTS)
            ).bounds(innerLeft + (W - 24) / 2 + 8, contentTop + CONTENT_H - 20, (W - 24) / 2, 16).build();
            pageWidgets.add(gotoTal);
            addRenderableWidget(gotoTal);
        }
    }

    private boolean hasRespecPotion() {
        if (minecraft == null || minecraft.player == null) return false;
        var inv = minecraft.player.getInventory();
        var respecItem = ModItems.respec_potion.get();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() == respecItem) return true;
        }
        return minecraft.player.isCreative();
    }

    private void onNodeClick(String nodeId) {
        if (data.isNodeUnlocked(nodeId) || !data.canUnlock(nodeId)) return;
        NodeDefinition node = com.huige233.transcend.ascension.tree.TreeRegistry.getInstance().getNode(nodeId);
        if (node != null && data.getTalentPoints() < node.getCost()) return;
        NetworkHandler.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                new C2SAscensionAction(1, nodeId));
    }

    // ─── 输入 ──────────────────────────────────────────────────────────
    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (activeTab == Tab.ASCENSION || activeTab == Tab.TALENTS || activeTab == Tab.STATS) {
            scrollOffsetY = Math.max(-400, Math.min(400, scrollOffsetY + (int)(delta * SCROLL_SPEED)));
            if (activeTab != Tab.STATS) buildContentButtons();
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // 树视图开始拖拽 (右键)
        if (button == 1 && (activeTab == Tab.ASCENSION || activeTab == Tab.TALENTS)) {
            isDragging = true;
            dragStartX = mx;
            dragStartY = my;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 1 && isDragging) {
            isDragging = false;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (isDragging && (activeTab == Tab.ASCENSION || activeTab == Tab.TALENTS)) {
            panOffsetX  = Math.max(-200, Math.min(200, panOffsetX + (int) dx));
            scrollOffsetY = Math.max(-400, Math.min(400, scrollOffsetY + (int) dy));
            buildContentButtons();
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    // ─── 渲染 ──────────────────────────────────────────────────────────
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        refreshData();

        int ox = (width - W) / 2, oy = (height - H) / 2;
        int contentTop = oy + TAB_H;

        drawFrame(g, ox, oy);
        drawTabs(g, ox, oy);

        // ── Phase 1: 树视图 backdrop (BG/tier 标签/连线) — 在 scissor 中 ──
        g.enableScissor(ox + 1, contentTop, ox + W - 1, contentTop + CONTENT_H);

        switch (activeTab) {
            case OVERVIEW  -> drawOverview(g, ox, contentTop, mx, my);
            case ASCENSION -> drawTreeView(g, ox, contentTop, mx, my, getAscensionTree(), "飞升树");
            case TALENTS   -> drawTreeView(g, ox, contentTop, mx, my, getTalentTree(),
                    data.hasSelectedClass()
                            ? "天赋树 · " + data.getMageClass().getDisplayName().getString()
                            : "天赋树");
            case STATS     -> drawStats(g, ox, contentTop, mx, my);
        }

        g.disableScissor();
        drawBottomBar(g, ox, oy);

        // ── Phase 2: super.render — InvisibleNodeButton 接收点击但不画自身 ──
        super.render(g, mx, my, pt);

        // ── Phase 3: 节点 overlay (BG fill/border/icon/name/cost/✓/★/hover ring)
        //   必须在 super.render 后绘制 — InvisibleNodeButton 不画自身,我们在此叠加完整视觉
        if (activeTab == Tab.ASCENSION || activeTab == Tab.TALENTS) {
            g.enableScissor(ox + 1, contentTop, ox + W - 1, contentTop + CONTENT_H);
            hoveredNode = null;
            for (NodeButton nb : nodeButtons) {
                drawNodeOverlay(g, nb, mx, my);
            }
            // Tier 标签栏置顶 — 防止节点 (panOffsetX 负值) 漂入并遮盖 T0..T5/S1..S4 标签
            TreeDefinition currentTree = (activeTab == Tab.ASCENSION) ? getAscensionTree() : getTalentTree();
            if (currentTree != null && data.getStage() >= 2) {
                drawTierLabels(g, ox, contentTop, currentTree);
            }
            g.disableScissor();
        }

        drawTooltip(g, mx, my);
    }

    private void drawFrame(GuiGraphics g, int ox, int oy) {
        g.fill(ox, oy, ox + W, oy + H, C_BG);
        g.fill(ox, oy,         ox + W, oy + 1,     C_BORDER);
        g.fill(ox, oy + H - 1, ox + W, oy + H,     C_BORDER);
        g.fill(ox, oy,         ox + 1, oy + H,     C_BORDER);
        g.fill(ox + W - 1, oy, ox + W, oy + H,     C_BORDER);
    }

    private void drawTabs(GuiGraphics g, int ox, int oy) {
        Tab[] tabs = Tab.values();
        int tabW = W / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            int color = (tabs[i] == activeTab) ? C_TAB_ON : C_TAB_OFF;
            g.fill(ox + i * tabW, oy, ox + i * tabW + tabW - 1, oy + TAB_H, color);
        }
        g.fill(ox, oy + TAB_H, ox + W, oy + TAB_H + 1, C_BORDER);
    }

    // ─── 概览页 ────────────────────────────────────────────────────────
    private void drawOverview(GuiGraphics g, int ox, int contentTop, int mx, int my) {
        int x = ox + 8, y = contentTop + 6;

        // 标题块
        String classLine = data.hasSelectedClass()
                ? "§l职业 §r" + data.getMageClass().getDisplayName().getString()
                : "§e尚未觉醒 — 选择职业开启飞升之路";
        g.drawString(font, classLine, x, y, C_GOLD, false);
        y += 12;

        if (data.getStage() >= 1) {
            String mastLine = data.hasMastery()
                    ? "§l元素 §r" + data.getMastery().getDisplayName().getString() + " §8(锁定)"
                    : "§7元素 §o未选择 (天赋树中绑定)";
            g.drawString(font, mastLine, x, y, C_TEXT, false);
            y += 12;
        }

        // 等级 + 进度条
        g.drawString(font,
                "§7飞升等级 §f" + data.getAscensionLevel() + " §7/ §f" + PlayerAscensionData.MAX_LEVEL,
                x, y, C_TEXT, false);
        y += 10;
        int bw = W - 16;
        g.fill(x, y, x + bw, y + 4, C_BAR_BG);
        g.fill(x, y, x + (int)(bw * data.getLevelProgress()), y + 4, C_BAR_XP);

        // XP 数字 (居中显示在进度条内)
        String xpText;
        if (data.getAscensionLevel() >= PlayerAscensionData.MAX_LEVEL) {
            xpText = "§a§lMAX §f" + data.getAscensionXP() + " XP";
        } else {
            long current = data.getAscensionXP();
            long next = data.getXPForNextLevel();
            long nextRel = next - data.getXPForCurrentLevel();
            long currentRel = current - data.getXPForCurrentLevel();
            xpText = String.format("§f%d §7/ §f%d §8(总 %d)", currentRel, nextRel, current);
        }
        int xpW = font.width(xpText);
        g.drawString(font, xpText, x + (bw - xpW) / 2, y - 3, 0xFFFFFFFF, true);
        y += 10;

        // 数据行
        g.drawString(font,
                String.format("击杀 §f%d§7  Boss §f%d§7  施法 §f%d§7  天赋点 §b%d",
                        data.getTotalKills(), data.getBossKills(),
                        data.getTotalCasts(), data.getTalentPoints()),
                x, y, C_GRAY, false);
        y += 12;
        g.fill(ox + 4, y, ox + W - 4, y + 1, C_BORDER);
        y += 4;

        // 阶段标题
        g.drawString(font, "§l阶段 " + data.getStage() + " / 4", x, y, C_GOLD, false);
        y += 10;

        // 仪式列表
        for (AscensionRitual r : AscensionRitual.values()) {
            boolean done    = data.isRitualCompleted(r);
            boolean current = !done && r.stageIndex == data.getStage();
            int col = done ? C_GREEN : (current ? C_AVAIL : C_DARK_GRAY);
            String mark = done ? "✓ " : (current ? "► " : "  ");
            g.drawString(font, mark + r.getDisplayName().getString(), x, y, col, false);
            y += 10;
            if (current) {
                g.drawString(font, "  " + buildRitualProgress(r), x + 4, y, C_GRAY, false);
                y += 10;
            }
        }
    }

    private String buildRitualProgress(AscensionRitual r) {
        String base;
        if (r.requiredKills == 0 && r.requiredCasts == 0) {
            base = "选择职业后可完成";
        } else {
            base = String.format("击杀 %d/%d  施法 %d/%d  等级 %d/%d",
                    Math.min(data.getTotalKills(), r.requiredKills), r.requiredKills,
                    Math.min(data.getTotalCasts(), r.requiredCasts), r.requiredCasts,
                    data.getAscensionLevel(), r.requiredLevel);
            if (r.requiresBoss)
                base += String.format("  Boss %d/%d", data.getBossKills(), r.requiredBossKills());
        }
        String itemName = r.requiredItem.get().getDescription().getString();
        base += String.format("  §o[%s ×%d]", itemName, r.requiredItemCount);
        return base;
    }

    // ─── 树视图 ────────────────────────────────────────────────────────
    private void drawTreeView(GuiGraphics g, int ox, int contentTop, int mx, int my,
                              TreeDefinition tree, String title) {
        hoveredNode = null;

        if (data.getStage() < 2) {
            g.drawCenteredString(font, "§7完成「磨砺仪式」后解锁" + title,
                    ox + W / 2, contentTop + CONTENT_H / 2, C_LOCKED);
            return;
        }
        if (tree == null) {
            g.drawCenteredString(font, "§7请先在概览页选择职业",
                    ox + W / 2, contentTop + CONTENT_H / 2, C_LOCKED);
            return;
        }

        // 顶部标题 + 进度
        int unlocked = countUnlockedInTree(tree);
        int total = tree.getNodes().size();
        g.drawString(font, "§l" + title, ox + TIER_LABEL_W + 4, contentTop + 2, C_GOLD, false);
        String progress = String.format("§7已解锁 §a%d§7/§f%d  §7天赋点 §b%d",
                unlocked, total, data.getTalentPoints());
        g.drawString(font, progress, ox + W - font.width(progress) - 6, contentTop + 2, C_TEXT, false);

        // 左侧 tier 标签
        drawTierLabels(g, ox, contentTop, tree);

        // 连接线 (在 backdrop 中,InvisibleNodeButton 不挡)
        for (NodeButton nb : nodeButtons) {
            drawNodeConnections(g, nb);
        }

        // 节点高亮已移至 render() Phase 3 (drawNodeOverlay) — 在 super.render 后绘制

        // 底部提示
        String tip = "§7↕ 滚轮  §8|  §7右键拖拽";
        g.drawString(font, tip, ox + 6, contentTop + CONTENT_H - 12, C_GRAY, false);
    }

    private int countUnlockedInTree(TreeDefinition tree) {
        int n = 0;
        for (String id : data.getUnlockedNodes()) {
            if (tree.getNode(id) != null) n++;
        }
        return n;
    }

    private void drawTierLabels(GuiGraphics g, int ox, int contentTop, TreeDefinition tree) {
        // 左侧栏背景 + 渐变描边
        g.fill(ox + 1, contentTop + 12, ox + TIER_LABEL_W, contentTop + CONTENT_H - 1, C_PANEL);
        g.fill(ox + TIER_LABEL_W, contentTop + 12, ox + TIER_LABEL_W + 1, contentTop + CONTENT_H - 1, C_BORDER_HL);

        int maxTier = tree.getMaxTier();
        int baseLine = contentTop + CONTENT_H - 30;
        for (int tier = 0; tier <= maxTier; tier++) {
            int ny = baseLine - tier * TIER_GAP_Y + scrollOffsetY;
            if (ny + NODE_H < contentTop + 12 || ny > contentTop + CONTENT_H) continue;

            int minStage = minStageForTier(tier);
            boolean unlocked = data.getStage() >= minStage;
            int color = unlocked ? (tier == 5 ? C_TIER5 : C_TEXT) : C_LOCKED;

            // tier 行底色 (与节点同水平,更直观对齐)
            int rowTop = ny;
            int rowBot = ny + NODE_H;
            int rowColor = unlocked ? (tier == 5 ? 0x40FF9933 : 0x401A2A4A) : 0x40000000;
            g.fill(ox + 1, rowTop, ox + TIER_LABEL_W, rowBot, rowColor);

            // 标签文本
            String label = (tier == 5 ? "★" : ("T" + tier));
            int labelW = font.width(label);
            g.drawString(font, label,
                    ox + (TIER_LABEL_W - labelW) / 2 + 1, ny + 3, color, false);

            // 阶段提示
            String stageHint = (unlocked ? "§7" : "§8") + "S" + minStage;
            int hintW = font.width(stageHint);
            g.drawString(font, stageHint,
                    ox + (TIER_LABEL_W - hintW) / 2 + 1, ny + 12, C_DARK_GRAY, false);
        }
    }

    /**
     * 节点完整视觉 — 在 super.render 后绘制（InvisibleNodeButton 不挡）。
     *
     * <p>布局（84×20 节点）:
     * <pre>
     *   ┌─────────────────────────────────┐
     *   │█[icon] 名称 (truncated)      cost│ ← 上排 y+3
     *   │█                                 │
     *   │█ [status]                        │ ← 下排 y+NODE_H-9
     *   └─────────────────────────────────┘
     *   ▲ 3px 元素色色条
     * </pre>
     */
    private void drawNodeOverlay(GuiGraphics g, NodeButton nb, int mx, int my) {
        boolean unlocked  = data.isNodeUnlocked(nb.node.getId());
        boolean tierLocked = !data.isTierUnlockedByStage(nb.node.getTier());
        boolean canUnlock = !unlocked && !tierLocked && data.canUnlock(nb.node.getId())
                && data.getTalentPoints() >= nb.node.getCost();
        boolean isCapstone = nb.node.isTierFive();

        // ── 元素色 ──────────────────────────────────────────────────────
        int elemColor = 0xFFFFFFFF;
        if (nb.node.getColor() != null && nb.node.getColor().getColor() != null) {
            elemColor = 0xFF000000 | nb.node.getColor().getColor();
        }

        // ── 背景填充（不透明，盖住 vanilla 按钮纹理痕迹） ────────────────
        int fill;
        int border;
        if (unlocked) {
            fill = 0xFF143E22;    // 深绿底
            border = 0xFF44DD44;
        } else if (isCapstone) {
            fill = canUnlock ? 0xFF6E4218 : 0xFF3C2810;
            border = 0xFFFF9933;
        } else if (canUnlock) {
            fill = 0xFF1B3B5E;    // 深蓝底
            border = 0xFF44AAFF;
        } else if (tierLocked) {
            fill = 0xFF1A1A26;
            border = 0xFF3A3A50;
        } else {
            fill = 0xFF14141C;
            border = 0xFF2A2A38;
        }

        // ── Capstone 外层光晕 ──────────────────────────────────────────
        if (isCapstone) {
            int glow = (canUnlock || unlocked) ? 0x80FF9933 : 0x40FF9933;
            g.fill(nb.x - 2, nb.y - 1, nb.x,          nb.y + NODE_H + 1, glow);
            g.fill(nb.x + NODE_W, nb.y - 1, nb.x + NODE_W + 2, nb.y + NODE_H + 1, glow);
        }

        // ── 边框 (1px) ──────────────────────────────────────────────────
        g.fill(nb.x - 1, nb.y - 1, nb.x + NODE_W + 1, nb.y, border);
        g.fill(nb.x - 1, nb.y + NODE_H, nb.x + NODE_W + 1, nb.y + NODE_H + 1, border);
        g.fill(nb.x - 1, nb.y, nb.x, nb.y + NODE_H, border);
        g.fill(nb.x + NODE_W, nb.y, nb.x + NODE_W + 1, nb.y + NODE_H, border);

        // ── 背景填充 ────────────────────────────────────────────────────
        g.fill(nb.x, nb.y, nb.x + NODE_W, nb.y + NODE_H, fill);

        // ── 左侧 3px 元素色色条 ─────────────────────────────────────────
        g.fill(nb.x, nb.y, nb.x + 3, nb.y + NODE_H, elemColor);

        // ── 类型图标 (基于 primary StatType) ────────────────────────────
        String typeIcon = getTypeIconForNode(nb.node);
        int typeColor = getTypeColorForNode(nb.node);
        int iconX = nb.x + 5;
        g.drawString(font, typeIcon, iconX, nb.y + 3, typeColor, false);

        // ── 节点名 (截断到剩余像素宽) ───────────────────────────────────
        int nameStartX = iconX + font.width(typeIcon) + 2;
        int costStr_w = font.width(String.valueOf(nb.node.getCost()));
        int nameMaxW = nb.x + NODE_W - 3 - nameStartX;  // 右留 3px padding
        String name = fitNodeNameToWidth(nb.node, nameMaxW);
        int nameColor = unlocked ? 0xFFFFFFFF
                : canUnlock ? 0xFFCCDDFF
                : tierLocked ? 0xFF666680
                : 0xFFAAAACC;
        g.drawString(font, name, nameStartX, nb.y + 3, nameColor, false);

        // ── 下排: 状态符号 + cost ───────────────────────────────────────
        // 状态符号 (左)
        String status;
        int statusColor;
        if (unlocked) { status = "§a✓"; statusColor = 0xFF44DD44; }
        else if (tierLocked) { status = "§8✗"; statusColor = 0xFF888888; }
        else if (isCapstone) { status = "§6★"; statusColor = 0xFFFF9933; }
        else if (canUnlock) { status = "§b►"; statusColor = 0xFF44AAFF; }
        else { status = "§7·"; statusColor = 0xFF888888; }
        g.drawString(font, status, nb.x + 5, nb.y + NODE_H - 9, statusColor, false);

        // 消耗 (右)
        String cost = String.valueOf(nb.node.getCost());
        int costX = nb.x + NODE_W - font.width(cost) - 3;
        int costColor = unlocked ? 0xFF888888
                : (data.getTalentPoints() >= nb.node.getCost() ? 0xFF44AAFF : 0xFFAA4444);
        g.drawString(font, cost, costX, nb.y + NODE_H - 9, costColor, false);

        // ── 悬停高亮 + tooltip target ──────────────────────────────────
        if (mx >= nb.x && mx <= nb.x + NODE_W && my >= nb.y && my <= nb.y + NODE_H) {
            hoveredNode = nb.node;
            int hl = 0xFFFFFFFF;
            int x1 = nb.x - 2, y1 = nb.y - 2, x2 = nb.x + NODE_W + 2, y2 = nb.y + NODE_H + 2;
            g.fill(x1, y1, x2, y1 + 1, hl);
            g.fill(x1, y2 - 1, x2, y2, hl);
            g.fill(x1, y1, x1 + 1, y2, hl);
            g.fill(x2 - 1, y1, x2, y2, hl);
        }
    }

    /** 节点名按精确像素截断（与 fitNodeName 不同，maxW 可变）。 */
    private String fitNodeNameToWidth(NodeDefinition node, int maxW) {
        String s = node.getDisplayName().getString();
        if (s == null || s.isEmpty()) return "?";
        if (font.width(s) <= maxW) return s;
        int lo = 1, hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (font.width(s.substring(0, mid) + "…") <= maxW) lo = mid;
            else hi = mid - 1;
        }
        return s.substring(0, Math.max(1, lo)) + "…";
    }

    /** 根据节点主属性返回 1-2 字符图标。 */
    private static String getTypeIconForNode(NodeDefinition node) {
        var stats = node.getStatBonuses();
        if (stats == null || stats.isEmpty()) return "§7◆";
        // 取第一项（LinkedHashMap 插入顺序 = 主要属性）
        StatType primary = stats.keySet().iterator().next();
        return switch (primary) {
            case BONUS_MAX_HEALTH       -> "§c♥";
            case SPELL_POWER_BONUS      -> "§d✦";
            case COOLDOWN_REDUCTION     -> "§5⟳";
            case BONUS_MANA_CAPACITY    -> "§9◆";
            case MANA_REGEN_BONUS       -> "§b＋";
            case MOVE_SPEED_BONUS       -> "§a➤";
            case REACTION_BONUS         -> "§e⚡";
            case SUMMON_DAMAGE_BONUS    -> "§6☼";
            case CRIT_CHANCE            -> "§6✶";
            case CRIT_MULTIPLIER        -> "§6☀";
            case INCOMING_SPELL_DAMAGE_REDUCTION -> "§b❖";
            case ARMOR_PENETRATION      -> "§c⚔";
            case RESISTANCE_IGNORE      -> "§4☄";
            case DAMAGE_REDUCTION_FLAT  -> "§7◊";
            case SPELL_VAMP             -> "§d♺";
            case LIFESTEAL              -> "§4♥";
            case XP_GAIN_MULT           -> "§e★";
            case DODGE_CHANCE           -> "§b⏃";
            case DAMAGE_REDUCTION_PERCENT -> "§7▼";
            case MANA_COST_REDUCTION    -> "§b❄";
        };
    }

    /** 类型图标对应的 ARGB 颜色（用于 drawString fallback,实际由 §x 控制）。 */
    private static int getTypeColorForNode(NodeDefinition node) {
        return 0xFFFFFFFF;  // §x 已在字符串中控制
    }

    /** 简化的 ARGB 颜色混合（保留 — 未来可能仍需要）。 */
    private static int blend(int bg, int fg) {
        int bgA = (bg >>> 24) & 0xFF;
        int fgA = (fg >>> 24) & 0xFF;
        int outA = Math.min(255, bgA + fgA);
        int outR = clamp255(((bg >>> 16) & 0xFF) + ((fg >>> 16) & 0xFF) / 2);
        int outG = clamp255(((bg >>> 8) & 0xFF) + ((fg >>> 8) & 0xFF) / 2);
        int outB = clamp255((bg & 0xFF) + (fg & 0xFF) / 2);
        return (outA << 24) | (outR << 16) | (outG << 8) | outB;
    }

    private static int clamp255(int v) { return Math.max(0, Math.min(255, v)); }

    private void drawNodeConnections(GuiGraphics g, NodeButton nb) {
        for (String parentId : nb.node.getParents()) {
            NodeButton pb = findNodeButton(parentId);
            if (pb == null) continue;
            int x1 = pb.x + NODE_W / 2, y1 = pb.y;
            int x2 = nb.x + NODE_W / 2, y2 = nb.y + NODE_H;
            boolean bothUnlocked = data.isNodeUnlocked(parentId) && data.isNodeUnlocked(nb.node.getId());
            boolean parentUnlocked = data.isNodeUnlocked(parentId);
            int lc = bothUnlocked ? C_GREEN
                   : (parentUnlocked ? 0xFF44AAFF // 父已解,子未解 — 蓝色提示可解
                                     : 0xFF334466);

            // Z 形线: 父节点上 → 中段水平 → 子节点底 (2 像素粗,更可见)
            int midY = (y1 + y2) / 2;
            // 父顶 → 中线 (2px 厚)
            g.fill(x1 - 1, midY, x1 + 1, y1, lc);
            // 中线水平
            int hxMin = Math.min(x1, x2), hxMax = Math.max(x1, x2);
            g.fill(hxMin, midY - 1, hxMax + 1, midY + 1, lc);
            // 子底 → 中线
            g.fill(x2 - 1, y2, x2 + 1, midY, lc);

            // 已解锁路径 — 在中点画小箭头 (▼)
            if (bothUnlocked) {
                int axMid = (x1 + x2) / 2;
                g.fill(axMid - 2, midY + 2, axMid + 3, midY + 3, lc);
                g.fill(axMid - 1, midY + 3, axMid + 2, midY + 4, lc);
                g.fill(axMid,     midY + 4, axMid + 1, midY + 5, lc);
            }
        }
    }

    private NodeButton findNodeButton(String nodeId) {
        for (NodeButton nb : nodeButtons) {
            if (nb.node.getId().equals(nodeId)) return nb;
        }
        return null;
    }

    // ─── 属性页 ────────────────────────────────────────────────────────
    private void drawStats(GuiGraphics g, int ox, int contentTop, int mx, int my) {
        if (!data.hasSelectedClass()) {
            g.drawCenteredString(font, "§7完成觉醒仪式后可查看属性",
                    ox + W / 2, contentTop + CONTENT_H / 2, C_LOCKED);
            return;
        }

        int x = ox + 8, y = contentTop + 4 + scrollOffsetY;
        int col2 = ox + W / 2 + 8;

        g.drawString(font, "§l" + data.getMageClass().getDisplayName().getString(), x, y, C_GOLD, false);
        g.drawString(font,
                String.format("§7等级 §f%d§7/10  §7阶段 §f%d§7/4",
                        data.getAscensionLevel(), data.getStage()),
                col2, y, C_GRAY, false);
        y += 12;
        g.fill(ox + 4, y, ox + W - 4, y + 1, C_BORDER);
        y += 4;

        AscensionStatBlock levelStats = AscensionStatBlock.fromLevel(data.getAscensionLevel(), data.getMageClass());
        g.drawString(font, "§l职业等级成长", x, y, 0xFFAABBFF, false);
        y += 10;
        drawStatBlock(g, x, col2, y, levelStats);
        y += 52;

        g.fill(ox + 4, y, ox + W - 4, y + 1, C_BORDER);
        y += 4;
        AscensionStatBlock ascStats = computeTreeStats(true);
        g.drawString(font, "§l飞升树加成", x, y, 0xFFFFCC44, false);
        y += 10;
        drawStatBlock(g, x, col2, y, ascStats);
        y += 52;

        g.fill(ox + 4, y, ox + W - 4, y + 1, C_BORDER);
        y += 4;
        AscensionStatBlock talentStats = computeTreeStats(false);
        g.drawString(font, "§l天赋树加成", x, y, 0xFF44DDAA, false);
        y += 10;
        drawStatBlock(g, x, col2, y, talentStats);
        y += 52;

        g.fill(ox + 4, y, ox + W - 4, y + 1, C_BORDER);
        y += 4;
        AscensionStatBlock total = data.buildTotalStats();
        g.drawString(font, "§l§e合计", x, y, C_GOLD, false);
        y += 10;
        drawStatBlock(g, x, col2, y, total);
        y += 60;

        if (total.getEffectiveArmorPen() > 0 || total.getEffectiveResistIgnore() > 0
                || total.getEffectiveSpellVamp() > 0 || total.damageReductionFlat > 0) {
            g.fill(ox + 4, y, ox + W - 4, y + 1, C_BORDER);
            y += 4;
            g.drawString(font, "§l战斗属性", x, y, 0xFFFF8844, false);
            y += 10;
            drawStat(g, x, y, "穿甲", String.format("%.0f%%", total.getEffectiveArmorPen() * 100));
            drawStat(g, col2, y, "无视抗性", String.format("%.0f%%", total.getEffectiveResistIgnore() * 100));
            y += 10;
            drawStat(g, x, y, "法术吸血", String.format("%.0f%%", total.getEffectiveSpellVamp() * 100));
            drawStat(g, col2, y, "固定减伤", String.format("%.1f", total.damageReductionFlat));
        }
    }

    private void drawStatBlock(GuiGraphics g, int x, int col2, int y, AscensionStatBlock s) {
        // 第 1 行: 生命 / 法强
        if (s.bonusMaxHealth != 0)
            drawStat(g, x, y, "生命", String.format("+%.0f", s.bonusMaxHealth));
        if (s.spellPowerBonus != 0)
            drawStat(g, col2, y, "法强", String.format("+%.1f%%", s.spellPowerBonus * 100));
        y += 10;
        // 第 2 行: CDR / 魔力上限
        if (s.cooldownReduction != 0)
            drawStat(g, x, y, "CDR", String.format("+%.1f%%", s.cooldownReduction * 100));
        if (s.bonusManaCapacity != 0)
            drawStat(g, col2, y, "魔力", "+" + s.bonusManaCapacity);
        y += 10;
        // 第 3 行: 移速 / 暴击率
        if (s.moveSpeedBonus != 0)
            drawStat(g, x, y, "速度", String.format("+%.1f%%", s.moveSpeedBonus * 100));
        if (s.critChance != 0)
            drawStat(g, col2, y, "暴击率", String.format("+%.1f%%", s.critChance * 100));
        y += 10;
        // 第 4 行: 魔力回复 / 暴击倍率
        if (s.manaRegenBonus != 0)
            drawStat(g, x, y, "回蓝", String.format("+%.2f/s", s.manaRegenBonus));
        if (s.critMultiplier > 1.0f)
            drawStat(g, col2, y, "暴伤", String.format("×%.2f", s.critMultiplier));
        y += 10;
        // 第 5 行: 反应 / 召唤
        if (s.reactionBonus != 0)
            drawStat(g, x, y, "反应", String.format("+%.1f%%", s.reactionBonus * 100));
        if (s.summonDamageBonus != 0)
            drawStat(g, col2, y, "召唤伤害", String.format("+%.1f%%", s.summonDamageBonus * 100));
    }

    private AscensionStatBlock computeTreeStats(boolean ascensionTree) {
        AscensionStatBlock block = new AscensionStatBlock();
        var reg = com.huige233.transcend.ascension.tree.TreeRegistry.getInstance();
        var tree = ascensionTree ? reg.getAscensionTree() : reg.getTalentTree(data.getMageClass());
        if (tree == null) return block;
        for (String nodeId : data.getUnlockedNodes()) {
            var node = tree.getNode(nodeId);
            if (node == null) continue;
            for (var entry : node.getStatBonuses().entrySet()) {
                // FIX: 使用与 TreeRegistry.computeNodeStats 相同的 NODE_STAT_GLOBAL_MULT
                // 避免 UI 显示与实际生效不一致
                entry.getKey().applyTo(block,
                        entry.getValue() * com.huige233.transcend.ascension.tree.TreeRegistry.NODE_STAT_GLOBAL_MULT);
            }
        }
        return block;
    }

    private void drawStat(GuiGraphics g, int x, int y, String label, String val) {
        g.drawString(font, "§7" + label + "  §f" + val, x, y, C_TEXT, false);
    }

    private void drawBottomBar(GuiGraphics g, int ox, int oy) {
        int botY = oy + H - BOT_H;
        g.fill(ox, botY, ox + W, botY + 1, C_BORDER);
        String hint = String.format("§7阶段 §f%d§7/4  §8|  §7等级 §f%d§7/10  §8|  §7天赋点 §b%d",
                data.getStage(), data.getAscensionLevel(), data.getTalentPoints());
        g.drawCenteredString(font, hint, ox + W / 2, botY + 5, C_GRAY);
    }

    // ─── 悬停提示 ──────────────────────────────────────────────────────
    private void drawTooltip(GuiGraphics g, int mx, int my) {
        if (hoveredNode != null) drawNodeTooltip(g, hoveredNode, mx, my);
    }

    private void drawNodeTooltip(GuiGraphics g, NodeDefinition node, int mx, int my) {
        boolean unlocked   = data.isNodeUnlocked(node.getId());
        boolean tierLocked = !data.isTierUnlockedByStage(node.getTier());

        List<Component> lines = new ArrayList<>();
        lines.add(node.getDisplayName());
        lines.add(node.getDescription());

        // ── 属性加成清单 ─────────────────────────────────────────────────
        var stats = node.getStatBonuses();
        if (stats != null && !stats.isEmpty()) {
            lines.add(Component.empty());
            lines.add(Component.literal("§l属性加成"));
            for (var entry : stats.entrySet()) {
                lines.add(formatStatLine(entry.getKey(), entry.getValue()));
            }
        }

        // ── 被动效果数量 (有的话提示) ─────────────────────────────────────
        var passives = node.getPassiveEffects();
        if (passives != null && !passives.isEmpty()) {
            lines.add(Component.literal("§7§o含 " + passives.size() + " 个被动效果"));
        }

        // ── 元素亲和 (如果有 element_scaling) ────────────────────────────
        var elemScaling = node.getAllElementScaling();
        if (elemScaling != null && !elemScaling.isEmpty()) {
            lines.add(Component.literal("§7§o含 " + elemScaling.size() + " 项元素亲和"));
        }

        lines.add(Component.empty());

        // 阶段 + 消耗
        lines.add(Component.literal("阶段要求 ")
                .append(Component.literal(String.valueOf(minStageForTier(node.getTier())))
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal("  消耗 "))
                .append(Component.literal(node.getCost() + "点")
                        .withStyle(ChatFormatting.AQUA))
                .withStyle(ChatFormatting.GRAY));

        // 状态行
        if (unlocked) {
            lines.add(Component.literal("✓ 已解锁").withStyle(ChatFormatting.GREEN));
        } else if (tierLocked) {
            lines.add(Component.literal("✗ 阶段不足，需完成更多仪式")
                    .withStyle(ChatFormatting.RED));
        } else if (!data.canUnlock(node.getId())) {
            lines.add(Component.literal("✗ 前置节点未解锁")
                    .withStyle(ChatFormatting.RED));
        } else if (data.getTalentPoints() < node.getCost()) {
            lines.add(Component.literal("✗ 天赋点不足 (需 " + node.getCost() + " 点)")
                    .withStyle(ChatFormatting.RED));
        } else {
            lines.add(Component.literal("► 左键点击解锁")
                    .withStyle(ChatFormatting.AQUA));
        }

        g.renderComponentTooltip(font, lines, mx, my);
    }

    /** Stat type + 数值格式化为友好双语行。 */
    private static Component formatStatLine(StatType type, float value) {
        String label = statLabel(type);
        boolean isPercent = isPercentStat(type);
        String valStr;
        if (isPercent) {
            valStr = String.format("%+.1f%%", value * 100);
        } else if (type == StatType.CRIT_MULTIPLIER) {
            valStr = String.format("x%.2f", value);
        } else if (type == StatType.BONUS_MANA_CAPACITY) {
            valStr = String.format("%+.0f", value);
        } else {
            valStr = String.format("%+.1f", value);
        }
        ChatFormatting valColor = (value >= 0) ? ChatFormatting.GREEN : ChatFormatting.RED;
        return Component.literal("  §7" + label + "  ")
                .append(Component.literal(valStr).withStyle(valColor));
    }

    /** Stat type → 中文标签（与节点图标体系一致）。 */
    private static String statLabel(StatType type) {
        return switch (type) {
            case BONUS_MAX_HEALTH                -> "生命";
            case SPELL_POWER_BONUS               -> "法术强度";
            case COOLDOWN_REDUCTION              -> "冷却缩减";
            case BONUS_MANA_CAPACITY             -> "魔力上限";
            case MANA_REGEN_BONUS                -> "魔力回复";
            case MOVE_SPEED_BONUS                -> "移动速度";
            case REACTION_BONUS                  -> "反应增益";
            case SUMMON_DAMAGE_BONUS             -> "召唤伤害";
            case CRIT_CHANCE                     -> "暴击率";
            case CRIT_MULTIPLIER                 -> "暴击倍率";
            case INCOMING_SPELL_DAMAGE_REDUCTION -> "法术抗性";
            case ARMOR_PENETRATION               -> "穿甲";
            case RESISTANCE_IGNORE               -> "无视抗性";
            case DAMAGE_REDUCTION_FLAT           -> "固定减伤";
            case SPELL_VAMP                      -> "法术吸血";
            case LIFESTEAL                       -> "生命偷取";
            case XP_GAIN_MULT                    -> "经验加成";
            case DODGE_CHANCE                    -> "闪避率";
            case DAMAGE_REDUCTION_PERCENT        -> "百分比减伤";
            case MANA_COST_REDUCTION             -> "法术消耗减免";
        };
    }

    /** 哪些 stat 适合用百分比显示。 */
    private static boolean isPercentStat(StatType type) {
        return switch (type) {
            case SPELL_POWER_BONUS, COOLDOWN_REDUCTION, MANA_REGEN_BONUS,
                 MOVE_SPEED_BONUS, REACTION_BONUS, SUMMON_DAMAGE_BONUS,
                 CRIT_CHANCE, INCOMING_SPELL_DAMAGE_REDUCTION,
                 ARMOR_PENETRATION, RESISTANCE_IGNORE,
                 SPELL_VAMP, LIFESTEAL, XP_GAIN_MULT, DODGE_CHANCE,
                 DAMAGE_REDUCTION_PERCENT, MANA_COST_REDUCTION -> true;
            case BONUS_MAX_HEALTH, BONUS_MANA_CAPACITY,
                 CRIT_MULTIPLIER, DAMAGE_REDUCTION_FLAT -> false;
        };
    }

    private int minStageForTier(int tier) {
        return switch (tier) {
            case 0, 1, 2 -> 2;
            case 3, 4    -> 3;
            default      -> 4;
        };
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private record NodeButton(NodeDefinition node, Button button, int x, int y) {}

    /**
     * 不可见节点按钮 — 仅接收点击,不绘制 vanilla button 纹理。
     * 所有视觉由 {@link AscensionTreeScreen#drawNodeOverlay} 在 super.render 后统一画。
     * <p>解决 v4 的 bug: vanilla Button 的不透明 widget 纹理覆盖了节点 highlight。
     */
    private static class InvisibleNodeButton extends Button {
        public InvisibleNodeButton(int x, int y, int w, int h, Button.OnPress press) {
            super(x, y, w, h, net.minecraft.network.chat.Component.empty(),
                    press, Button.DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(net.minecraft.client.gui.GuiGraphics g,
                                    int mx, int my, float pt) {
            // 故意不绘制 — Screen 在 Phase 3 统一画整套节点 UI
        }
    }
}
