package com.huige233.transcend.client.magic;

import net.minecraft.world.item.ItemStack;

public class MagicCircleNBTHelper {

    private static final String KEY_POWER = "mc_power";
    private static final String KEY_DURATION = "mc_duration";
    private static final String KEY_EFFICIENCY = "mc_efficiency";
    private static final String KEY_SPECIAL = "mc_special";

    public static final int MAX_LEVEL = 5;

    private static final float[] POWER_MULTIPLIERS = {1.0F, 1.2F, 1.5F, 1.8F, 2.2F, 2.8F};
    private static final float[] DURATION_MULTIPLIERS = {1.0F, 1.5F, 2.0F, 2.8F, 3.8F, 5.0F};
    private static final int[] BASE_COST = {1, 1, 2, 2, 3, 3};
    private static final int[] EFFICIENCY_DISCOUNT = {0, 1, 1, 2, 2, 3};

    public enum EnhanceType {
        POWER(KEY_POWER),
        DURATION(KEY_DURATION),
        EFFICIENCY(KEY_EFFICIENCY),
        SPECIAL(KEY_SPECIAL);

        public final String nbtKey;

        EnhanceType(String nbtKey) {
            this.nbtKey = nbtKey;
        }
    }

    public static int getPowerLevel(ItemStack stack) {
        return stack.getOrCreateTag().getInt(KEY_POWER);
    }

    public static int getDurationLevel(ItemStack stack) {
        return stack.getOrCreateTag().getInt(KEY_DURATION);
    }

    public static int getEfficiencyLevel(ItemStack stack) {
        return stack.getOrCreateTag().getInt(KEY_EFFICIENCY);
    }

    public static int getSpecialLevel(ItemStack stack) {
        return stack.getOrCreateTag().getInt(KEY_SPECIAL);
    }

    public static int getTotalLevel(ItemStack stack) {
        return getPowerLevel(stack) + getDurationLevel(stack)
                + getEfficiencyLevel(stack) + getSpecialLevel(stack);
    }

    public static float getPowerMultiplier(ItemStack stack) {
        int lvl = Math.min(getPowerLevel(stack), MAX_LEVEL);
        return POWER_MULTIPLIERS[lvl];
    }

    public static float getDurationMultiplier(ItemStack stack) {
        int lvl = Math.min(getDurationLevel(stack), MAX_LEVEL);
        return DURATION_MULTIPLIERS[lvl];
    }

    public static int getCrystalCost(ItemStack stack) {
        int powerLvl = getPowerLevel(stack);
        int durationLvl = getDurationLevel(stack);
        int specialLvl = getSpecialLevel(stack);
        int efficiencyLvl = getEfficiencyLevel(stack);
        int baseCost = 1 + powerLvl + durationLvl + specialLvl;
        int discount = EFFICIENCY_DISCOUNT[Math.min(efficiencyLvl, MAX_LEVEL)];
        return Math.max(1, baseCost - discount);
    }

    public static int getBaseCostForLevel(int totalLevel) {
        int costIndex = Math.min(totalLevel / 3, MAX_LEVEL);
        return BASE_COST[costIndex];
    }

    public static boolean canEnhance(ItemStack stack, EnhanceType type) {
        int current = stack.getOrCreateTag().getInt(type.nbtKey);
        return current < MAX_LEVEL;
    }

    public static boolean enhance(ItemStack stack, EnhanceType type) {
        int current = stack.getOrCreateTag().getInt(type.nbtKey);
        if (current >= MAX_LEVEL) return false;
        stack.getOrCreateTag().putInt(type.nbtKey, current + 1);
        return true;
    }

    public static int getLevel(ItemStack stack, EnhanceType type) {
        return stack.getOrCreateTag().getInt(type.nbtKey);
    }
}
