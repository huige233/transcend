package com.huige233.transcend.items;

import com.huige233.transcend.spell.SpellAugment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SpellGlyphItem extends Item {

    private final SpellAugment augment;

    public SpellGlyphItem(SpellAugment augment) {
        super(new Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
        this.augment = augment;
    }

    public SpellAugment getAugment() {
        return augment;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("glyph.transcend." + augment.id + ".desc")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("glyph.transcend.usage")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (augment.maxStack > 1) {
            tooltip.add(Component.translatable("glyph.transcend.max_stack",
                            augment.maxStack)
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("glyph.transcend.unique")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
