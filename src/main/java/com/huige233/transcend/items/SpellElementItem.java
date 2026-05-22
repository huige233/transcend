package com.huige233.transcend.items;

import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.spell.SpellElement;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SpellElementItem extends Item {

    private final SpellElement element;

    public SpellElementItem(SpellElement element) {
        super(new Properties().stacksTo(16));
        this.element = element;
        ModItems.ITEMS.add(this);
    }

    public SpellElement getElement() {
        return element;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.EPIC;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        ChatFormatting nameColor = getElementColor();
        tooltip.add(Component.translatable(element.getDisplayKey())
                .withStyle(nameColor));
        tooltip.add(Component.translatable("tooltip.transcend.spell_element.damage",
                        String.format("%.1f", element.getBaseDamage()))
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.transcend.spell_element.mana_cost",
                        element.getManaCost())
                .withStyle(ChatFormatting.AQUA));
    }

    private ChatFormatting getElementColor() {
        return switch (element) {
            case FIRE -> ChatFormatting.RED;
            case ICE -> ChatFormatting.AQUA;
            case THUNDER -> ChatFormatting.YELLOW;
            case WIND -> ChatFormatting.GREEN;
            case EARTH -> ChatFormatting.GOLD;
            case VOID -> ChatFormatting.DARK_PURPLE;
            case HOLY -> ChatFormatting.WHITE;
            case BLOOD -> ChatFormatting.DARK_RED;
            case DARK -> ChatFormatting.DARK_GRAY;
            case LIGHT -> ChatFormatting.YELLOW;
            case POISON -> ChatFormatting.DARK_GREEN;
            case TIME -> ChatFormatting.GOLD;
            case SPACE -> ChatFormatting.LIGHT_PURPLE;
            case NATURE -> ChatFormatting.GREEN;
            case CHAOS -> ChatFormatting.LIGHT_PURPLE;
            case ACID -> ChatFormatting.DARK_GREEN;
            case SONIC -> ChatFormatting.WHITE;
            case ELDRITCH -> ChatFormatting.DARK_PURPLE;
        };
    }
}
