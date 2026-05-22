package com.huige233.transcend.items;

import com.huige233.transcend.ModRarities;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.spell.WandRune;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RuneItem extends Item {

    private final WandRune rune;

    public RuneItem(WandRune rune) {
        super(new Properties().stacksTo(1));
        this.rune = rune;
        ModItems.ITEMS.add(this);
    }

    public WandRune getRune() {
        return rune;
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
        tooltip.add(Component.translatable(rune.displayKey).withStyle(rune.color));
        tooltip.add(Component.translatable(rune.getDescKey()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.transcend.rune.usage").withStyle(ChatFormatting.DARK_GRAY));
    }
}
