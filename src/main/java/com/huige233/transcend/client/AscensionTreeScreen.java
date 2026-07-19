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

public class AscensionTreeScreen extends Screen {

    private static final int W_DEFAULT   = 420;
    private static final int H_DEFAULT   = 300;
    private static final int TAB_H       = 16;
    private static final int BOT_H       = 18;
    private static final int TIER_LABEL_W = 28;

    private int W           = W_DEFAULT;

    private int H           = H_DEFAULT;

    private int CONTENT_H   = H_DEFAULT - TAB_H - BOT_H;

    private static final int NODE_W      = 84;
    private static final int NODE_H      = 20;
    private static final int NODE_GAP_X  = 8;
    private static final int TIER_GAP_Y  = 34;

    private static final int SCROLL_SPEED = 16;

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

    private final List<AbstractWidget>  pageWidgets = new ArrayList<>();

    public AscensionTreeScreen() {
        super(Component.translatable("screen.transcend.ascension_tree"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new AscensionTreeScreen());
    }

    @Override
    protected void init() {

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
        String[] labels = {
                "gui.transcend.tab.overview", "gui.transcend.tab.universal",
                "gui.transcend.tab.talents", "gui.transcend.tab.stats"
        };
        Tab[] tabs = Tab.values();
        int tabW = W / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            final Tab t = tabs[i];
            Button b = Button.builder(Component.translatable(labels[i]),
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

    private void buildContentButtons() {

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

    private void buildTreeButtons(int ox, int contentTop, TreeDefinition tree) {
        if (tree == null || data.getRitualTier() < 2) return;

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

            if (ny + NODE_H < contentTop || ny > contentTop + CONTENT_H) continue;
            if (nx < ox + TIER_LABEL_W || nx + NODE_W > ox + W - 2) continue;

            final String nodeId = node.getId();

            Button btn = new InvisibleNodeButton(nx, ny, NODE_W, NODE_H,
                    b -> onNodeClick(nodeId));
            nodeButtons.add(new NodeButton(node, btn, nx, ny));
            addRenderableWidget(btn);
        }
    }

    private String fitNodeName(NodeDefinition node) {
        String s = node.getDisplayName().getString();

        final int maxPixelWidth = NODE_W - 18;
        if (font.width(s) <= maxPixelWidth) return s;

        int lo = 1, hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (font.width(s.substring(0, mid) + "…") <= maxPixelWidth) lo = mid;
            else hi = mid - 1;
        }
        return s.substring(0, Math.max(1, lo)) + "…";
    }

    private void buildOverviewButtons(int ox, int contentTop) {
        int innerLeft  = ox + 8;
        int innerRight = ox + W - 8;
        int y = contentTop + 4;

        if (data.hasSelectedClass()) {
            boolean supportedSkill = data.getMageClass() == MageClass.CRYOMANCER
                    || data.getMageClass() == MageClass.STORMCALLER
                    || data.getMageClass() == MageClass.CHRONOWEAVER;
            int actionY = contentTop + CONTENT_H - 108;
            int auraWidth = supportedSkill ? (W - 24) / 2 : W - 16;
            Button auraButton = Button.builder(
                    Component.translatable(data.isAuraGuardEnabled()
                            ? "gui.transcend.aura_guard.disable"
                            : "gui.transcend.aura_guard.enable"),
                    b -> {
                        NetworkHandler.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                                new C2SAscensionAction(8, ""));
                        b.setMessage(Component.translatable(data.isAuraGuardEnabled()
                                ? "gui.transcend.aura_guard.enable"
                                : "gui.transcend.aura_guard.disable"));
                    })
                    .bounds(innerLeft, actionY, auraWidth, 18).build();
            auraButton.active = !data.hasCultivationDeviation() && !data.isTribulationActive();
            pageWidgets.add(auraButton);
            addRenderableWidget(auraButton);

            if (supportedSkill) {
                Button skillButton = Button.builder(
                        Component.translatable("gui.transcend.class_skill." + data.getMageClass().id),
                        b -> NetworkHandler.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                                new C2SAscensionAction(9, "")))
                        .bounds(innerLeft + auraWidth + 8, actionY, (W - 24) / 2, 18).build();
                pageWidgets.add(skillButton);
                addRenderableWidget(skillButton);
            }
        }

        if (!data.hasSelectedClass()) {
            MageClass[] allClasses = MageClass.values();
            java.util.List<MageClass> classes = new java.util.ArrayList<>();
            for (MageClass mc : allClasses) {
                if (mc != MageClass.NONE && !mc.hidden) classes.add(mc);
            }
            int row = 0;
            for (int i = 0; i < classes.size(); i++) {
                final MageClass mc = classes.get(i);
                int col = i % 3;
                row = i / 3;
                boolean classUnlocked = data.getRitualTier() >= 1;
                Component label = classUnlocked ? mc.getDisplayName() : Component.translatable(
                        "gui.transcend.class_requires_ritual_tier", mc.getDisplayName());
                Button b = Button.builder(label,
                        btn -> {
                            NetworkHandler.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                                    new C2SAscensionAction(0, mc.id));
                            refreshData();
                            buildContentButtons();
                        })
                        .bounds(innerLeft + col * 134, contentTop + 36 + row * 22, 130, 18).build();
                b.active = classUnlocked;
                pageWidgets.add(b);
                addRenderableWidget(b);
            }
        }

        AscensionRitual pending = data.getPendingRitual();
        if (pending != null && pending.isMet(data)) {
            Button ritualBtn = Button.builder(
                    Component.translatable("gui.transcend.complete_ritual", pending.getDisplayName()),
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

        if (data.getCultivationStage() == CultivationStage.LATE
                && data.getCultivationRealm() != CultivationRealm.CHAOS
                && !data.isTribulationActive() && !data.hasCultivationDeviation()) {
            Button tribulationBtn = Button.builder(
                    Component.translatable("gui.transcend.tribulation.start"),
                    b -> NetworkHandler.CHANNEL.send(PacketDistributor.SERVER.noArg(),
                            new C2SAscensionAction(7, ""))
            ).bounds(innerLeft, contentTop + CONTENT_H - 86, W - 16, 18).build();
            pageWidgets.add(tribulationBtn);
            addRenderableWidget(tribulationBtn);
        }

        if (data.getRitualTier() >= 1) {
            Button vowBtn = Button.builder(
                    Component.translatable("gui.transcend.vow.manage"),
                    b -> VowSelectionScreen.open()
            ).bounds(innerLeft, contentTop + CONTENT_H - 42, (W - 24) / 2, 18).build();
            pageWidgets.add(vowBtn);
            addRenderableWidget(vowBtn);
        }

        if (data.hasSelectedClass() && (!data.getUnlockedNodes().isEmpty() || data.hasMastery())) {
            boolean hasPotion = hasRespecPotion();
            int btnX = innerLeft + (W - 24) / 2 + 8;
            int btnW = (W - 24) / 2;
            Button respecBtn = Button.builder(
                    Component.translatable(hasPotion
                            ? "gui.transcend.respec"
                            : "gui.transcend.respec.requires_potion"),
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

        if (data.getRitualTier() >= 2) {
            Button gotoAsc = Button.builder(
                    Component.translatable("gui.transcend.goto_universal_tree"),
                    b -> switchTab(Tab.ASCENSION)
            ).bounds(innerLeft, contentTop + CONTENT_H - 20, (W - 24) / 2, 16).build();
            pageWidgets.add(gotoAsc);
            addRenderableWidget(gotoAsc);

            Button gotoTal = Button.builder(
                    Component.translatable("gui.transcend.goto_class_tree"),
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

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        refreshData();

        int ox = (width - W) / 2, oy = (height - H) / 2;
        int contentTop = oy + TAB_H;

        drawFrame(g, ox, oy);
        drawTabs(g, ox, oy);

        g.enableScissor(ox + 1, contentTop, ox + W - 1, contentTop + CONTENT_H);

        switch (activeTab) {
            case OVERVIEW  -> drawOverview(g, ox, contentTop, mx, my);
            case ASCENSION -> drawTreeView(g, ox, contentTop, mx, my, getAscensionTree(),
                    Component.translatable("tree.transcend.ascension.name"));
            case TALENTS   -> drawTreeView(g, ox, contentTop, mx, my, getTalentTree(),
                    data.hasSelectedClass()
                            ? Component.translatable("gui.transcend.class_tree_title", data.getMageClass().getDisplayName())
                            : Component.translatable("gui.transcend.tab.talents"));
            case STATS     -> drawStats(g, ox, contentTop, mx, my);
        }

        g.disableScissor();
        drawBottomBar(g, ox, oy);

        super.render(g, mx, my, pt);

        if (activeTab == Tab.ASCENSION || activeTab == Tab.TALENTS) {
            g.enableScissor(ox + 1, contentTop, ox + W - 1, contentTop + CONTENT_H);
            hoveredNode = null;
            for (NodeButton nb : nodeButtons) {
                drawNodeOverlay(g, nb, mx, my);
            }

            TreeDefinition currentTree = (activeTab == Tab.ASCENSION) ? getAscensionTree() : getTalentTree();
            if (currentTree != null && data.getRitualTier() >= 2) {
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

    private void drawOverview(GuiGraphics g, int ox, int contentTop, int mx, int my) {
        int x = ox + 8, y = contentTop + 6;

        Component classLine = data.hasSelectedClass()
                ? Component.translatable("gui.transcend.class_line", data.getMageClass().getDisplayName())
                : Component.translatable("gui.transcend.no_class_prompt");
        g.drawString(font, classLine, x, y, C_GOLD, false);
        y += 12;

        if (data.getRitualTier() >= 1) {
            Component mastLine = data.hasMastery()
                    ? Component.translatable("gui.transcend.mastery_line", data.getMastery().getDisplayName())
                    : Component.translatable("gui.transcend.mastery_unselected");
            g.drawString(font, mastLine, x, y, C_TEXT, false);
            y += 12;
        }

        g.drawString(font, Component.translatable("gui.transcend.insight_level_progress",
                data.getInsightLevel(), PlayerAscensionData.MAX_LEVEL), x, y, C_TEXT, false);
        y += 10;
        int bw = W - 16;
        g.fill(x, y, x + bw, y + 4, C_BAR_BG);
        g.fill(x, y, x + (int)(bw * data.getLevelProgress()), y + 4, C_BAR_XP);

        Component xpText;
        if (data.getInsightLevel() >= PlayerAscensionData.MAX_LEVEL) {
            xpText = Component.translatable("gui.transcend.insight_xp_max", data.getInsightXP());
        } else {
            long current = data.getInsightXP();
            long next = data.getXPForNextLevel();
            long nextRel = next - data.getXPForCurrentLevel();
            long currentRel = current - data.getXPForCurrentLevel();
            xpText = Component.translatable("gui.transcend.insight_xp_progress", currentRel, nextRel, current);
        }
        int xpW = font.width(xpText);
        g.drawString(font, xpText, x + (bw - xpW) / 2, y - 3, 0xFFFFFFFF, true);
        y += 10;

        g.drawString(font, Component.translatable("gui.transcend.progress_counts",
                        data.getTotalKills(), data.getBossKills(), data.getTotalCasts(), data.getTalentPoints()),
                x, y, C_GRAY, false);
        y += 12;
        g.fill(ox + 4, y, ox + W - 4, y + 1, C_BORDER);
        y += 4;

        g.drawString(font, Component.translatable("gui.transcend.ritual_tier_progress", data.getRitualTier(), 4),
                x, y, C_GOLD, false);
        y += 10;

        for (AscensionRitual r : AscensionRitual.values()) {
            boolean done    = data.isRitualCompleted(r);
            boolean current = !done && r.stageIndex == data.getRitualTier();
            int col = done ? C_GREEN : (current ? C_AVAIL : C_DARK_GRAY);
            String mark = done ? "✓ " : (current ? "► " : "  ");
            g.drawString(font, mark + r.getDisplayName().getString(), x, y, col, false);
            y += 10;
            if (current) {
                g.drawString(font, buildRitualProgress(r), x + 4, y, C_GRAY, false);
                y += 10;
            }
        }
    }

    private Component buildRitualProgress(AscensionRitual r) {
        Component base;
        if (r.requiredKills == 0 && r.requiredCasts == 0) {
            base = Component.translatable("gui.transcend.ritual.select_class");
        } else {
            base = Component.translatable("gui.transcend.ritual.requirements",
                    Math.min(data.getTotalKills(), r.requiredKills), r.requiredKills,
                    Math.min(data.getTotalCasts(), r.requiredCasts), r.requiredCasts,
                    data.getInsightLevel(), r.requiredLevel);
            if (r.requiresBoss)
                base = base.copy().append(Component.translatable("gui.transcend.ritual.boss_requirement",
                        data.getBossKills(), r.requiredBossKills()));
        }
        return base.copy().append(Component.translatable("gui.transcend.ritual.item_requirement",
                r.requiredItem.get().getDescription(), r.requiredItemCount));
    }

    private void drawTreeView(GuiGraphics g, int ox, int contentTop, int mx, int my,
                              TreeDefinition tree, Component title) {
        hoveredNode = null;

        if (data.getRitualTier() < 2) {
            g.drawCenteredString(font, Component.translatable("gui.transcend.tree.locked", title),
                    ox + W / 2, contentTop + CONTENT_H / 2, C_LOCKED);
            return;
        }
        if (tree == null) {
            g.drawCenteredString(font, Component.translatable("gui.transcend.tree.select_class"),
                    ox + W / 2, contentTop + CONTENT_H / 2, C_LOCKED);
            return;
        }

        int unlocked = countUnlockedInTree(tree);
        int total = tree.getNodes().size();
        g.drawString(font, title, ox + TIER_LABEL_W + 4, contentTop + 2, C_GOLD, false);
        Component progress = Component.translatable("gui.transcend.tree.progress",
                unlocked, total, data.getTalentPoints());
        g.drawString(font, progress, ox + W - font.width(progress) - 6, contentTop + 2, C_TEXT, false);

        drawTierLabels(g, ox, contentTop, tree);

        for (NodeButton nb : nodeButtons) {
            drawNodeConnections(g, nb);
        }

        Component tip = Component.translatable("gui.transcend.tree.controls");
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

        g.fill(ox + 1, contentTop + 12, ox + TIER_LABEL_W, contentTop + CONTENT_H - 1, C_PANEL);
        g.fill(ox + TIER_LABEL_W, contentTop + 12, ox + TIER_LABEL_W + 1, contentTop + CONTENT_H - 1, C_BORDER_HL);

        int maxTier = tree.getMaxTier();
        int baseLine = contentTop + CONTENT_H - 30;
        for (int tier = 0; tier <= maxTier; tier++) {
            int ny = baseLine - tier * TIER_GAP_Y + scrollOffsetY;
            if (ny + NODE_H < contentTop + 12 || ny > contentTop + CONTENT_H) continue;

            int minRealmRank = minRealmRankForTier(tier);
            boolean unlocked = data.getCultivationRealm().getRank() >= minRealmRank;
            int color = unlocked ? (tier == 5 ? C_TIER5 : C_TEXT) : C_LOCKED;

            int rowTop = ny;
            int rowBot = ny + NODE_H;
            int rowColor = unlocked ? (tier == 5 ? 0x40FF9933 : 0x401A2A4A) : 0x40000000;
            g.fill(ox + 1, rowTop, ox + TIER_LABEL_W, rowBot, rowColor);

            String label = (tier == 5 ? "★" : ("T" + tier));
            int labelW = font.width(label);
            g.drawString(font, label,
                    ox + (TIER_LABEL_W - labelW) / 2 + 1, ny + 3, color, false);

            String realmHint = (unlocked ? "§7" : "§8") + "R" + minRealmRank;
            int hintW = font.width(realmHint);
            g.drawString(font, realmHint,
                    ox + (TIER_LABEL_W - hintW) / 2 + 1, ny + 12, C_DARK_GRAY, false);
        }
    }

    private void drawNodeOverlay(GuiGraphics g, NodeButton nb, int mx, int my) {
        boolean unlocked  = data.isNodeUnlocked(nb.node.getId());
        boolean tierLocked = !data.isTierUnlockedByStage(nb.node.getTier());
        boolean canUnlock = !unlocked && !tierLocked && data.canUnlock(nb.node.getId())
                && data.getTalentPoints() >= nb.node.getCost();
        boolean isCapstone = nb.node.isTierFive();

        int elemColor = 0xFFFFFFFF;
        if (nb.node.getColor() != null && nb.node.getColor().getColor() != null) {
            elemColor = 0xFF000000 | nb.node.getColor().getColor();
        }

        int fill;
        int border;
        if (unlocked) {
            fill = 0xFF143E22;
            border = 0xFF44DD44;
        } else if (isCapstone) {
            fill = canUnlock ? 0xFF6E4218 : 0xFF3C2810;
            border = 0xFFFF9933;
        } else if (canUnlock) {
            fill = 0xFF1B3B5E;
            border = 0xFF44AAFF;
        } else if (tierLocked) {
            fill = 0xFF1A1A26;
            border = 0xFF3A3A50;
        } else {
            fill = 0xFF14141C;
            border = 0xFF2A2A38;
        }

        if (isCapstone) {
            int glow = (canUnlock || unlocked) ? 0x80FF9933 : 0x40FF9933;
            g.fill(nb.x - 2, nb.y - 1, nb.x,          nb.y + NODE_H + 1, glow);
            g.fill(nb.x + NODE_W, nb.y - 1, nb.x + NODE_W + 2, nb.y + NODE_H + 1, glow);
        }

        g.fill(nb.x - 1, nb.y - 1, nb.x + NODE_W + 1, nb.y, border);
        g.fill(nb.x - 1, nb.y + NODE_H, nb.x + NODE_W + 1, nb.y + NODE_H + 1, border);
        g.fill(nb.x - 1, nb.y, nb.x, nb.y + NODE_H, border);
        g.fill(nb.x + NODE_W, nb.y, nb.x + NODE_W + 1, nb.y + NODE_H, border);

        g.fill(nb.x, nb.y, nb.x + NODE_W, nb.y + NODE_H, fill);

        g.fill(nb.x, nb.y, nb.x + 3, nb.y + NODE_H, elemColor);

        String typeIcon = getTypeIconForNode(nb.node);
        int typeColor = getTypeColorForNode(nb.node);
        int iconX = nb.x + 5;
        g.drawString(font, typeIcon, iconX, nb.y + 3, typeColor, false);

        int nameStartX = iconX + font.width(typeIcon) + 2;
        int costStr_w = font.width(String.valueOf(nb.node.getCost()));
        int nameMaxW = nb.x + NODE_W - 3 - nameStartX;
        String name = fitNodeNameToWidth(nb.node, nameMaxW);
        int nameColor = unlocked ? 0xFFFFFFFF
                : canUnlock ? 0xFFCCDDFF
                : tierLocked ? 0xFF666680
                : 0xFFAAAACC;
        g.drawString(font, name, nameStartX, nb.y + 3, nameColor, false);

        String status;
        int statusColor;
        if (unlocked) { status = "§a✓"; statusColor = 0xFF44DD44; }
        else if (tierLocked) { status = "§8✗"; statusColor = 0xFF888888; }
        else if (isCapstone) { status = "§6★"; statusColor = 0xFFFF9933; }
        else if (canUnlock) { status = "§b►"; statusColor = 0xFF44AAFF; }
        else { status = "§7·"; statusColor = 0xFF888888; }
        g.drawString(font, status, nb.x + 5, nb.y + NODE_H - 9, statusColor, false);

        String cost = String.valueOf(nb.node.getCost());
        int costX = nb.x + NODE_W - font.width(cost) - 3;
        int costColor = unlocked ? 0xFF888888
                : (data.getTalentPoints() >= nb.node.getCost() ? 0xFF44AAFF : 0xFFAA4444);
        g.drawString(font, cost, costX, nb.y + NODE_H - 9, costColor, false);

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

    private static String getTypeIconForNode(NodeDefinition node) {
        var stats = node.getStatBonuses();
        if (stats == null || stats.isEmpty()) return "§7◆";

        StatType primary = stats.keySet().iterator().next();
        return switch (primary) {
            case BONUS_MAX_HEALTH       -> "§c♥";
            case SPELL_POWER_BONUS      -> "§d✦";
            case COOLDOWN_REDUCTION     -> "§5⟳";
            case BONUS_MANA_CAPACITY    -> "§9◆";
            case MANA_REGEN_BONUS       -> "§b＋";
            case MOVE_SPEED_BONUS       -> "§a➤";
            case REACTION_BONUS         -> "§e⚡";
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

    private static int getTypeColorForNode(NodeDefinition node) {
        return 0xFFFFFFFF;
    }

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
                   : (parentUnlocked ? 0xFF44AAFF
                                     : 0xFF334466);

            int midY = (y1 + y2) / 2;

            g.fill(x1 - 1, midY, x1 + 1, y1, lc);

            int hxMin = Math.min(x1, x2), hxMax = Math.max(x1, x2);
            g.fill(hxMin, midY - 1, hxMax + 1, midY + 1, lc);

            g.fill(x2 - 1, y2, x2 + 1, midY, lc);

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

    private void drawStats(GuiGraphics g, int ox, int contentTop, int mx, int my) {
        if (!data.hasSelectedClass()) {
            g.drawCenteredString(font, Component.translatable("gui.transcend.stats.locked"),
                    ox + W / 2, contentTop + CONTENT_H / 2, C_LOCKED);
            return;
        }

        int x = ox + 8, y = contentTop + 4 + scrollOffsetY;
        int col2 = ox + W / 2 + 8;

        g.drawString(font, "§l" + data.getMageClass().getDisplayName().getString(), x, y, C_GOLD, false);
        g.drawString(font, Component.translatable("gui.transcend.stats.progress",
                        data.getInsightLevel(), PlayerAscensionData.MAX_LEVEL, data.getRitualTier(), 4),
                col2, y, C_GRAY, false);
        y += 12;
        g.fill(ox + 4, y, ox + W - 4, y + 1, C_BORDER);
        y += 4;

        AscensionStatBlock levelStats = AscensionStatBlock.fromLevel(data.getInsightLevel(), data.getMageClass());
        g.drawString(font, Component.translatable("gui.transcend.stats.insight_growth"), x, y, 0xFFAABBFF, false);
        y += 10;
        drawStatBlock(g, x, col2, y, levelStats);
        y += 52;

        g.fill(ox + 4, y, ox + W - 4, y + 1, C_BORDER);
        y += 4;
        AscensionStatBlock ascStats = computeTreeStats(true);
        g.drawString(font, Component.translatable("gui.transcend.stats.universal_tree"), x, y, 0xFFFFCC44, false);
        y += 10;
        drawStatBlock(g, x, col2, y, ascStats);
        y += 52;

        g.fill(ox + 4, y, ox + W - 4, y + 1, C_BORDER);
        y += 4;
        AscensionStatBlock talentStats = computeTreeStats(false);
        g.drawString(font, Component.translatable("gui.transcend.stats.class_tree"), x, y, 0xFF44DDAA, false);
        y += 10;
        drawStatBlock(g, x, col2, y, talentStats);
        y += 52;

        g.fill(ox + 4, y, ox + W - 4, y + 1, C_BORDER);
        y += 4;
        AscensionStatBlock total = data.buildTotalStats();
        g.drawString(font, Component.translatable("gui.transcend.stats.total"), x, y, C_GOLD, false);
        y += 10;
        drawStatBlock(g, x, col2, y, total);
        y += 60;

        if (total.getEffectiveArmorPen() > 0 || total.getEffectiveResistIgnore() > 0
                || total.getEffectiveSpellVamp() > 0 || total.damageReductionFlat > 0) {
            g.fill(ox + 4, y, ox + W - 4, y + 1, C_BORDER);
            y += 4;
            g.drawString(font, Component.translatable("gui.transcend.stats.combat"), x, y, 0xFFFF8844, false);
            y += 10;
            drawStat(g, x, y, "armor_penetration", String.format("%.0f%%", total.getEffectiveArmorPen() * 100));
            drawStat(g, col2, y, "resistance_ignore", String.format("%.0f%%", total.getEffectiveResistIgnore() * 100));
            y += 10;
            drawStat(g, x, y, "spell_vamp", String.format("%.0f%%", total.getEffectiveSpellVamp() * 100));
            drawStat(g, col2, y, "damage_reduction_flat", String.format("%.1f", total.damageReductionFlat));
        }
    }

    private void drawStatBlock(GuiGraphics g, int x, int col2, int y, AscensionStatBlock s) {

        if (s.bonusMaxHealth != 0)
            drawStat(g, x, y, "bonus_max_health", String.format("+%.0f", s.bonusMaxHealth));
        if (s.spellPowerBonus != 0)
            drawStat(g, col2, y, "spell_power_bonus", String.format("+%.1f%%", s.spellPowerBonus * 100));
        y += 10;

        if (s.cooldownReduction != 0)
            drawStat(g, x, y, "cooldown_reduction", String.format("+%.1f%%", s.cooldownReduction * 100));
        if (s.bonusManaCapacity != 0)
            drawStat(g, col2, y, "bonus_mana_capacity", "+" + s.bonusManaCapacity);
        y += 10;

        if (s.moveSpeedBonus != 0)
            drawStat(g, x, y, "move_speed_bonus", String.format("+%.1f%%", s.moveSpeedBonus * 100));
        if (s.critChance != 0)
            drawStat(g, col2, y, "crit_chance", String.format("+%.1f%%", s.critChance * 100));
        y += 10;

        if (s.manaRegenBonus != 0)
            drawStat(g, x, y, "mana_regen_bonus", String.format("+%.2f/s", s.manaRegenBonus));
        if (s.critMultiplier > 1.0f)
            drawStat(g, col2, y, "crit_multiplier", String.format("×%.2f", s.critMultiplier));
        y += 10;

        if (s.reactionBonus != 0)
            drawStat(g, x, y, "reaction_bonus", String.format("+%.1f%%", s.reactionBonus * 100));
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

                entry.getKey().applyTo(block,
                        entry.getValue() * com.huige233.transcend.ascension.tree.TreeRegistry.NODE_STAT_GLOBAL_MULT);
            }
        }
        return block;
    }

    private void drawStat(GuiGraphics g, int x, int y, String statKey, String val) {
        g.drawString(font, Component.translatable("gui.transcend.stat_name." + statKey)
                .withStyle(ChatFormatting.GRAY).append("  ")
                .append(Component.literal(val).withStyle(ChatFormatting.WHITE)), x, y, C_TEXT, false);
    }

    private void drawBottomBar(GuiGraphics g, int ox, int oy) {
        int botY = oy + H - BOT_H;
        g.fill(ox, botY, ox + W, botY + 1, C_BORDER);
        Component hint = Component.translatable("gui.transcend.bottom_progress",
                data.getRitualTier(), data.getInsightLevel(), data.getTalentPoints());
        g.drawCenteredString(font, hint, ox + W / 2, botY + 5, C_GRAY);
    }

    private void drawTooltip(GuiGraphics g, int mx, int my) {
        if (hoveredNode != null) drawNodeTooltip(g, hoveredNode, mx, my);
    }

    private void drawNodeTooltip(GuiGraphics g, NodeDefinition node, int mx, int my) {
        boolean unlocked   = data.isNodeUnlocked(node.getId());
        boolean tierLocked = !data.isTierUnlockedByStage(node.getTier());

        List<Component> lines = new ArrayList<>();
        lines.add(node.getDisplayName());
        lines.add(node.getDescription());

        var stats = node.getStatBonuses();
        if (stats != null && !stats.isEmpty()) {
            lines.add(Component.empty());
            lines.add(Component.translatable("gui.transcend.tooltip.stat_bonuses"));
            for (var entry : stats.entrySet()) {
                lines.add(formatStatLine(entry.getKey(), entry.getValue()));
            }
        }

        var passives = node.getPassiveEffects();
        if (passives != null && !passives.isEmpty()) {
            lines.add(Component.translatable("gui.transcend.tooltip.passive_count", passives.size()));
        }

        var elemScaling = node.getAllElementScaling();
        if (elemScaling != null && !elemScaling.isEmpty()) {
            lines.add(Component.translatable("gui.transcend.tooltip.affinity_count", elemScaling.size()));
        }

        lines.add(Component.empty());

        lines.add(Component.translatable("gui.transcend.tooltip.node_cost",
                        minRealmRankForTier(node.getTier()), node.getCost())
                .withStyle(ChatFormatting.GRAY));

        if (unlocked) {
            lines.add(Component.translatable("gui.transcend.tooltip.unlocked").withStyle(ChatFormatting.GREEN));
        } else if (tierLocked) {
            lines.add(Component.translatable("gui.transcend.tooltip.realm_locked")
                    .withStyle(ChatFormatting.RED));
        } else if (!data.canUnlock(node.getId())) {
            lines.add(Component.translatable("gui.transcend.tooltip.parent_locked")
                    .withStyle(ChatFormatting.RED));
        } else if (data.getTalentPoints() < node.getCost()) {
            lines.add(Component.translatable("gui.transcend.tooltip.points_locked", node.getCost())
                    .withStyle(ChatFormatting.RED));
        } else {
            lines.add(Component.translatable("gui.transcend.tooltip.click_unlock")
                    .withStyle(ChatFormatting.AQUA));
        }

        g.renderComponentTooltip(font, lines, mx, my);
    }

    private static Component formatStatLine(StatType type, float value) {
        Component label = Component.translatable("gui.transcend.stat_name." + type.jsonKey);
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
        return Component.literal("  ").append(label.copy().withStyle(ChatFormatting.GRAY)).append("  ")
                .append(Component.literal(valStr).withStyle(valColor));
    }

    private static boolean isPercentStat(StatType type) {
        return switch (type) {
            case SPELL_POWER_BONUS, COOLDOWN_REDUCTION, MANA_REGEN_BONUS,
                 MOVE_SPEED_BONUS, REACTION_BONUS,
                 CRIT_CHANCE, INCOMING_SPELL_DAMAGE_REDUCTION,
                 ARMOR_PENETRATION, RESISTANCE_IGNORE,
                 SPELL_VAMP, LIFESTEAL, XP_GAIN_MULT, DODGE_CHANCE,
                 DAMAGE_REDUCTION_PERCENT, MANA_COST_REDUCTION -> true;
            case BONUS_MAX_HEALTH, BONUS_MANA_CAPACITY,
                 CRIT_MULTIPLIER, DAMAGE_REDUCTION_FLAT -> false;
        };
    }

    private int minRealmRankForTier(int tier) {
        return switch (tier) {
            case 0, 1 -> 1;
            case 2 -> 3;
            case 3 -> 5;
            case 4 -> 8;
            case 5 -> 11;
            default -> Integer.MAX_VALUE;
        };
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private record NodeButton(NodeDefinition node, Button button, int x, int y) {}

    private static class InvisibleNodeButton extends Button {
        public InvisibleNodeButton(int x, int y, int w, int h, Button.OnPress press) {
            super(x, y, w, h, net.minecraft.network.chat.Component.empty(),
                    press, Button.DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(net.minecraft.client.gui.GuiGraphics g,
                                    int mx, int my, float pt) {

        }
    }
}
