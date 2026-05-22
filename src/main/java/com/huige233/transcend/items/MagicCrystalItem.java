package com.huige233.transcend.items;

import com.huige233.transcend.ModRarities;
import com.huige233.transcend.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MagicCrystalItem extends Item {

    private final boolean refined;

    public MagicCrystalItem(boolean refined) {
        super(new Properties().stacksTo(64));
        this.refined = refined;
        ModItems.ITEMS.add(this);
    }

    public boolean isRefined() {
        return refined;
    }

    public int getCrystalValue() {
        return refined ? 3 : 1;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return refined;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return refined ? ModRarities.COSMIC : Rarity.RARE;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (refined) {
            tooltip.add(Component.translatable("tooltip.transcend.refined_magic_crystal.desc")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(Component.translatable("tooltip.transcend.refined_magic_crystal.value")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.transcend.magic_crystal.desc")
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.transcend.magic_crystal.value")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
