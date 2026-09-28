package com.huige233.transcend.client;

import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.init.ModBlocks;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.menu.ResearchStationMenu;
import com.huige233.transcend.network.C2SResearchStartPacket;
import com.huige233.transcend.tech.research.ResearchNode;
import com.huige233.transcend.tech.research.ResearchRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Searchable research catalog, explicit prerequisites and server-authoritative controls. */
public final class ResearchStationScreen extends MachineScreen<ResearchStationMenu> {
    private static final int TEXT = 0xffe4e4df, MUTED = 0xffa9b0b3, ACCENT = 0xff76bec7;
    private static final int LIST_X = 14, LIST_Y = 78, ROW_HEIGHT = 24, ROWS = 5;
    private final Set<String> completed = new HashSet<>();
    private final Map<String, ItemStack> icons = new HashMap<>();
    private final List<Hint> hints = new ArrayList<>();
    private final ResearchButton[] entries = new ResearchButton[ROWS];
    private final ResearchButton[] before = new ResearchButton[2], after = new ResearchButton[2];
    private List<ResearchNode> filtered = List.of(), successors = List.of();
    private ResearchBrowser.Branch branch = ResearchBrowser.Branch.ALL;
    private EditBox search;
    private ResearchButton branchButton, stateButton, previous, next, moreLinks, prepare, start, pause;
    private ResearchStationMenu.CompoundState lastState;
    private String query = "", selectedId = "";
    private int stateFilter, scroll, relationOffset;
    private boolean userSelection, snapshotSelected, draggingScroll;

    public ResearchStationScreen(ResearchStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 472;
        imageHeight = 324;
    }

    @Override protected void init() {
        super.init();
        search = new EditBox(font, leftPos + 15, topPos + 35, 136, 16, tr("search"));
        search.setMaxLength(80);
        search.setHint(tr("search"));
        search.setValue(query);
        search.setResponder(value -> { query = value; scroll = 0; userSelection = true; refresh(); });
        addRenderableWidget(search);
        branchButton = button(Component.empty(), 14, 55, 66, 18, b -> {
            branch = ResearchBrowser.Branch.values()[(branch.ordinal() + 1) % ResearchBrowser.Branch.values().length];
            scroll = 0; userSelection = true; refresh();
        });
        stateButton = button(Component.empty(), 84, 55, 68, 18, b -> {
            stateFilter = (stateFilter + 1) % 5;
            scroll = 0; userSelection = true; refresh();
        });
        for (int i = 0; i < ROWS; i++) {
            final int row = i;
            entries[i] = button(Component.empty(), LIST_X, LIST_Y + i * ROW_HEIGHT, 132, 23,
                    b -> { if (scroll + row < filtered.size()) select(filtered.get(scroll + row).id(), false); });
        }
        previous = button(Component.literal("<"), 112, 203, 18, 14, b -> scroll(-ROWS));
        next = button(Component.literal(">"), 134, 203, 18, 14, b -> scroll(ROWS));
        for (int i = 0; i < 2; i++) {
            before[i] = button(Component.empty(), 172, 111 + i * 21, 87, 18,
                    b -> select(((ResearchButton) b).node.id(), true));
            after[i] = button(Component.empty(), 369, 111 + i * 21, 87, 18,
                    b -> select(((ResearchButton) b).node.id(), true));
        }
        moreLinks = button(Component.literal(">"), 442, 98, 14, 11, b -> {
            relationOffset = relationOffset + 2 >= successors.size() ? 0 : relationOffset + 2;
            refreshRelations();
        });
        moreLinks.setTooltip(Tooltip.create(tr("more_links")));
        prepare = button(tr("prepare"), 68, 270, 118, 20, b -> sendPrepare());
        start = button(tr("start"), 166, 202, 210, 18, b -> { if (selection() != null) send(selectedId); });
        pause = button(tr("pause"), 382, 202, 82, 18, b -> send(""));
        prepare.setTooltip(Tooltip.create(tr("prepare_help")));
        pause.setTooltip(Tooltip.create(tr("pause_help")));
        previous.setTooltip(Tooltip.create(tr("previous_page")));
        next.setTooltip(Tooltip.create(tr("next_page")));
        refresh();
    }

    private ResearchButton button(Component text, int x, int y, int width, int height, Button.OnPress press) {
        return addRenderableWidget(new ResearchButton(leftPos + x, topPos + y, width, height, text, press));
    }

    private ResearchNode selection() { return ResearchRegistry.get(selectedId); }
    private String activeId() { return menu.clientState().progress().getString("Active"); }
    private boolean active(ResearchNode node) { return node.id().equals(activeId()); }

    private boolean matchesState(ResearchNode node) {
        return switch (stateFilter) {
            case 1 -> menu.isAvailable(node.id());
            case 2 -> active(node);
            case 3 -> completed.contains(node.id());
            case 4 -> !active(node) && !completed.contains(node.id()) && !menu.isAvailable(node.id());
            default -> true;
        };
    }

    private void refresh() {
        boolean revealActive = false;
        completed.clear();
        var done = menu.clientState().progress().getList("Completed", 8);
        for (int i = 0; i < done.size(); i++) completed.add(done.getString(i));
        if (!snapshotSelected && menu.hasSnapshot()) {
            snapshotSelected = true;
            if (!userSelection && !activeId().isEmpty()) { selectedId = activeId(); revealActive = true; }
        }
        filtered = ResearchBrowser.filter(menu.nodes(), query, branch, this::matchesState, id -> nodeName(id).getString());
        if (selection() == null || !filtered.contains(selection())) selectedId = filtered.isEmpty() ? "" : filtered.get(0).id();
        if (revealActive) scroll = Math.max(0, filtered.indexOf(selection()));
        scroll = Math.max(0, Math.min(scroll, Math.max(0, filtered.size() - ROWS)));
        branchButton.setMessage(tr("branch." + branch.name().toLowerCase(Locale.ROOT)));
        stateButton.setMessage(tr("filter." + stateFilter));
        branchButton.hint(tr("branch_help", branchButton.getMessage()));
        stateButton.hint(tr("filter_help", stateButton.getMessage()));
        for (int i = 0; i < ROWS; i++) {
            ResearchButton button = entries[i];
            button.visible = scroll + i < filtered.size();
            if (!button.visible) continue;
            ResearchNode node = filtered.get(scroll + i);
            button.node = node;
            button.catalog = true;
            button.selected = node.id().equals(selectedId);
            button.setMessage(nodeName(node.id()));
            button.hint(nodeTooltip(node));
        }
        previous.active = scroll > 0;
        next.active = scroll + ROWS < filtered.size();
        prepare.active = menu.canPrepare();
        start.active = selection() != null && menu.canStart(selectedId);
        start.setMessage(tr(selection() != null && active(selection()) ? "resume" : "start"));
        start.hint(selection() == null ? tr("start_help") : actionReason(selection()));
        pause.active = menu.canPause();
        refreshRelations();
        lastState = menu.clientState();
    }

    private void refreshRelations() {
        ResearchNode node = selection();
        successors = node == null ? List.of() : ResearchBrowser.successors(menu.nodes(), selectedId);
        relationOffset = Math.max(0, Math.min(relationOffset, Math.max(0, successors.size() - 1)));
        for (int i = 0; i < 2; i++) {
            bindLink(before[i], node != null && i < node.prerequisites().size() ? ResearchRegistry.get(node.prerequisites().get(i)) : null);
            bindLink(after[i], i + relationOffset < successors.size() ? successors.get(i + relationOffset) : null);
        }
        moreLinks.visible = successors.size() > 2;
    }

    private void bindLink(ResearchButton button, ResearchNode node) {
        button.node = node;
        button.visible = node != null;
        if (node != null) { button.setMessage(nodeName(node.id())); button.hint(nodeTooltip(node)); }
    }

    private void select(String id, boolean reveal) {
        selectedId = id;
        userSelection = true;
        relationOffset = 0;
        if (reveal) {
            branch = ResearchBrowser.Branch.ALL;
            stateFilter = 0;
            query = "";
            search.setValue("");
        }
        refresh();
        if (reveal) {
            int index = filtered.indexOf(selection());
            if (index >= 0) { scroll = Math.min(index, Math.max(0, filtered.size() - ROWS)); refresh(); }
        }
    }

    private void scroll(int delta) {
        scroll = Math.max(0, Math.min(Math.max(0, filtered.size() - ROWS), scroll + delta));
        refresh();
    }

    @Override protected boolean scrollContent(double x, double y, double delta) {
        if (delta != 0 && isHovering(8, 76, 150, 144, x, y)) { scroll(delta > 0 ? -1 : 1); return true; }
        return super.scrollContent(x, y, delta);
    }

    private void dragScroll(double y) {
        int track = ROWS * ROW_HEIGHT - 2;
        int thumb = Math.max(12, track * ROWS / filtered.size());
        scroll = Math.max(0, Math.min(filtered.size() - ROWS,
                (int) Math.round((y - topPos - LIST_Y - thumb / 2d) / (track - thumb) * (filtered.size() - ROWS))));
        refresh();
    }

    @Override protected boolean clickContent(double x, double y, int button) {
        if (button == 0 && filtered.size() > ROWS && isHovering(147, LIST_Y, 8, ROWS * ROW_HEIGHT, x, y)) {
            draggingScroll = true;
            dragScroll(y);
            return true;
        }
        return super.clickContent(x, y, button);
    }

    @Override protected boolean dragContent(double x, double y, int button, double dx, double dy) {
        if (draggingScroll && button == 0 && filtered.size() > ROWS) { dragScroll(y); return true; }
        return super.dragContent(x, y, button, dx, dy);
    }

    @Override protected boolean releaseContent(double x, double y, int button) {
        if (draggingScroll && button == 0) { draggingScroll = false; return true; }
        return super.releaseContent(x, y, button);
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (search.isFocused() && key != 258) {
            if (key == 256) { setFocused(null); search.setFocused(false); return true; }
            return search.keyPressed(key, scanCode, modifiers);
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override protected void containerTick() {
        super.containerTick();
        search.tick();
        if (lastState != menu.clientState()) refresh();
        // Inventory updates and research snapshots are separate packets.
        prepare.active = menu.canPrepare();
    }

    private void sendPrepare() {
        if (menu.station() != null) NetworkHandler.CHANNEL.sendToServer(
                C2SResearchStartPacket.prepare(menu.station().getBlockPos(), menu.containerId));
    }

    private void send(String id) {
        if (menu.station() != null) NetworkHandler.CHANNEL.sendToServer(
                new C2SResearchStartPacket(menu.station().getBlockPos(), menu.containerId, id));
    }

    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        bevel(g, x, y, imageWidth, imageHeight, 0xff999b99);
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + 23, 0xff393d40);
        inset(g, 8, 28, 150, 192);
        inset(g, 166, 28, 298, 168);
        inset(g, 8, 226, 270, 90);
        inset(g, 284, 226, 180, 90);
        for (var slot : menu.slots) {
            int sx = x + slot.x - 1, sy = y + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, 0xff777777);
            g.hLine(sx, sx + 17, sy, 0xff303030);
            g.vLine(sx, sy, sy + 17, 0xff303030);
            g.hLine(sx, sx + 17, sy + 17, 0xffd4d4d4);
            g.vLine(sx + 17, sy, sy + 17, 0xffd4d4d4);
        }
        if (filtered.size() > ROWS) {
            int track = ROWS * ROW_HEIGHT - 2;
            int thumb = Math.max(12, track * ROWS / filtered.size());
            int offset = (track - thumb) * scroll / (filtered.size() - ROWS);
            g.fill(x + 149, y + LIST_Y, x + 153, y + LIST_Y + track, 0xff15191b);
            g.fill(x + 150, y + LIST_Y + offset, x + 153, y + LIST_Y + offset + thumb, 0xff969c9d);
        }
        ResearchNode node = selection();
        if (node != null) {
            for (int i = 0; i < 2; i++) {
                int cy = y + 120 + 21 * i;
                if (before[i].visible) { g.hLine(x + 259, x + 269, cy, color(before[i].node)); g.vLine(x + 269, Math.min(cy, y + 130), Math.max(cy, y + 130), color(before[i].node)); }
                if (after[i].visible) { g.hLine(x + 359, x + 369, cy, color(after[i].node)); g.vLine(x + 359, Math.min(cy, y + 130), Math.max(cy, y + 130), color(after[i].node)); }
            }
            g.hLine(x + 269, x + 359, y + 130, ACCENT);
            bevel(g, x + 275, y + 118, 78, 25, 0xff43575a);
        }
    }

    private void inset(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(leftPos + x, topPos + y, leftPos + x + w, topPos + y + h, 0xff22272a);
        g.hLine(leftPos + x, leftPos + x + w - 1, topPos + y, 0xff303030);
        g.vLine(leftPos + x, topPos + y, topPos + y + h - 1, 0xff303030);
        g.hLine(leftPos + x, leftPos + x + w - 1, topPos + y + h - 1, 0xffdadada);
        g.vLine(leftPos + x + w - 1, topPos + y, topPos + y + h - 1, 0xffdadada);
    }

    private static void bevel(GuiGraphics g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + h, 0xff171717);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
        g.hLine(x + 1, x + w - 2, y + 1, 0xffdddddd);
        g.vLine(x + 1, y + 1, y + h - 2, 0xffdddddd);
        g.hLine(x + 1, x + w - 2, y + h - 2, 0xff505353);
        g.vLine(x + w - 2, y + 1, y + h - 2, 0xff505353);
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        hints.clear();
        label(g, title, 12, 9, 250, TEXT);
        label(g, tr("catalog_count", completed.size(), menu.nodes().size()), 304, 9, 156, MUTED);
        label(g, tr("results", filtered.size()), 14, 207, 94, MUTED);
        if (filtered.isEmpty()) label(g, tr("no_results"), 20, 126, 124, MUTED);
        var state = menu.clientState();
        hintLabel(g, tr("energy", compact(Long.toString(state.energy())) + " RF"), tr("energy", state.energy() + " RF"), 14, 234, 130, TEXT);
        hintLabel(g, tr("points", compact(state.points())), tr("points", state.points()), 150, 234, 122, ACCENT);
        hintLabel(g, tr("rate", compact(Long.toString(state.rate())) + " RF"), tr("rate", state.rate() + " RF"), 14, 248, 256, MUTED);
        label(g, tr("inputs"), 14, 261, 170, MUTED);
        label(g, tr("plugins"), 194, 261, 76, MUTED);
        label(g, playerInventoryTitle, 292, 229, 162, MUTED);
        ResearchNode node = selection();
        label(g, node == null ? tr("start_help") : actionReason(node), 14, 302, 256, node != null && menu.canStart(node.id()) ? ACCENT : MUTED);
        if (node == null) { label(g, tr("select_node"), 174, 68, 282, MUTED); return; }
        g.renderItem(icon(node), 174, 34);
        label(g, nodeName(node.id()), 198, 35, 218, TEXT);
        label(g, Component.literal(node.tier().name()), 434, 35, 22, ACCENT);
        label(g, menu.status(node.id()), 174, 52, 136, color(node));
        label(g, tr("branch." + ResearchBrowser.branch(node).name().toLowerCase(Locale.ROOT)), 322, 52, 134, MUTED);
        Component description = Component.translatable("research.transcend.description." + node.id());
        var lines = font.split(description, 282);
        for (int i = 0; i < Math.min(3, lines.size()); i++) g.drawString(font, lines.get(i), 174, 66 + i * 9, MUTED, false);
        hints.add(new Hint(174, 65, 282, 29, description));
        label(g, tr("requires_count", node.prerequisites().size()), 172, 100, 95, MUTED);
        label(g, tr("leads_to", successors.size()), 369, 100, 70, MUTED);
        if (node.prerequisites().isEmpty()) label(g, tr("root_node"), 174, 126, 85, MUTED);
        if (successors.isEmpty()) label(g, tr("end_node"), 371, 126, 83, MUTED);
        g.renderItem(icon(node), 281, 122);
        label(g, Component.literal(node.tier().name()), 307, 126, 36, TEXT);
        hints.add(new Hint(275, 118, 78, 25, nodeTooltip(node)));
        BigInteger paid = paid(node);
        Component fullPaid = tr("paid", paid.toString(), node.cost().toString());
        hintLabel(g, tr("paid", compact(paid.toString()), compact(node.cost().toString())), fullPaid, 174, 153, 282, TEXT);
        MachinePanelStyle.bar(g, 174, 164, 282, ratio(paid, node.cost()), ACCENT);
        long ticks = completed.contains(node.id()) ? node.durationTicks() : active(node)
                ? Math.max(0, Math.min(node.durationTicks(), state.progress().getLong("Ticks"))) : 0;
        Component duration = tr("time_progress", duration(ticks), duration(node.durationTicks()));
        hintLabel(g, duration, tr("progress", ticks, node.durationTicks()), 174, 175, 282, TEXT);
        MachinePanelStyle.bar(g, 174, 187, 282, (double) ticks / node.durationTicks(), 0xff94b879);
        hints.add(new Hint(174, 164, 282, 5, fullPaid));
        hints.add(new Hint(174, 187, 282, 5, tr("progress", ticks, node.durationTicks())));
    }

    private Component actionReason(ResearchNode node) {
        if (!menu.hasSnapshot()) return tr("syncing");
        if (completed.contains(node.id())) return tr("completed");
        if (menu.canStart(node.id())) return tr(active(node) ? "resume_help" : "ready");
        if (active(node) && menu.canPause()) return tr("in_progress");
        if (menu.clientState().researcher() != null) return tr("action.busy");
        if (!activeId().isEmpty() && !active(node)) return tr("finish_active");
        long earlier = menu.nodes().stream().filter(n -> n.tier().index() < node.tier().index() && !completed.contains(n.id())).count();
        if (earlier > 0) return tr("earlier_required", earlier);
        if (!completed.containsAll(node.prerequisites())) return tr("missing_prerequisites");
        return tr("need_points");
    }

    private Component nodeTooltip(ResearchNode node) {
        return nodeName(node.id()).copy().append(" [" + node.tier().name() + "]\n")
                .append(menu.status(node.id())).append("\n").append(tr("cost", node.cost().toString()))
                .append("\n").append(tr("time_required", duration(node.durationTicks())))
                .append("\n").append(actionReason(node));
    }

    private int color(ResearchNode node) {
        return active(node) ? 0xffd7b77d : completed.contains(node.id()) ? 0xff91bb7a
                : menu.isAvailable(node.id()) ? ACCENT : 0xff8b9396;
    }

    private ItemStack icon(ResearchNode node) {
        return icons.computeIfAbsent(node.id(), id -> new ItemStack(switch (ResearchBrowser.branch(node)) {
            case MATERIALS -> node.tier().index() >= 3 ? ModItems.mini_singularity.get() : ModItems.tech_part_gear.get();
            case AUTOMATION -> ModBlocks.ASSEMBLY.get().asItem();
            case COMBAT -> ModItems.particle_gun.get();
            case KNOWLEDGE -> (node.tier().index() >= 4 ? ModBlocks.RESEARCH_COSMIC_SIMULATOR : node.tier().index() >= 2
                    ? ModBlocks.RESEARCH_QUANTUM_COMPUTER : ModBlocks.RESEARCH_PROCESSOR).get().asItem();
            default -> (node.tier().index() >= 3 ? ModBlocks.MINI_UNIVERSE_GENERATOR : ModBlocks.LONG_STORAGE_BASIC).get().asItem();
        }));
    }

    private void label(GuiGraphics g, Component text, int x, int y, int width, int color) {
        drawLabel(g, text, x, y, width, color);
    }

    private void hintLabel(GuiGraphics g, Component text, Component full, int x, int y, int width, int color) {
        label(g, text, x, y, width, color);
        hints.add(new Hint(x, y, width, font.lineHeight, full));
    }

    @Override protected boolean renderHints(GuiGraphics g, int mouseX, int mouseY, int tooltipX, int tooltipY) {
        if (hoveredSlot != null && hoveredSlot.index < 6) {
            Component hint = hoveredSlot.index < 2 ? Component.translatable(hoveredSlot.index == 0
                    ? "item.transcend.blank_research_component" : "item.transcend.research_assist_unit") : tr("plugins");
            g.renderTooltip(font, hint, tooltipX, tooltipY);
            return true;
        }
        for (Hint hint : hints) if (isHovering(hint.x(), hint.y(), hint.width(), hint.height(), mouseX, mouseY)) {
            g.renderTooltip(font, font.split(hint.text(), Math.min(300, g.guiWidth() - 16)), tooltipX, tooltipY);
            return true;
        }
        return false;
    }

    private BigInteger paid(ResearchNode node) {
        if (completed.contains(node.id())) return node.cost();
        if (!active(node)) return BigInteger.ZERO;
        try { return new BigInteger(menu.clientState().progress().getString("Paid")).max(BigInteger.ZERO).min(node.cost()); }
        catch (NumberFormatException ignored) { return BigInteger.ZERO; }
    }

    private static double ratio(BigInteger value, BigInteger total) {
        return total.signum() == 0 ? 1 : value.multiply(BigInteger.valueOf(10_000)).divide(total).doubleValue() / 10_000;
    }
    private static String compact(String value) {
        try { String digits = new BigInteger(value).toString(); return digits.length() <= 7 ? digits
                : digits.charAt(0) + "." + digits.substring(1, 3) + "e" + (digits.length() - 1); }
        catch (NumberFormatException ignored) { return value; }
    }
    private static String duration(long ticks) {
        long seconds = (ticks + 19) / 20;
        return seconds >= 3600 ? String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
                : String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
    private static Component tr(String key, Object... args) { return Component.translatable("gui.transcend.research." + key, args); }
    private static Component nodeName(String id) { return Component.translatable("research.transcend." + id); }
    private record Hint(int x, int y, int width, int height, Component text) { }

    private final class ResearchButton extends Button {
        private ResearchNode node;
        private boolean catalog, selected;
        private Component lastHint;
        ResearchButton(int x, int y, int w, int h, Component text, OnPress press) { super(x, y, w, h, text, press, DEFAULT_NARRATION); }
        void hint(Component value) { if (!value.equals(lastHint)) { lastHint = value; setTooltip(Tooltip.create(value)); } }

        @Override protected net.minecraft.network.chat.MutableComponent createNarrationMessage() {
            return node == null ? super.createNarrationMessage()
                    : nodeName(node.id()).copy().append(". ").append(menu.status(node.id()));
        }

        @Override public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            int fill = !active ? 0xff424647 : selected ? 0xff4b686d : isHoveredOrFocused() ? 0xff707c7e : 0xff5b6163;
            bevel(g, x, y, w, h, fill);
            if (isFocused() || selected) g.renderOutline(x, y, w, h, selected ? ACCENT : 0xffeeeeee);
            if (catalog && node != null) {
                g.fill(x + 2, y + 2, x + 4, y + h - 2, color(node));
                g.renderItem(icon(node), x + 7, y + 4);
                MachinePanelStyle.label(g, font, getMessage(), x + 27, y + 3, w - 32, TEXT);
                MachinePanelStyle.label(g, font, Component.literal(node.tier().name() + " · ").append(menu.status(node.id())), x + 27, y + 13, w - 32, color(node));
            } else {
                var label = MachinePanelStyle.clipped(font, getMessage(), w - 8);
                g.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - font.lineHeight) / 2,
                        active ? TEXT : 0xff999d9e, false);
            }
        }
    }
}
