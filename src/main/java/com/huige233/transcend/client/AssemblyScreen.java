package com.huige233.transcend.client;

import com.huige233.transcend.menu.AssemblyMenu;
import com.huige233.transcend.tech.assembly.AssemblyKnowledge;
import com.huige233.transcend.tech.assembly.AssemblyRules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 提供装配台加工、模块装卸、充能和批量任务操作，并显示材料、熟练度与任务进度。 */
public class AssemblyScreen extends AbstractContainerScreen<AssemblyMenu> {
    public AssemblyScreen(AssemblyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 284;
        imageHeight = 278;
        inventoryLabelX = 61;
        inventoryLabelY = 183;
    }

    @Override protected void init() {
        super.init();
        String[] actions = {"press", "grind", "cast", "fabricate", "recycle", "install", "remove_ammo", "remove_barrel", "remove_muzzle", "charge"};
        for (int i = 0; i < actions.length; i++) {
            final int action = i;
            addRenderableWidget(Button.builder(Component.translatable("assembly.transcend." + actions[i]), button -> {
                if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
            }).bounds(leftPos + 9 + i % 5 * 54, topPos + 77 + i / 5 * 22, 50, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("-"), button -> sendAction(AssemblyMenu.BUTTON_BATCH_LESS))
                .bounds(leftPos + 8, topPos + 138, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> sendAction(AssemblyMenu.BUTTON_BATCH_MORE))
                .bounds(leftPos + 30, topPos + 138, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("assembly.transcend.cancel"), button -> sendAction(AssemblyMenu.BUTTON_CANCEL))
                .bounds(leftPos + 222, topPos + 138, 52, 20).build());
    }

    private void sendAction(int action) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachinePanelStyle.frame(graphics, leftPos, topPos, imageWidth, imageHeight, 0xff59b8b7);
        MachinePanelStyle.panel(graphics, leftPos + 7, topPos + 20, 270, 54);
        MachinePanelStyle.panel(graphics, leftPos + 7, topPos + 122, 270, 58);
        MachinePanelStyle.panel(graphics, leftPos + 56, topPos + 190, 172, 81);
        for (var slot : menu.slots) MachinePanelStyle.slot(graphics, leftPos, topPos, slot,
                slot.index == 4 ? 0xffc3a472 : 0xff405a6e);
        MachinePanelStyle.bar(graphics, leftPos + 10, topPos + 175, 264,
                menu.duration() > 0 ? (double) menu.progress() / menu.duration() : 0, 0xff59b8b7);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xffa6ebed, false);
        String[] labels = {"host", "module", "materials", "output", "battery"};
        int[] positions = {40, 72, 119, 184, 217};
        for (int i = 0; i < labels.length; i++) graphics.drawString(font,
                Component.translatable("assembly.transcend." + labels[i]), positions[i], 23, 0xffb7c6d4, false);
        graphics.drawString(font, Component.translatable("assembly.transcend.energy", menu.energy()), 8, 58, 0xff8fdae3, false);
        graphics.drawString(font, Component.translatable("assembly.transcend.knowledge", menu.knowledgeTier()), 8, 68, 0xff8fdae3, false);
        graphics.drawString(font, Component.translatable("assembly.transcend.levels",
                AssemblyRules.level(menu.xp(0)), AssemblyRules.level(menu.xp(1)),
                AssemblyRules.level(menu.xp(2)), AssemblyRules.level(menu.xp(3))), 8, 125, 0xffb7c6d4, false);
        graphics.drawString(font, Component.translatable("assembly.transcend.batch", menu.batchSize(), menu.remaining()),
                55, 144, 0xffb7c6d4, false);
        graphics.drawString(font, Component.translatable("assembly.transcend.status." + menu.status()), 8, 162, 0xff8fdae3, false);
        graphics.drawString(font, Component.translatable("assembly.transcend.progress", menu.progress(), menu.duration()),
                8, 172, 0xffb7c6d4, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xffb7c6d4, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        for (int i = 0; i < 5; i++) {
            int x = leftPos + 9 + i * 54;
            if (mouseX < x || mouseX >= x + 50 || mouseY < topPos + 77 || mouseY >= topPos + 97) continue;
            final int process = i;
            menu.recipe(i).ifPresentOrElse(recipe -> graphics.renderComponentTooltip(font, java.util.List.of(
                    recipe.output().getHoverName(),
                    Component.translatable("assembly.transcend.requirements", recipe.tier(),
                            AssemblyKnowledge.requiredProficiency(recipe.tier())).withStyle(
                            AssemblyKnowledge.canManufacture(menu.knowledgeTier(), AssemblyRules.level(menu.xp(process)), recipe.tier())
                                    ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED),
                    Component.translatable("assembly.transcend.cost", recipe.firstCount(), recipe.secondCount(), recipe.energy()),
                    Component.translatable("assembly.transcend.chance", String.format(java.util.Locale.ROOT, "%.1f",
                            100 * (1 - AssemblyRules.failureChance(recipe.failure(), menu.xp(process))))),
                    Component.translatable("assembly.transcend.xp", menu.xp(process), AssemblyRules.level(menu.xp(process)))) , mouseX, mouseY),
                    () -> graphics.renderTooltip(font, Component.translatable("assembly.transcend.no_recipe"), mouseX, mouseY));
        }
    }
}
