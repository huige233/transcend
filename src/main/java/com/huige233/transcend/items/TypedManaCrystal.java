package com.huige233.transcend.items;

import com.huige233.transcend.ModRarities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** 型别魔法水晶物品（特定元素）。 */
public class TypedManaCrystal extends MagicCrystalItem {

    public enum ManaAspect {
        AETHER ("aether",  5,  ChatFormatting.AQUA,         Rarity.EPIC,        false),
        BLOOD  ("blood",   8,  ChatFormatting.DARK_RED,     Rarity.EPIC,        true),
        COSMIC ("cosmic",  15, ChatFormatting.LIGHT_PURPLE, ModRarities.COSMIC, true),
        TAINTED("tainted", 2,  ChatFormatting.DARK_GREEN,   Rarity.RARE,        false);

        public final String id;
        public final int value;
        public final ChatFormatting nameColor;
        public final Rarity rarity;
        public final boolean foilGlow;

        ManaAspect(String id, int value, ChatFormatting nameColor, Rarity rarity, boolean foilGlow) {
            this.id = id;
            this.value = value;
            this.nameColor = nameColor;
            this.rarity = rarity;
            this.foilGlow = foilGlow;
        }
    }

    private final ManaAspect aspect;

    public TypedManaCrystal(ManaAspect aspect) {
        super(false);
        this.aspect = aspect;
    }

    public ManaAspect getAspect() {
        return aspect;
    }

    @Override
    public int getCrystalValue() {
        return aspect.value;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return aspect.foilGlow;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return aspect.rarity;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {

        tooltip.add(Component.translatable("tooltip.transcend.crystal_" + aspect.id + ".desc")
                .withStyle(aspect.nameColor));
        tooltip.add(Component.translatable("tooltip.transcend.crystal.value", aspect.value)
                .withStyle(ChatFormatting.GRAY));
    }
}
