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

public class SpellUpgradeStone extends Item {

    public SpellUpgradeStone() {
        super(new Properties().stacksTo(16));
        ModItems.ITEMS.add(this);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return ModRarities.COSMIC;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.transcend.upgrade_stone.desc")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.transcend.upgrade_stone.usage")
                .withStyle(ChatFormatting.GRAY));
    }
}
