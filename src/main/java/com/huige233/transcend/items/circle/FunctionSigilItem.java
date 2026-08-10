package com.huige233.transcend.items.circle;

import com.huige233.transcend.circle.CircleFunctionType;
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

/** 功能印记徽记物品。 */
public class FunctionSigilItem extends Item {
    private final CircleFunctionType functionType;

    public FunctionSigilItem(CircleFunctionType functionType) {
        super(new Properties().stacksTo(1));
        this.functionType = functionType;
        ModItems.ITEMS.add(this);
    }

    public CircleFunctionType getFunctionType() {
        return functionType;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {

        return switch (functionType.getMinTier().getLevel()) {
            case 1, 2 -> Rarity.UNCOMMON;
            case 3 -> Rarity.RARE;
            case 4, 5 -> Rarity.EPIC;
            default -> Rarity.COMMON;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {

        tooltip.add(Component.translatable(functionType.getTranslationKey() + ".desc")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable("tooltip.transcend.sigil.min_tier",
                        Component.translatable(functionType.getMinTier().getTranslationKey()))
                .withStyle(ChatFormatting.DARK_PURPLE));

        tooltip.add(Component.translatable("tooltip.transcend.sigil.upkeep",
                        String.format("%.1f", functionType.getBaseUpkeepPerMinute()))
                .withStyle(ChatFormatting.AQUA));
    }
}
