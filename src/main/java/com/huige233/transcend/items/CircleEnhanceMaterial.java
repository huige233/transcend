package com.huige233.transcend.items;

import com.huige233.transcend.ModRarities;
import com.huige233.transcend.client.magic.MagicCircleNBTHelper;
import com.huige233.transcend.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

public class CircleEnhanceMaterial extends Item {

    private final MagicCircleNBTHelper.EnhanceType enhanceType;

    public CircleEnhanceMaterial(MagicCircleNBTHelper.EnhanceType type) {
        super(new Properties().stacksTo(16));
        this.enhanceType = type;
        ModItems.ITEMS.add(this);
    }

    public MagicCircleNBTHelper.EnhanceType getEnhanceType() {
        return enhanceType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        ItemStack materialStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offhandStack = player.getItemInHand(InteractionHand.OFF_HAND);

        if (level.isClientSide) {
            return InteractionResultHolder.success(materialStack);
        }

        if (!(offhandStack.getItem() instanceof MagicCircleItemBase)) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.enhance_need_circle")
                            .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.fail(materialStack);
        }

        if (!MagicCircleNBTHelper.canEnhance(offhandStack, enhanceType)) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.enhance_max_level")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(materialStack);
        }

        MagicCircleNBTHelper.enhance(offhandStack, enhanceType);
        if (!player.isCreative()) {
            materialStack.shrink(1);
        }

        String typeKey = switch (enhanceType) {
            case POWER -> "msg.transcend.enhance_power_up";
            case DURATION -> "msg.transcend.enhance_duration_up";
            case EFFICIENCY -> "msg.transcend.enhance_efficiency_up";
            case SPECIAL -> "msg.transcend.enhance_special_up";
        };
        int newLevel = MagicCircleNBTHelper.getLevel(offhandStack, enhanceType);
        player.displayClientMessage(
                Component.translatable(typeKey, newLevel)
                        .withStyle(ChatFormatting.GREEN), true);

        level.playSound(null, player.blockPosition(),
                SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8F, 1.5F);

        player.getCooldowns().addCooldown(this, 20);

        return InteractionResultHolder.success(materialStack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return ModRarities.COSMIC;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        String key = switch (enhanceType) {
            case POWER -> "tooltip.transcend.enhance_power.desc";
            case DURATION -> "tooltip.transcend.enhance_duration.desc";
            case EFFICIENCY -> "tooltip.transcend.enhance_efficiency.desc";
            case SPECIAL -> "tooltip.transcend.enhance_special.desc";
        };
        ChatFormatting color = switch (enhanceType) {
            case POWER -> ChatFormatting.RED;
            case DURATION -> ChatFormatting.AQUA;
            case EFFICIENCY -> ChatFormatting.GREEN;
            case SPECIAL -> ChatFormatting.LIGHT_PURPLE;
        };
        tooltip.add(Component.translatable(key).withStyle(color));
        tooltip.add(Component.translatable("tooltip.transcend.enhance.usage").withStyle(ChatFormatting.GRAY));
    }
}
