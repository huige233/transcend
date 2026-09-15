package com.huige233.transcend.items.tech;

import com.huige233.transcend.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import java.util.List;

/** 保存并限制黑洞种子的能量值，判定未激活状态并据能量显示光效与提示。 */
public final class BlackHoleSeedItem extends Item {
    public static final String POWER = "Power";
    public static final long MAX_POWER = 1_000_000_000_000L;
    public static final String STATE = "State";
    public static final String INACTIVE = "inactive";
    public static boolean isInactive(ItemStack stack) {
        return stack.is(ModItems.black_hole_seed.get()) && power(stack) == 0L
                && !stack.getOrCreateTag().getBoolean("Activated");
    }
    public BlackHoleSeedItem() { super(new Properties().stacksTo(16).fireResistant()); }
    public static long power(ItemStack stack) { return Math.max(0L, Math.min(MAX_POWER, stack.getOrCreateTag().getLong(POWER))); }
    public static void setPower(ItemStack stack, long value) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putLong(POWER, Math.max(0L, Math.min(MAX_POWER, value)));
    }
    @Override public boolean isFoil(ItemStack stack) { return power(stack) > 0; }
    @Override public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level level, List<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.transcend.black_hole_seed.power", power(stack)));
        lines.add(Component.translatable("tooltip.transcend.black_hole_seed.lore"));
    }
}
