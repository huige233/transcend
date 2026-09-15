package com.huige233.transcend.client;

import com.huige233.transcend.block.FEGeneratorBlockEntity;
import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.menu.GeneratorMenu;
import com.huige233.transcend.network.C2SGeneratorOutputPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.Locale;

/** 展示燃料、风力和创造发电机的能量与运行状态，并向服务端提交创造发电机输出调整请求。 */
public class GeneratorScreen extends AbstractContainerScreen<GeneratorMenu> {
    private Button decrease, increase;

    public GeneratorScreen(GeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override protected void init() {
        super.init();
        
        decrease = addRenderableWidget(Button.builder(Component.literal("−"), b -> changeOutput(-step()))
                .bounds(leftPos + 110, topPos + 35, 26, 18).build());
        increase = addRenderableWidget(Button.builder(Component.literal("+"), b -> changeOutput(step()))
                .bounds(leftPos + 140, topPos + 35, 26, 18).build());
        decrease.setTooltip(Tooltip.create(Component.translatable("gui.transcend.generator.decrease_output")));
        increase.setTooltip(Tooltip.create(Component.translatable("gui.transcend.generator.increase_output")));
        updateControls();
    }

    private int step() { return FEGeneratorBlockEntity.CREATIVE_OUTPUT_STEP * (hasControlDown() ? 100 : hasShiftDown() ? 10 : 1); }
    private void changeOutput(int delta) { NetworkHandler.CHANNEL.sendToServer(new C2SGeneratorOutputPacket(menu.machinePos(), delta)); }
    private void updateControls() {
        boolean creative = menu.mode() == FEGeneratorBlockEntity.Mode.CREATIVE;
        decrease.visible = increase.visible = creative;
        decrease.active = creative && menu.configuredOutput() > 0;
        increase.active = creative && menu.configuredOutput() < FEGeneratorBlockEntity.CREATIVE_MAX_OUTPUT;
    }
    @Override protected void containerTick() { super.containerTick(); updateControls(); }

    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        MachinePanelStyle.frame(g, x, y, imageWidth, imageHeight, accent());
        MachinePanelStyle.panel(g, x + 6, y + 21, 164, 48);
        MachinePanelStyle.bar(g, x + 10, y + 25, 156, menu.energy() / 100000d, accent());
        for (int i = 0; i < menu.slots.size(); i++) {
            if (i == 0 && menu.mode() != FEGeneratorBlockEntity.Mode.FIRE) continue;
            MachinePanelStyle.slot(g, x, y, menu.slots.get(i), i == 0 ? accent() : 0xff293a50);
        }
        if (menu.mode() == FEGeneratorBlockEntity.Mode.FIRE) {
            MachinePanelStyle.bar(g, x + 106, y + 41, 54,
                    menu.burnDuration() > 0 ? (double) Math.max(0, menu.burnTime()) / menu.burnDuration() : 0, accent());
        }
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        label(g, title, 8, 9, 160, 0xffe1eaf5);
        if (menu.mode() == FEGeneratorBlockEntity.Mode.CREATIVE) {
            label(g, Component.translatable("gui.transcend.generator_output", compact(menu.configuredOutput())), 10, 39, 96, 0xffd0b2ef);
        } else {
            label(g, Component.translatable(menu.mode() == FEGeneratorBlockEntity.Mode.FIRE
                    ? "gui.transcend.generator_fuel" : "gui.transcend.generator_wind"), 10, 39,
                    menu.mode() == FEGeneratorBlockEntity.Mode.FIRE ? 64 : 156, 0xffb8cce0);
        }
        label(g, statusLabel(), 10, 57, 156, 0xff8ed0b8);
        label(g, playerInventoryTitle, 8, 73, 160, 0xff94a9bf);
    }

    private Component statusLabel() {
        String key = switch (menu.status()) {
            case GENERATING -> "generating"; case FULL -> "full"; case BLOCKED -> "blocked"; default -> "idle";
        };
        return Component.translatable("gui.transcend.generator_status." + key);
    }
    private void label(GuiGraphics g, Component text, int x, int y, int width, int color) {
        MachinePanelStyle.label(g, font, text, x, y, width, color);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
        if (isHovering(10, 23, 156, 8, mouseX, mouseY)) {
            g.renderTooltip(font, Component.translatable("gui.transcend.generator_energy", menu.energy(), 100000), mouseX, mouseY);
        } else if (menu.mode() == FEGeneratorBlockEntity.Mode.CREATIVE && isHovering(10, 35, 96, 18, mouseX, mouseY)) {
            g.renderComponentTooltip(font, java.util.List.of(
                    Component.translatable("gui.transcend.generator_output", menu.configuredOutput()),
                    Component.translatable("gui.transcend.generator_output_limit", FEGeneratorBlockEntity.CREATIVE_MAX_OUTPUT)), mouseX, mouseY);
        } else if (isHovering(10, 57, 156, 10, mouseX, mouseY)) {
            g.renderTooltip(font, font.split(statusLabel(), 200), mouseX, mouseY);
        }
    }
    private int accent() { return switch (menu.mode()) { case FIRE -> 0xffd79651; case WIND -> 0xff55b9cb; case CREATIVE -> 0xffac7de0; }; }
    private static String compact(long value) {
        if (value >= 1_000_000_000L) return String.format(Locale.ROOT, "%.1fG", value / 1_000_000_000d);
        if (value >= 1_000_000L) return String.format(Locale.ROOT, "%.1fM", value / 1_000_000d);
        if (value >= 1_000L) return String.format(Locale.ROOT, "%.1fk", value / 1_000d);
        return Long.toString(value);
    }
}
