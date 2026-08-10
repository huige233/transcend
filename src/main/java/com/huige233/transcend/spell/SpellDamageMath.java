package com.huige233.transcend.spell;

/** 法术伤害计算工具类。 */
public final class SpellDamageMath {
    public static final float BASE_RESISTANCE = 0.05F;
    public static final float CHAOS_BASE_RESISTANCE = 0.01F;
    public static final int IMMUNITY_TIER_GAP = 3;

    private static final float NORMAL_FIRST_LAYER_REDUCTION = 0.55F;
    private static final float AURA_FIRST_LAYER_REDUCTION = 0.80F;
    private static final float REDUCTION_PER_TIER = 0.10F;
    private static final float MAX_LAYER_REDUCTION = 0.90F;
    private static final float MIN_RESISTANCE_MULTIPLIER = 0.10F;
    private static final float MAX_RESISTANCE_IGNORE = 0.30F;
    private static final float GENERATION_BASE_MULTIPLIER = 1.50F;
    private static final float MAX_REACTION_BONUS = 4.00F;

    private SpellDamageMath() {}

    public record Context(float rawDamage, int defenderTier, int spellTier, boolean auraGuardActive,
                          float aggregatedResistance, float resistanceIgnore) {}

    public record Calculation(float tierAdjustedDamage, float effectiveResistance, float finalDamage) {}

    public static Calculation calculate(Context context) {
        float tierAdjusted = applyTierSuppression(context.rawDamage(), context.defenderTier(),
                context.spellTier(), context.auraGuardActive());
        float effectiveResistance = effectiveElementalResistance(
                context.aggregatedResistance(), context.resistanceIgnore());
        return new Calculation(tierAdjusted, effectiveResistance,
                applyElementalResistance(tierAdjusted, effectiveResistance));
    }

    public static float tierSuppressionMultiplier(int defenderTier, int spellTier, boolean auraGuardActive) {
        int gap = defenderTier - spellTier;
        if (gap <= 0) return 1.0F;
        if (gap > IMMUNITY_TIER_GAP) return 0.0F;

        float firstLayer = auraGuardActive
                ? AURA_FIRST_LAYER_REDUCTION
                : NORMAL_FIRST_LAYER_REDUCTION;
        float multiplier = 1.0F;
        for (int layer = 0; layer < gap; layer++) {
            float reduction = Math.min(MAX_LAYER_REDUCTION, firstLayer + REDUCTION_PER_TIER * layer);
            multiplier *= 1.0F - reduction;
        }
        return multiplier;
    }

    public static float elementalResistanceMultiplier(float effectiveResistance) {
        if (effectiveResistance < BASE_RESISTANCE) {
            float vulnerability = clamp((BASE_RESISTANCE - effectiveResistance) / 0.04F, 0.0F, 1.0F);
            return 1.0F + vulnerability;
        }
        return Math.max(MIN_RESISTANCE_MULTIPLIER, (1.0F - effectiveResistance) / 0.95F);
    }

    public static float applyTierSuppression(float damage, int defenderTier, int spellTier,
                                             boolean auraGuardActive) {
        return Math.max(0.0F, damage) * tierSuppressionMultiplier(defenderTier, spellTier, auraGuardActive);
    }

    public static float applyElementalResistance(float damage, float effectiveResistance) {
        return Math.max(0.0F, damage) * elementalResistanceMultiplier(effectiveResistance);
    }

    public static float effectiveElementalResistance(float defenderResistance, float resistanceIgnore) {
        float boundedResistance = Float.isFinite(defenderResistance) ? defenderResistance : BASE_RESISTANCE;
        float boundedIgnore = Float.isFinite(resistanceIgnore)
                ? clamp(resistanceIgnore, 0.0F, MAX_RESISTANCE_IGNORE)
                : 0.0F;
        return boundedResistance - boundedIgnore;
    }

    public static float reactionGenerationMultiplier(float reactionBonus) {
        float boundedBonus = Float.isFinite(reactionBonus)
                ? clamp(reactionBonus, 0.0F, MAX_REACTION_BONUS)
                : 0.0F;
        return GENERATION_BASE_MULTIPLIER * (1.0F + boundedBonus);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
