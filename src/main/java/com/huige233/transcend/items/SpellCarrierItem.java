package com.huige233.transcend.items;

import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.spell.SpellCarrier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SpellCarrierItem extends Item {

    private final SpellCarrier carrier;

    public SpellCarrierItem(SpellCarrier carrier) {
        super(new Properties().stacksTo(16));
        this.carrier = carrier;
        ModItems.ITEMS.add(this);
    }

    public SpellCarrier getCarrier() {
        return carrier;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.RARE;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(carrier.getDisplayKey())
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(carrier.getDisplayKey() + ".desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
