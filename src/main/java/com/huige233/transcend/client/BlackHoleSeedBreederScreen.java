package com.huige233.transcend.client;

import com.huige233.transcend.block.BlackHoleSeedBreederBlockEntity;
import com.huige233.transcend.menu.BlackHoleSeedBreederMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 展示黑洞种子增殖机的物料槽、能量、增殖进度和运行状态，并提供槽位与耗料提示。 */
public class BlackHoleSeedBreederScreen extends AbstractContainerScreen<BlackHoleSeedBreederMenu> {
    private static final String[] SLOT_NAMES = {"input", "matter", "singularity", "container"};

    public BlackHoleSeedBreederScreen(BlackHoleSeedBreederMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 220;
        imageHeight = 216;
    }

    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        MachinePanelStyle.frame(g, x, y, imageWidth, imageHeight, 0xffa376cc);
        MachinePanelStyle.panel(g, x + 8, y + 22, 204, 38);
        MachinePanelStyle.panel(g, x + 8, y + 63, 204, 52);
        MachinePanelStyle.panel(g, x + 22, y + 129, 170, 80);
        arrow(g, x + 56, y + 46, x + 68);
        arrow(g, x + 92, y + 46, x + 128);
        arrow(g, x + 152, y + 46, x + 164);
        for (int i = 0; i < menu.slots.size(); i++) {
            MachinePanelStyle.slot(g, x, y, menu.slots.get(i), i < 2 ? 0xff9872b0 : i < 4 ? 0xffbda067 : 0xff293a50);
        }
        MachinePanelStyle.bar(g, x + 12, y + 78, 196,
                (double) menu.progress() / BlackHoleSeedBreederBlockEntity.CYCLE_TICKS, 0xffa572cb);
        MachinePanelStyle.bar(g, x + 12, y + 99, 196,
                (double) menu.energy() / BlackHoleSeedBreederBlockEntity.ENERGY_CAPACITY, 0xffc59750);
    }

    private void arrow(GuiGraphics g, int x1, int y, int x2) {
        g.hLine(x1, x2 - 1, y, 0xffcbb27d);
        for (int i = 0; i < 3; i++) g.vLine(x2 - i, y - i, y + i, 0xffcbb27d);
    }

    private void label(GuiGraphics g, Component text, int x, int y, int width, int color) {
        MachinePanelStyle.label(g, font, text, x, y, width, color);
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        label(g, title, 10, 9, 180, 0xffe1eaf5);
        label(g, Component.translatable("gui.transcend.breeder_help"), 196, 9, 16, 0xffe0bc80);
        for (int i = 0; i < 4; i++) {
            int sx = menu.slots.get(i).x;
            label(g, Component.translatable("gui.transcend.breeder_slot_" + SLOT_NAMES[i]), sx - 8, 26, i == 1 ? 50 : 35, 0xffb9a4cd);
        }
        label(g, Component.translatable("gui.transcend.breeder_progress", menu.progress(), BlackHoleSeedBreederBlockEntity.CYCLE_TICKS), 12, 67, 196, 0xffc7b6dc);
        label(g, Component.translatable("gui.transcend.breeder_energy", menu.energy()), 12, 87, 196, 0xffdfbf8c);
        label(g, Component.translatable("gui.transcend.breeder_status." + menu.status()), 12, 106, 196, 0xff8ed0b8);
        label(g, playerInventoryTitle, 26, 119, 170, 0xff94a9bf);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        if (hoveredSlot != null && hoveredSlot.index < BlackHoleSeedBreederMenu.MACHINE_SLOTS && !hoveredSlot.hasItem()) {
            g.renderTooltip(font, font.split(Component.translatable("gui.transcend.breeder_slot_hint." + hoveredSlot.index), 220), mouseX, mouseY);
        } else if (isHovering(192, 7, 22, 13, mouseX, mouseY)) {
            g.renderTooltip(font, font.split(Component.translatable("gui.transcend.breeder_help_text"), 220), mouseX, mouseY);
        } else if (isHovering(12, 67, 196, 16, mouseX, mouseY)) {
            g.renderComponentTooltip(font, java.util.List.of(
                    Component.translatable("gui.transcend.breeder_progress", menu.progress(), BlackHoleSeedBreederBlockEntity.CYCLE_TICKS),
                    Component.translatable("gui.transcend.breeder_matter", menu.matterConsumed(), BlackHoleSeedBreederBlockEntity.MATTER_PER_CYCLE)), mouseX, mouseY);
        } else if (isHovering(12, 106, 196, 9, mouseX, mouseY)) {
            g.renderTooltip(font, font.split(Component.translatable("gui.transcend.breeder_status." + menu.status()), 220), mouseX, mouseY);
        }
    }
}
