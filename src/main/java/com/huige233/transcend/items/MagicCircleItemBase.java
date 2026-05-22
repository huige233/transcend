package com.huige233.transcend.items;

import com.huige233.transcend.ModRarities;
import com.huige233.transcend.client.magic.*;
import com.huige233.transcend.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            int cost = MagicCircleNBTHelper.getCrystalCost(stack);
            if (!player.isCreative() && !MagicCrystalHelper.hasEnoughMana(player, cost)) {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.crystal_insufficient", cost)
                                .withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }

            MagicCrystalHelper.consumeMana(player, cost);

            float powerMult = MagicCircleNBTHelper.getPowerMultiplier(stack);
            float durationMult = MagicCircleNBTHelper.getDurationMultiplier(stack);
            int specialLvl = MagicCircleNBTHelper.getSpecialLevel(stack);

            Vec3 pos = player.position().add(0, 0.05, 0);
            MagicCircleManager.addEffect(
                    MagicCircleFactory.create(circleType, serverLevel, pos, powerMult, durationMult, specialLvl));

            level.playSound(null, player.blockPosition(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS,
                    1.0F, circleType.soundPitch);

            player.getCooldowns().addCooldown(this, circleType.cooldown);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
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
        tooltip.add(Component.translatable(circleType.getTooltipKey("desc1"))
                .withStyle(circleType.primaryFormat));
        tooltip.add(Component.translatable(circleType.getTooltipKey("desc2"))
                .withStyle(circleType.secondaryFormat));

        int totalLvl = MagicCircleNBTHelper.getTotalLevel(stack);

        tooltip.add(Component.empty());

        int powerLvl = MagicCircleNBTHelper.getPowerLevel(stack);
        int durationLvl = MagicCircleNBTHelper.getDurationLevel(stack);
        int efficiencyLvl = MagicCircleNBTHelper.getEfficiencyLevel(stack);

        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < MagicCircleNBTHelper.MAX_LEVEL; i++) {
            stars.append(i < Math.min(totalLvl, 15) / 3 ? "★" : "☆");
        }
        tooltip.add(Component.translatable("tooltip.transcend.mc_enhance.level", stars.toString(), totalLvl)
                .withStyle(ChatFormatting.GOLD));

        tooltip.add(buildStatLine("tooltip.transcend.mc_enhance.power",
                powerLvl, MagicCircleNBTHelper.getPowerMultiplier(stack), ChatFormatting.RED));
        tooltip.add(buildStatLine("tooltip.transcend.mc_enhance.duration",
                durationLvl, MagicCircleNBTHelper.getDurationMultiplier(stack), ChatFormatting.AQUA));
        tooltip.add(buildEfficiencyLine(efficiencyLvl));

        int specialLvl = MagicCircleNBTHelper.getSpecialLevel(stack);
        tooltip.add(buildSpecialLine(specialLvl));

        int cost = MagicCircleNBTHelper.getCrystalCost(stack);
        tooltip.add(Component.translatable("tooltip.transcend.mc_enhance.cost", cost)
                .withStyle(ChatFormatting.LIGHT_PURPLE));

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable(circleType.getTooltipKey("usage"))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.transcend.mc_enhance.shift_usage")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private MutableComponent buildStatLine(String key, int lvl, float mult, ChatFormatting color) {
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < MagicCircleNBTHelper.MAX_LEVEL; i++) {
            bar.append(i < lvl ? "█" : "░");
        }
        int percent = Math.round((mult - 1.0F) * 100);
        String bonus = percent > 0 ? " +" + percent + "%" : "";
        return Component.literal("  ")
                .append(Component.translatable(key).withStyle(color))
                .append(Component.literal(" " + toRoman(lvl) + " [" + bar + "]" + bonus)
                        .withStyle(ChatFormatting.GRAY));
    }

    private MutableComponent buildEfficiencyLine(int lvl) {
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < MagicCircleNBTHelper.MAX_LEVEL; i++) {
            bar.append(i < lvl ? "█" : "░");
        }
        String bonus = lvl > 0 ? " -" + lvl : "";
        return Component.literal("  ")
                .append(Component.translatable("tooltip.transcend.mc_enhance.efficiency")
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" " + toRoman(lvl) + " [" + bar + "]" + bonus)
                        .withStyle(ChatFormatting.GRAY));
    }

    private MutableComponent buildSpecialLine(int lvl) {
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < MagicCircleNBTHelper.MAX_LEVEL; i++) {
            bar.append(i < lvl ? "█" : "░");
        }
        return Component.literal("  ")
                .append(Component.translatable(circleType.getSpecialNameKey())
                        .withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal(" " + toRoman(lvl) + " [" + bar + "]")
                        .withStyle(ChatFormatting.GRAY));
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 0 -> "0";
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }
}
