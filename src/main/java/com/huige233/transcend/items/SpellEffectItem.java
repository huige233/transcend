package com.huige233.transcend.items;

import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.spell.SpellEffect;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SpellEffectItem extends Item {

    private final SpellEffect effect;

    public SpellEffectItem(SpellEffect effect) {
        super(new Properties().stacksTo(16));
        this.effect = effect;
        ModItems.ITEMS.add(this);
    }

    public SpellEffect getEffect() {
        return effect;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.RARE;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(effect.getDisplayKey())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable(effect.getDisplayKey() + ".desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
