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

/**
 * 功能符印 — 决定法阵执行何种功能。
 * 每个符印对应一个 CircleFunctionType。
 */
public class FunctionSigilItem extends Item {
    private final CircleFunctionType functionType;

    public FunctionSigilItem(CircleFunctionType functionType) {
        super(new Properties().stacksTo(1));
        this.functionType = functionType;
        ModItems.ITEMS.add(this);
    }

    /**
     * 获取该符印对应的法阵功能类型。
     */
    public CircleFunctionType getFunctionType() {
        return functionType;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        // 根据最低层级决定稀有度
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
        // 功能描述
        tooltip.add(Component.translatable(functionType.getTranslationKey() + ".desc")
                .withStyle(ChatFormatting.GRAY));
        // 最低层级要求
        tooltip.add(Component.translatable("tooltip.transcend.sigil.min_tier",
                        Component.translatable(functionType.getMinTier().getTranslationKey()))
                .withStyle(ChatFormatting.DARK_PURPLE));
        // 维持成本
        tooltip.add(Component.translatable("tooltip.transcend.sigil.upkeep",
                        String.format("%.1f", functionType.getBaseUpkeepPerMinute()))
                .withStyle(ChatFormatting.AQUA));
    }
}
