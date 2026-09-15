package com.huige233.transcend.items.tech;

import com.huige233.transcend.ModRarities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;


/** 定义不可堆叠且耐火的天狼星专用枪口模块物品，并显示其高阶科技提示。 */
public final class SiriusModuleItem extends GunModuleItem {
    public SiriusModuleItem() {
        super(new Properties().rarity(ModRarities.COSMIC).stacksTo(1).fireResistant());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.transcend.sirius_module.tier").withStyle(ChatFormatting.GOLD));
    }
}
