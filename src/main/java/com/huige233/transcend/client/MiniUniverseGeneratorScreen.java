package com.huige233.transcend.client;

import com.huige233.transcend.block.MiniUniverseGeneratorBlockEntity;
import com.huige233.transcend.network.C2SMiniUniverseOverclockPacket;
import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.menu.MiniUniverseGeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.math.BigInteger;
import java.util.Locale;

/** 展示微型宇宙发电机的输入输出缓冲、启动充能和剩余储量，并提供超频切换操作。 */
public class MiniUniverseGeneratorScreen extends AbstractContainerScreen<MiniUniverseGeneratorMenu> {
    private Button overclockButton;

    public MiniUniverseGeneratorScreen(MiniUniverseGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = MiniUniverseGeneratorMenu.WIDTH;
        imageHeight = MiniUniverseGeneratorMenu.HEIGHT;
    }

    @Override protected void init() {
        super.init();
        overclockButton = addRenderableWidget(Button.builder(overclockLabel(), button ->
                NetworkHandler.CHANNEL.sendToServer(new C2SMiniUniverseOverclockPacket(menu.machinePos())))
                .bounds(leftPos + 110, topPos + 25, 120, 18).build());
        overclockButton.setTooltip(Tooltip.create(Component.translatable("gui.transcend.universe.overclock_help")));
    }

    private Component overclockLabel() {
        return Component.translatable(menu.overclocked()
                ? "gui.transcend.universe.overclock_on" : "gui.transcend.universe.overclock");
    }

    @Override protected void containerTick() {
        super.containerTick();
        if (overclockButton != null) overclockButton.setMessage(overclockLabel());
    }

    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        MachinePanelStyle.frame(g, x, y, imageWidth, imageHeight, 0xff9876ed);
        MachinePanelStyle.panel(g, x + 7, y + 23, 226, 48);
        MachinePanelStyle.panel(g, x + 7, y + 74, 226, 61);
        MachinePanelStyle.panel(g, x + 33, y + 147, 174, 80);
        g.hLine(x + 60, x + 131, y + 58, 0xff81bfe6);
        for (int i = 0; i < 4; i++) g.vLine(x + 131 - i, y + 58 - i, y + 58 + i, 0xff81bfe6);
        for (int i = 0; i < menu.slots.size(); i++) {
            MachinePanelStyle.slot(g, x, y, menu.slots.get(i), i == 0 ? 0xffa078dc : i == 1 ? 0xff66b8d9 : 0xff293a50);
        }
        MachinePanelStyle.bar(g, x + 12, y + 92, 216, (double) menu.energy() / menu.capacity(), 0xff4faaca);
    }

    private void label(GuiGraphics g, Component text, int x, int y, int width, int color) {
        MachinePanelStyle.label(g, font, text, x, y, width, color);
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        label(g, title, 10, 9, 220, 0xffe1eaf5);
        label(g, Component.translatable("gui.transcend.universe.input_slot"), 12, 30, 92, 0xffc5b2e4);
        label(g, Component.translatable("gui.transcend.universe.item_output"), 166, 54, 62, 0xff90cadd);
        label(g, Component.translatable("gui.transcend.universe.buffers", compact(BigInteger.valueOf(menu.inputEnergy())), compact(BigInteger.valueOf(menu.energy()))), 12, 80, 216, 0xffb8cce0);
        label(g, Component.translatable("gui.transcend.generator_phase", Component.translatable("gui.transcend.universe.phase." + menu.phase().toLowerCase(Locale.ROOT))), 12, 101, 216, 0xff8ed0b8);
        Component amount = menu.phase().equals("CHARGING")
                ? Component.translatable("gui.transcend.universe.charged", compact(menu.charged()), compact(MiniUniverseGeneratorBlockEntity.STARTUP))
                : Component.translatable("gui.transcend.generator_remaining", compact(menu.remaining()));
        label(g, amount, 12, 112, 216, 0xffb8cce0);
        label(g, statusLabel(), 12, 123, 216, 0xffe0bc80);
        label(g, playerInventoryTitle, MiniUniverseGeneratorMenu.INVENTORY_X, 138, 170, 0xff94a9bf);
    }

    private Component statusLabel() {
        return Component.translatable("gui.transcend.universe.status", Component.translatable("gui.transcend.universe.status." + menu.status().toLowerCase(Locale.ROOT)));
    }

    private static String compact(BigInteger value) {
        String digits = value.toString();
        return digits.length() <= 7 ? digits : digits.charAt(0) + "." + digits.substring(1, 3) + "e" + (digits.length() - 1);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        if (isHovering(12, 25, 92, 18, mouseX, mouseY)) {
            g.renderTooltip(font, font.split(Component.translatable("gui.transcend.universe.help"), 220), mouseX, mouseY);
        } else if (isHovering(12, 80, 216, 17, mouseX, mouseY)) {
            g.renderComponentTooltip(font, java.util.List.of(Component.translatable("gui.transcend.universe.buffers", menu.inputEnergy(), menu.energy()), Component.translatable("gui.transcend.universe.ports")), mouseX, mouseY);
        } else if (isHovering(12, 101, 216, 20, mouseX, mouseY)) {
            g.renderTooltip(font, font.split(Component.translatable("gui.transcend.universe.exact", menu.charged().toString(), MiniUniverseGeneratorBlockEntity.STARTUP.toString(), menu.remaining().toString()), 220), mouseX, mouseY);
        } else if (isHovering(12, 123, 216, 10, mouseX, mouseY)) {
            g.renderTooltip(font, font.split(statusLabel(), 220), mouseX, mouseY);
        } else if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index == 0) {
            g.renderTooltip(font, Component.translatable("gui.transcend.universe.input_slot"), mouseX, mouseY);
        }
    }
}
