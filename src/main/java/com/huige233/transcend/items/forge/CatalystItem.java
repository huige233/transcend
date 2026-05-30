package com.huige233.transcend.items.forge;

import com.huige233.transcend.gear.forge.AspectKind;
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
 * R82: 坩埚催化剂物品（6 种，对应 {@link AspectKind} 6 个值）。
 *
 * <p>玩家放进 {@code aspect_crucible} 4 个槽位中的任意一个；
 * 4 个槽位填满后，由 {@link com.huige233.transcend.gear.forge.AspectRegistry#resolve}
 * 解析为 24 命名 aspect 之一（或 INDETERMINATE 回退）。
 *
 * <p>催化剂本身没有主动行为，只是 marker item；只要 {@code stack.getItem() instanceof CatalystItem}
 * 就能被坩埚识别。
 */
public class CatalystItem extends Item {

    private final AspectKind kind;

    public CatalystItem(AspectKind kind) {
        super(new Properties().rarity(Rarity.UNCOMMON).stacksTo(16));
        this.kind = kind;
    }

    public AspectKind getKind() { return kind; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        // 元素名（带颜色）
        tooltip.add(Component.translatable("catalyst.transcend.tooltip.element",
                Component.translatable(kind.langKey()).withStyle(kind.color))
                .withStyle(ChatFormatting.GRAY));
        // 用法提示
        tooltip.add(Component.translatable("catalyst.transcend.tooltip.usage")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
