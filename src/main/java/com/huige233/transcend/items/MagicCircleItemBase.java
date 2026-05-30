package com.huige233.transcend.items;

import com.huige233.transcend.ModRarities;
import com.huige233.transcend.client.magic.*;
import com.huige233.transcend.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <b>已弃用 (R55):</b> 旧版手持法环物品基类。
 *
 * <p>R45 起项目转向 BlockEntity 化的多方块法环（{@code MagicCircleCoreBlockEntity}
 * + 符文石/导管/符印结构）。本物品系列保留注册以兼容旧存档，但：
 * <ul>
 *   <li>{@link #use(Level, Player, InteractionHand)} 不再生成临时法环效果</li>
 *   <li>提示信息中明确标注"已弃用"并引导玩家使用新系统</li>
 *   <li>已从 {@code TranscendTab} 创造栏隐藏，不再分发给玩家</li>
 *   <li>原有 NBT (mc_power / mc_duration / mc_efficiency / mc_special) 不再被读取</li>
 * </ul>
 *
 * <p>不删除注册的原因：旧存档中玩家背包/箱子里可能仍持有这些物品；
 * 强行注销 ID 会导致 vanilla 加载报缺失项错误。
 */
@Deprecated
public class MagicCircleItemBase extends Item {

    private final MagicCircleType circleType;

    public MagicCircleItemBase(MagicCircleType type) {
        super(new Properties().stacksTo(1));
        this.circleType = type;
        ModItems.ITEMS.add(this);
    }

    public MagicCircleType getCircleType() {
        return circleType;
    }

    /**
     * 已弃用：右键不再生成法环效果。仅显示提示信息引导玩家使用新系统。
     * 保留方法签名以保证向后兼容。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.magic_circle.deprecated")
                            .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC), true);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return MagicCircleNBTHelper.getTotalLevel(stack) > 0;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return ModRarities.COSMIC;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        // 弃用横幅 (R55)
        tooltip.add(Component.translatable("tooltip.transcend.magic_circle.deprecated_banner")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        tooltip.add(Component.translatable("tooltip.transcend.magic_circle.deprecated_hint")
                .withStyle(ChatFormatting.GRAY));
        // 保留旧 lore 一行作为博物馆式怀旧文字
        tooltip.add(Component.translatable(circleType.getTooltipKey("desc1"))
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
