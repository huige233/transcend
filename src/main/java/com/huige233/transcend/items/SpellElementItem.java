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

/** 法术元素物品。 */
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
            case METAL -> ChatFormatting.GOLD;
            case WOOD -> ChatFormatting.GREEN;
            case WATER -> ChatFormatting.AQUA;
            case FIRE -> ChatFormatting.RED;
            case EARTH -> ChatFormatting.GOLD;
            case CHAOS -> ChatFormatting.LIGHT_PURPLE;
        };
    }
}
