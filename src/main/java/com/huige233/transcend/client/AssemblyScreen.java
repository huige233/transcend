package com.huige233.transcend.client;

import com.huige233.transcend.menu.AssemblyMenu;
import com.huige233.transcend.tech.assembly.AssemblyKnowledge;
import com.huige233.transcend.tech.assembly.AssemblyRules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 提供装配台加工、模块装卸、充能和批量任务操作，并显示材料、熟练度与任务进度。 */
public class AssemblyScreen extends MachineScreen<AssemblyMenu> {
    private final Button[] actions = new Button[10];
    private Button batchLess, batchMore, cancel;
    public AssemblyScreen(AssemblyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 284;
        imageHeight = 278;
        inventoryLabelX = 61;
        inventoryLabelY = 183;
    }

    @Override protected void init() {
        super.init();
        String[] names = {"press", "grind", "cast", "fabricate", "recycle", "install", "remove_ammo", "remove_barrel", "remove_muzzle", "charge"};
        for (int i = 0; i < names.length; i++) {
            final int action = i;
            actions[i] = addRenderableWidget(Button.builder(Component.translatable("assembly.transcend." + names[i]), button -> {
                if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                        action < 5 && hasShiftDown() ? AssemblyMenu.BUTTON_SELECT_FIRST + action : action);
            }).bounds(leftPos + 9 + i % 5 * 54, topPos + 77 + i / 5 * 22, 50, 20).build(MachineButton::new));
            if (i < 5) actions[i].setTooltip(null); // Recipe hints are rendered below.
        }
        batchLess = addRenderableWidget(Button.builder(Component.literal("−"), button -> sendAction(hasShiftDown() ? AssemblyMenu.BUTTON_BATCH_LESS_EIGHT : AssemblyMenu.BUTTON_BATCH_LESS))
                .bounds(leftPos + 8, topPos + 138, 20, 20).build(MachineButton::new));
        batchMore = addRenderableWidget(Button.builder(Component.literal("+"), button -> sendAction(hasShiftDown() ? AssemblyMenu.BUTTON_BATCH_MORE_EIGHT : AssemblyMenu.BUTTON_BATCH_MORE))
                .bounds(leftPos + 30, topPos + 138, 20, 20).build(MachineButton::new));
        cancel = addRenderableWidget(Button.builder(Component.translatable("assembly.transcend.cancel"), button -> sendAction(AssemblyMenu.BUTTON_CANCEL))
                .bounds(leftPos + 222, topPos + 138, 52, 20).build(MachineButton::new));
        batchLess.setTooltip(Tooltip.create(Component.translatable("assembly.transcend.batch_less")));
        batchMore.setTooltip(Tooltip.create(Component.translatable("assembly.transcend.batch_more")));
        updateControls();
    }

    private void updateControls() {
        boolean running = menu.remaining() > 0;
        for (int i = 0; i < 9; i++) actions[i].active = !running;
        boolean shield = menu.getSlot(0).getItem().getItem() instanceof com.huige233.transcend.items.tech.PhaseShield;
        actions[6].setMessage(Component.translatable(shield ? "assembly.transcend.remove_shield_module" : "assembly.transcend.remove_ammo"));
        actions[6].setTooltip(shield ? Tooltip.create(Component.translatable("assembly.transcend.remove_shield_module.hint")) : null);
        actions[7].active &= !shield;
        actions[8].active &= !shield;
        batchLess.active = !running && menu.batchSize() > 1;
        batchMore.active = !running && menu.batchSize() < 64;
        cancel.active = running;
    }

    @Override protected void containerTick() { super.containerTick(); updateControls(); }

    private void sendAction(int action) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachinePanelStyle.frame(graphics, leftPos, topPos, imageWidth, imageHeight, 0xff59b8b7);
        MachinePanelStyle.panel(graphics, leftPos + 7, topPos + 22, 270, 51);
        MachinePanelStyle.panel(graphics, leftPos + 7, topPos + 122, 270, 58);
        MachinePanelStyle.panel(graphics, leftPos + 56, topPos + 190, 172, 81);
        for (var slot : menu.slots) MachinePanelStyle.slot(graphics, leftPos, topPos, slot,
                slot.index == 4 ? 0xffc3a472 : 0xff405a6e);
        MachinePanelStyle.bar(graphics, leftPos + 10, topPos + 175, 264,
                menu.duration() > 0 ? (double) menu.progress() / menu.duration() : 0, 0xff59b8b7);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawLabel(graphics, title, 10, 8, 264, MachinePanelStyle.TEXT);
        String[] labels = {"host", "module", "materials", "output", "battery"};
        int[] positions = {40, 72, 119, 184, 217};
        for (int i = 0; i < labels.length; i++) drawLabel(graphics,
                Component.translatable("assembly.transcend." + labels[i]), positions[i], 24,
                i == 2 ? 60 : i == 4 ? 53 : 30, MachinePanelStyle.MUTED);
        drawLabel(graphics, Component.translatable("assembly.transcend.energy", menu.energy()), 12, 59, 164, 0xff8fdae3);
        drawLabel(graphics, Component.translatable("assembly.transcend.knowledge", menu.knowledgeTier()), 180, 59, 92, 0xff8fdae3);
        drawLabel(graphics, Component.translatable("assembly.transcend.levels",
                AssemblyRules.level(menu.xp(0)), AssemblyRules.level(menu.xp(1)),
                AssemblyRules.level(menu.xp(2)), AssemblyRules.level(menu.xp(3))), 12, 125, 260, MachinePanelStyle.MUTED);
        drawLabel(graphics, Component.translatable("assembly.transcend.batch", menu.batchSize(), menu.remaining()),
                55, 144, 161, MachinePanelStyle.TEXT);
        drawLabel(graphics, Component.translatable("assembly.transcend.status." + menu.status()), 12, 162, 210,
                menu.status() > 1 ? 0xffe0bc80 : 0xff8ed0b8);
        int percent = menu.duration() <= 0 ? 0 : (int) Math.min(100, 100L * menu.progress() / menu.duration());
        Component progress = Component.literal(percent + "%");
        drawLabel(graphics, progress, 272 - font.width(progress), 162, 48, MachinePanelStyle.TEXT);
        drawLabel(graphics, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 164, MachinePanelStyle.MUTED);
    }

    @Override protected boolean renderHints(GuiGraphics graphics, int mouseX, int mouseY, int tooltipX, int tooltipY) {
        if (isHovering(9, 5, 264, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("assembly.transcend.workflow_hint"), tooltipX, tooltipY);
            return true;
        }
        if (isHovering(10, 173, 264, 7, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("assembly.transcend.progress", menu.progress(), menu.duration()), tooltipX, tooltipY);
            return true;
        } else if (hoveredSlot != null && hoveredSlot.index < 6) {
            String[] names = {"host", "module", "materials", "materials", "output", "battery"};
            graphics.renderTooltip(font, Component.translatable("assembly.transcend." + names[hoveredSlot.index]), tooltipX, tooltipY);
            return true;
        }
        for (int i = 0; i < 5; i++) {
            int x = leftPos + 9 + i * 54;
            if (mouseX < x || mouseX >= x + 50 || mouseY < topPos + 77 || mouseY >= topPos + 97) continue;
            final int process = i;
            menu.recipe(i).ifPresentOrElse(recipe -> graphics.renderComponentTooltip(font, java.util.List.of(
                    recipe.output().getHoverName(),
                    Component.translatable("assembly.transcend.select_recipe", menu.recipeChoices(process).indexOf(recipe) + 1,
                            menu.recipeChoices(process).size()),
                    Component.translatable("assembly.transcend.requirements", recipe.tier(),
                            AssemblyKnowledge.requiredProficiency(recipe.tier(), recipe.process())).withStyle(
                            AssemblyKnowledge.canManufacture(menu.knowledgeTier(), AssemblyRules.level(menu.xp(process)), recipe.tier(), recipe.process())
                                    ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED),
                    com.huige233.transcend.tech.research.ResearchUnlocks.required(recipe.output()) == null
                            ? Component.translatable("assembly.transcend.research_none")
                            : Component.translatable("assembly.transcend.research_required", Component.translatable("research.transcend."
                                    + com.huige233.transcend.tech.research.ResearchUnlocks.required(recipe.output()))),
                    Component.translatable("assembly.transcend.cost", recipe.firstCount(), recipe.secondCount(), recipe.energy()),
                    Component.translatable("assembly.transcend.materials_progress", menu.getSlot(2).getItem().getCount(), recipe.firstCount(),
                            menu.getSlot(3).getItem().getCount(), recipe.secondCount()).withStyle(
                            menu.getSlot(2).getItem().getCount() >= recipe.firstCount() && menu.getSlot(3).getItem().getCount() >= recipe.secondCount()
                                    ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED),
                    Component.translatable("assembly.transcend.duration_hint", String.format(java.util.Locale.ROOT, "%.1f", recipe.durationTicks() / 20.0)),
                    Component.translatable("assembly.transcend.salvage_hint", String.format(java.util.Locale.ROOT, "%.0f",
                            100 * com.huige233.transcend.tech.TechConfig.assemblySalvageRate())),
                    Component.translatable("assembly.transcend.chance", String.format(java.util.Locale.ROOT, "%.1f",
                            100 * (1 - AssemblyRules.failureChance(recipe.failure(), menu.xp(process))))),
                    Component.translatable("assembly.transcend.xp", menu.xp(process), AssemblyRules.level(menu.xp(process)))) , tooltipX, tooltipY),
                    () -> graphics.renderTooltip(font, Component.translatable("assembly.transcend.no_recipe"), tooltipX, tooltipY));
            return true;
        }
        return false;
    }
}
