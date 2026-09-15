package com.huige233.transcend.client;

import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.menu.ResearchStationMenu;
import com.huige233.transcend.network.C2SResearchStartPacket;
import com.huige233.transcend.tech.research.ResearchNode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 展示研究资源、分页节点及前置条件和进度，并提供组件准备、研究启动、恢复与暂停操作。 */
public final class ResearchStationScreen extends AbstractContainerScreen<ResearchStationMenu> {
    private static final int BACKGROUND = 0xff0b1020, PANEL = 0xff121b30, BORDER = 0xff354365;
    private static final int TEXT = 0xffedf0ff, MUTED = 0xffb5bfd8, ACCENT = 0xff9580e8;
    private static final int DETAIL_X = 192, DETAIL_WIDTH = 212;
    private final PanelButton[] nodes = new PanelButton[9];
    private final Set<String> completed = new HashSet<>();
    private final List<TextHint> textHints = new ArrayList<>();
    private PanelButton previous, next, prepare, start, pause;
    private int page, selected = -1;

    public ResearchStationScreen(ResearchStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 420;
        imageHeight = 272;
    }

    @Override
    protected void init() {
        super.init();
        previous = button(Component.translatable("gui.transcend.research.previous"), 370, 27, 18, 17,
                b -> { page--; refresh(); });
        next = button(Component.translatable("gui.transcend.research.next"), 390, 27, 18, 17,
                b -> { page++; refresh(); });
        for (int i = 0; i < nodes.length; i++) {
            final int index = i;
            nodes[i] = button(Component.empty(), 192 + i % 3 * 72, 48 + i / 3 * 25, 68, 23,
                    b -> { selected = page * nodes.length + index; refresh(); });
        }
        prepare = button(Component.translatable("gui.transcend.research.prepare"), 66, 101, 106, 20,
                b -> sendPrepare());
        start = button(Component.translatable("gui.transcend.research.start"), 192, 242, 132, 20,
                b -> { if (validSelection()) send(menu.nodes().get(selected).id()); });
        pause = button(Component.translatable("gui.transcend.research.pause"), 330, 242, 74, 20,
                b -> send(""));
        prepare.setTooltip(Tooltip.create(Component.translatable("gui.transcend.research.prepare_help")));
        start.setTooltip(Tooltip.create(Component.translatable("gui.transcend.research.start_help")));
        pause.setTooltip(Tooltip.create(Component.translatable("gui.transcend.research.pause_help")));
        refresh();
    }

    private PanelButton button(Component text, int x, int y, int width, int height, Button.OnPress action) {
        return addRenderableWidget(new PanelButton(leftPos + x, topPos + y, width, height, text, action));
    }

    private boolean validSelection() {
        return selected >= 0 && selected < menu.nodes().size();
    }

    private String activeId() {
        return menu.clientState().progress().getString("Active");
    }

    private void refresh() {
        completed.clear();
        var done = menu.clientState().progress().getList("Completed", 8);
        for (int i = 0; i < done.size(); i++) completed.add(done.getString(i));
        
        if (!validSelection() && !activeId().isEmpty()) {
            for (int i = 0; i < menu.nodes().size(); i++) {
                if (menu.nodes().get(i).id().equals(activeId())) {
                    selected = i;
                    page = i / nodes.length;
                    break;
                }
            }
        }
        page = Math.max(0, Math.min(page, menu.pageCount() - 1));
        previous.active = page > 0;
        next.active = page + 1 < menu.pageCount();
        for (int i = 0; i < nodes.length; i++) {
            int index = page * nodes.length + i;
            PanelButton button = nodes[i];
            button.visible = index < menu.nodes().size();
            if (!button.visible) continue;
            ResearchNode node = menu.nodes().get(index);
            button.setMessage(nodeName(node.id()));
            button.secondary = menu.status(node.id());
            button.selected = selected == index;
            button.stripe = node.id().equals(activeId()) ? ACCENT
                    : completed.contains(node.id()) ? 0xff79cdb8
                    : menu.isAvailable(node.id()) ? 0xff79bde8 : 0xff63718e;
            
            button.setTooltip(Tooltip.create(nodeTooltip(node)));
        }
        prepare.active = menu.canPrepare();
        start.active = validSelection() && menu.canStart(menu.nodes().get(selected).id());
        boolean resume = validSelection() && menu.nodes().get(selected).id().equals(activeId());
        start.setMessage(Component.translatable(resume ? "gui.transcend.research.resume" : "gui.transcend.research.start"));
        pause.active = menu.canPause();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refresh();
    }

    private void sendPrepare() {
        if (menu.station() != null)
            NetworkHandler.CHANNEL.sendToServer(C2SResearchStartPacket.prepare(menu.station().getBlockPos(), menu.containerId));
    }

    private void send(String id) {
        if (menu.station() != null)
            NetworkHandler.CHANNEL.sendToServer(new C2SResearchStartPacket(menu.station().getBlockPos(), menu.containerId, id));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, BACKGROUND);
        graphics.renderOutline(leftPos, topPos, imageWidth, imageHeight, BORDER);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + 21, 0xff1e2340);
        graphics.fill(leftPos + 8, topPos + 20, leftPos + 62, topPos + 21, ACCENT);
        panel(graphics, 8, 25, 172, 59);
        panel(graphics, 8, 87, 172, 77);
        panel(graphics, 8, 168, 172, 96);
        panel(graphics, 186, 25, 226, 99);
        panel(graphics, 186, 128, 226, 108);
        
        for (Slot slot : menu.slots) {
            int x = leftPos + slot.x, y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, BORDER);
            graphics.fill(x, y, x + 16, y + 16, 0xff080e1b);
            graphics.fill(x, y + 15, x + 16, y + 16, 0xff26334c);
        }
    }

    private void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + height, PANEL);
        graphics.renderOutline(leftPos + x, topPos + y, width, height, BORDER);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        textHints.clear();
        label(graphics, title, 12, 7, 396, TEXT);
        label(graphics, tr("resources"), 16, 30, 156, TEXT);
        var state = menu.clientState();
        numberLabel(graphics, "energy", Long.toString(state.energy()), " RF", 43);
        numberLabel(graphics, "rate", Long.toString(state.rate()), " RF", 56);
        numberLabel(graphics, "points", state.points(), "", 69);
        label(graphics, tr("inputs"), 16, 91, 156, MUTED);
        label(graphics, tr("plugins"), 16, 146, 72, MUTED);
        label(graphics, playerInventoryTitle, 16, 172, 156, MUTED);
        label(graphics, tr("tree"), DETAIL_X, 31, 94, TEXT);
        label(graphics, tr("page", page + 1, menu.pageCount()), 288, 31, 78, MUTED);
        label(graphics, tr("details"), DETAIL_X, 133, DETAIL_WIDTH, MUTED);
        if (!validSelection()) {
            label(graphics, tr("start_help"), DETAIL_X, 151, DETAIL_WIDTH, TEXT);
            return;
        }
        ResearchNode node = menu.nodes().get(selected);
        label(graphics, nodeName(node.id()), DETAIL_X, 145, DETAIL_WIDTH, TEXT);
        label(graphics, tr("status", menu.status(node.id())), DETAIL_X, 158, DETAIL_WIDTH, MUTED);
        label(graphics, prerequisites(node), DETAIL_X, 171, DETAIL_WIDTH, MUTED);
        BigInteger paid = paid(node);
        Component fullPaid = tr("paid", paid.toString(), node.cost().toString());
        label(graphics, tr("paid", compact(paid.toString()), compact(node.cost().toString())), fullPaid,
                DETAIL_X, 184, DETAIL_WIDTH, TEXT);
        progressBar(graphics, 196, ratio(paid, node.cost()), fullPaid);
        long ticks = completed.contains(node.id()) ? node.durationTicks()
                : node.id().equals(activeId()) ? Math.max(0, Math.min(node.durationTicks(), state.progress().getLong("Ticks"))) : 0;
        Component time = tr("progress", ticks, node.durationTicks());
        Component fullTime = time.copy().append("\n").append(tr("duration", node.durationTicks()));
        label(graphics, time, fullTime, DETAIL_X, 205, DETAIL_WIDTH, TEXT);
        progressBar(graphics, 218, (double) ticks / node.durationTicks(), fullTime);
        label(graphics, tr("duration", node.durationTicks()), DETAIL_X, 225, DETAIL_WIDTH, MUTED);
    }

    private void numberLabel(GuiGraphics graphics, String key, String value, String suffix, int y) {
        label(graphics, tr(key, compact(value) + suffix), tr(key, value + suffix), 16, y, 156, MUTED);
    }

    private void label(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        label(graphics, text, text, x, y, width, color);
    }

    private void label(GuiGraphics graphics, Component text, Component fullText, int x, int y, int width, int color) {
        graphics.drawString(font, clipped(text, width), x, y, color, false);
        textHints.add(new TextHint(x, y, width, font.lineHeight, fullText));
    }

    private String clipped(Component text, int width) {
        String value = text.getString();
        return font.width(value) <= width ? value : font.plainSubstrByWidth(value, Math.max(0, width - font.width("..."))) + "...";
    }

    private void progressBar(GuiGraphics graphics, int y, double fraction, Component tooltip) {
        graphics.fill(DETAIL_X, y, DETAIL_X + DETAIL_WIDTH, y + 4, 0xff080e1b);
        int filled = (int) (DETAIL_WIDTH * Math.max(0, Math.min(1, fraction)));
        if (filled > 0) graphics.fill(DETAIL_X, y, DETAIL_X + filled, y + 4, ACCENT);
        textHints.add(new TextHint(DETAIL_X, y - 1, DETAIL_WIDTH, 6, tooltip));
    }

    private BigInteger paid(ResearchNode node) {
        if (completed.contains(node.id())) return node.cost();
        if (!node.id().equals(activeId())) return BigInteger.ZERO;
        try {
            return new BigInteger(menu.clientState().progress().getString("Paid")).max(BigInteger.ZERO).min(node.cost());
        } catch (NumberFormatException ignored) {
            return BigInteger.ZERO;
        }
    }

    private static double ratio(BigInteger value, BigInteger total) {
        return total.signum() == 0 ? 1 : value.multiply(BigInteger.valueOf(10_000)).divide(total).doubleValue() / 10_000;
    }

    private Component prerequisites(ResearchNode node) {
        MutableComponent names = Component.empty();
        for (String id : node.prerequisites()) {
            if (!names.getSiblings().isEmpty()) names.append(", ");
            names.append(nodeName(id)).append(" [").append(tr(completed.contains(id) ? "completed" : "locked")).append("]");
        }
        if (node.prerequisites().isEmpty()) names.append("—");
        return tr("prerequisites", names);
    }

    private Component nodeTooltip(ResearchNode node) {
        return nodeName(node.id()).copy().append(" [" + node.tier().name() + "]\n")
                .append(tr("status", menu.status(node.id()))).append("\n")
                .append(tr("cost", node.cost().toString())).append("\n")
                .append(tr("duration", node.durationTicks())).append("\n")
                .append(prerequisites(node));
    }

    private static Component nodeName(String id) {
        return Component.translatable("research.transcend." + id);
    }

    private static Component tr(String key, Object... arguments) {
        return Component.translatable("gui.transcend.research." + key, arguments);
    }

    private static String compact(String value) {
        try {
            String digits = new BigInteger(value).toString();
            if (digits.length() > 9) return digits.charAt(0) + "." + digits.substring(1, 4) + "e" + (digits.length() - 1);
            return digits;
        } catch (NumberFormatException ignored) {
            return value;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < 6) {
            Component hint = hoveredSlot.index < 2
                    ? Component.translatable(hoveredSlot.index == 0 ? "item.transcend.blank_research_component" : "item.transcend.research_assist_unit")
                    : tr("plugins");
            graphics.renderComponentTooltip(font, List.of(hint), mouseX, mouseY);
        }
        for (TextHint hint : textHints) {
            if (hint.contains(mouseX - leftPos, mouseY - topPos)) {
                graphics.renderComponentTooltip(font, List.of(hint.text()), mouseX, mouseY);
                break;
            }
        }
    }

    /** 保存研究界面文字或进度条的悬停区域及完整提示内容，并判断鼠标是否命中。 */
    private record TextHint(int x, int y, int width, int height, Component text) {
        boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    
    /** 绘制研究界面的限宽按钮文字、节点状态和选中高亮，同时保留原生按钮交互与无障碍提示。 */
    private final class PanelButton extends Button {
        private Component secondary;
        private boolean selected;
        private int stripe = BORDER;

        private PanelButton(int x, int y, int width, int height, Component text, OnPress action) {
            super(x, y, width, height, text, action, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX(), y = getY();
            graphics.fill(x, y, x + getWidth(), y + getHeight(), selected ? 0xff302a50
                    : active && isHoveredOrFocused() ? 0xff26334f : 0xff19243c);
            graphics.renderOutline(x, y, getWidth(), getHeight(), selected || isFocused() ? ACCENT
                    : active && isHovered() ? MUTED : BORDER);
            if (secondary != null) {
                graphics.fill(x + 1, y + 2, x + 3, y + getHeight() - 2, stripe);
                graphics.drawString(font, clipped(getMessage(), getWidth() - 10), x + 6, y + 3, TEXT, false);
                Component status = selected ? Component.literal("> ").append(secondary) : secondary;
                graphics.drawString(font, clipped(status, getWidth() - 10), x + 6, y + 13, MUTED, false);
            } else {
                String text = clipped(getMessage(), getWidth() - 8);
                graphics.drawString(font, text, x + (getWidth() - font.width(text)) / 2,
                        y + (getHeight() - font.lineHeight) / 2, active ? TEXT : 0xff8590aa, false);
            }
        }
    }
}
