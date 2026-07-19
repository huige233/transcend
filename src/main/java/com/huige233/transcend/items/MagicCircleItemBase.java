package com.huige233.transcend.items;

import com.huige233.transcend.ModRarities;
import com.huige233.transcend.magic.*;
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

        tooltip.add(Component.translatable("tooltip.transcend.magic_circle.deprecated_banner")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        tooltip.add(Component.translatable("tooltip.transcend.magic_circle.deprecated_hint")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable(circleType.getTooltipKey("desc1"))
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
