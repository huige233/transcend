package com.huige233.transcend.client;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.tech.assembly.AssemblyKnowledge;
import com.huige233.transcend.tech.assembly.AssemblyRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Locale;


/** 在 JEI 中展示装配配方的材料、产物、能耗、等级要求、失败率与耗时。 */
public final class AssemblyJeiCategory implements IRecipeCategory<AssemblyRecipe> {
    public static final RecipeType<AssemblyRecipe> TYPE = new RecipeType<>(Transcend.rl("assembly"), AssemblyRecipe.class);
    private final IDrawable background;
    private final IDrawable icon;

    public AssemblyJeiCategory(IGuiHelper gui) {
        background = gui.createBlankDrawable(170, 108);
        icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.assembly_table.get()));
    }

    @Override public RecipeType<AssemblyRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("block.transcend.assembly_table"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AssemblyRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 12, 19).addItemStacks(Arrays.stream(recipe.first().getItems())
                .map(stack -> stack.copyWithCount(recipe.firstCount())).toList());
        if (recipe.secondCount() > 0) {
            builder.addSlot(RecipeIngredientRole.INPUT, 48, 19).addItemStacks(Arrays.stream(recipe.second().getItems())
                    .map(stack -> stack.copyWithCount(recipe.secondCount())).toList());
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 137, 19).addItemStack(recipe.output().copy());
    }

    @Override
    public void draw(AssemblyRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("assembly.transcend." + recipe.process()), 4, 2, 0x404040, false);
        graphics.drawString(font, "+", 35, 23, 0x404040, false);
        graphics.drawString(font, "->", 94, 23, 0x404040, false);
        graphics.drawString(font, Component.translatable("assembly.transcend.energy", recipe.energy()), 4, 43, 0x404040, false);
        graphics.drawString(font, Component.translatable("jei.transcend.assembly.requirements", recipe.tier(),
                AssemblyKnowledge.requiredProficiency(recipe.tier())), 4, 56, 0x404040, false);
        graphics.drawString(font, Component.translatable("jei.transcend.assembly.failure",
                String.format(Locale.ROOT, "%.0f", recipe.failure() * 100)), 4, 69, 0x404040, false);
        graphics.drawString(font, Component.translatable("jei.transcend.assembly.duration", recipe.tier() * 40),
                4, 82, 0x404040, false);
        graphics.drawString(font, Component.translatable("jei.transcend.assembly.session"), 4, 95, 0x404040, false);
    }
}
