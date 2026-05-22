package com.huige233.transcend.items;

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
 * Round 37: 封印古卷 (Sealed Scroll) — 所有 20 张古法咒卷的统一合成基底。
 *
 * <p>本身无效果 — 只是合成 ingredient。在 anvil / smithing 中与不同 catalyst
 * 组合形成对应的 ancient scroll。
 *
 * <p>设计目的：
 * <ul>
 *   <li>统一 progression — 玩家先获得 Sealed Scroll（结构掉落 / boss drop / craft）</li>
 *   <li>再通过元素水晶 + 卷轴特定材料 → 解封为某种 ancient scroll</li>
 *   <li>避免每张古卷都要独立的 9-cell 合成槽——节省合成空间</li>
 * </ul>
 *
 * <p>可堆叠到 16，UNCOMMON rarity。
 */
public class SealedScrollItem extends Item {

    public SealedScrollItem() {
        super(new Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.transcend.sealed_scroll.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.transcend.sealed_scroll.hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
