package com.huige233.transcend.items;

import com.huige233.transcend.ModRarities;
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

public class SpellBaseItem extends Item {

    private final int tier;

    public SpellBaseItem(int tier) {
        super(new Properties().stacksTo(16));
        this.tier = tier;
        ModItems.ITEMS.add(this);
    }

    public int getTier() {
        return tier;
    }

    /**
     * Returns the power multiplier for this base tier.
     * Tier 1: 1.2x, Tier 2: 1.5x, Tier 3: 2.0x
     */
    public float getPowerMultiplier() {
        return switch (tier) {
            case 2 -> 1.5F;
            case 3 -> 2.0F;
            default -> 1.2F;
        };
    }

    /**
     * Returns the cooldown multiplier for this base tier.
     * Tier 1: 0.9x, Tier 2: 0.8x, Tier 3: 0.6x (lower = faster)
     */
    public float getCooldownMultiplier() {
        return switch (tier) {
            case 2 -> 0.8F;
            case 3 -> 0.6F;
            default -> 0.9F;
        };
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return switch (tier) {
            case 3 -> ModRarities.COSMIC;
            case 2 -> Rarity.EPIC;
            default -> Rarity.RARE;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        ChatFormatting tierColor = switch (tier) {
            case 3 -> ChatFormatting.RED;
            case 2 -> ChatFormatting.DARK_PURPLE;
            default -> ChatFormatting.BLUE;
        };

        String tierKey = switch (tier) {
            case 3 -> "tooltip.transcend.spell_base.tier3";
            case 2 -> "tooltip.transcend.spell_base.tier2";
            default -> "tooltip.transcend.spell_base.tier1";
        };

        tooltip.add(Component.translatable("tooltip.transcend.spell_base.tier",
                        Component.translatable(tierKey))
                .withStyle(tierColor));

        int powerPercent = (int) ((getPowerMultiplier() - 1.0F) * 100);
        tooltip.add(Component.translatable("tooltip.transcend.spell_base.power_bonus",
                        "+" + powerPercent + "%")
                .withStyle(ChatFormatting.GREEN));

        int cdReductionPercent = (int) ((1.0F - getCooldownMultiplier()) * 100);
        tooltip.add(Component.translatable("tooltip.transcend.spell_base.cooldown_reduction",
                        "-" + cdReductionPercent + "%")
                .withStyle(ChatFormatting.AQUA));
    }
}
