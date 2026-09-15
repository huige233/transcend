package com.huige233.transcend.items.tech;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;


/** 通过物品数据保存护盾模块种类，并展示容量、耗能、等级和分类减伤参数。 */
public class ShieldModuleItem extends Item {
    private static final String MODULE = "shield_module";

    public ShieldModuleItem() { super(new Properties().stacksTo(1)); }

    public static ItemStack create(ShieldModule module) {
        ItemStack stack = new ItemStack(com.huige233.transcend.init.ModItems.shield_module.get());
        stack.getOrCreateTag().putString(MODULE, module.id());
        return stack;
    }

    public static ShieldModule getModule(ItemStack stack) {
        if (!(stack.getItem() instanceof ShieldModuleItem)) return null;
        CompoundTag tag = stack.getTag();
        return tag == null ? null : ShieldModule.byId(tag.getString(MODULE));
    }

    @Override
    public Component getName(ItemStack stack) {
        ShieldModule module = getModule(stack);
        return module == null ? Component.translatable("item.transcend.shield_module")
                : Component.translatable("shieldmodule.transcend." + module.id());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ShieldModule module = getModule(stack);
        if (module == null) {
            tooltip.add(Component.translatable("tooltip.transcend.shield_module.blank").withStyle(ChatFormatting.RED));
            return;
        }
        tooltip.add(Component.translatable("tooltip.transcend.shield_module.capacity",
                String.format(java.util.Locale.ROOT, "%.0f", module.capacity())).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.transcend.shield_module.energy_multiplier",
                String.format(java.util.Locale.ROOT, "%.2f", module.energyMultiplier())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.transcend.shield_module.tier", module.tier()).withStyle(ChatFormatting.GOLD));
        if (module.category() >= 0) {
            tooltip.add(Component.translatable("tooltip.transcend.shield_module.type", module.category()).withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(Component.translatable("tooltip.transcend.shield_module.reduction",
                    String.format(java.util.Locale.ROOT, "%.0f", module.typeReduction() * 100.0F)).withStyle(ChatFormatting.GREEN));
        }
    }
}
