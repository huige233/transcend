package com.huige233.transcend.spell.config;

import com.huige233.transcend.ascension.PlayerAscensionData;
import com.huige233.transcend.spell.SpellCarrier;
import com.huige233.transcend.spell.SpellEffect;
import com.huige233.transcend.spell.SpellElement;
import com.huige233.transcend.spell.SpellTierHelper;
import com.huige233.transcend.spell.data.SpellDefinition;
import com.huige233.transcend.spell.data.SpellDefinitionRegistry;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

/** 法术配置规则工具类。 */
public final class SpellConfigurationRules {
    private static final float EFFECT_COUNT_GROWTH = 1.25F;
    private static final float REPEAT_GROWTH = 1.50F;
    private static final EnumSet<SpellCarrier> PROJECTILE_CARRIERS = EnumSet.of(
            SpellCarrier.ORB, SpellCarrier.ARROW, SpellCarrier.VORTEX, SpellCarrier.TRAP);
    private static final EnumSet<SpellEffect> PROJECTILE_ONLY_EFFECTS = EnumSet.of(
            SpellEffect.PIERCING, SpellEffect.SPLIT, SpellEffect.HOMING, SpellEffect.MULTISHOT);
    private static final float[] TIER_COEFFICIENTS = {
            1.00F, 1.15F, 1.30F, 1.50F, 1.75F, 2.05F,
            2.40F, 2.80F, 3.25F, 3.75F, 4.30F, 5.00F
    };

    private SpellConfigurationRules() {}

    public enum Result {
        VALID,
        UNKNOWN_BASE_SPELL,
        TOO_MANY_EFFECTS,
        EFFECT_SLOTS_LOCKED,
        DUPLICATE_EFFECT,
        INCOMPATIBLE_CARRIER,
        SPELL_TIER_LOCKED
    }

    public record Computation(int tier, int manaCost) {}

    public record MultishotPlan(int projectileCount, float powerPerProjectile) {}

    public record SplitPlan(int childCount) {}

    public static Computation compute(SpellCarrier carrier, SpellElement element, List<SpellEffect> effects) {
        if (carrier == null || element == null || effects == null || effects.size() > ConfiguredSpell.MAX_EFFECTS) {
            return new Computation(12, Integer.MAX_VALUE);
        }
        int tier = Math.max(SpellTierHelper.getCarrierTier(carrier), SpellTierHelper.getElementTier(element));
        EnumMap<SpellEffect, Integer> counts = new EnumMap<>(SpellEffect.class);
        float weightSum = 0.0F;
        for (SpellEffect effect : effects) {
            if (effect == null) return new Computation(12, Integer.MAX_VALUE);
            tier = Math.max(tier, SpellTierHelper.getEffectTier(effect));
            int occurrence = counts.merge(effect, 1, Integer::sum);
            float weight = Math.max(0.1F, effect.getExtraManaCost() / 10.0F);
            weightSum += weight * (float) Math.pow(REPEAT_GROWTH, occurrence - 1);
        }
        tier = Math.max(tier, effectLoadTier(effects.size()));
        tier = Math.max(1, Math.min(12, tier));

        float countGrowth = effects.size() <= 1
                ? 1.0F
                : (float) Math.pow(EFFECT_COUNT_GROWTH, effects.size() - 1);
        double rawCost = Math.ceil(Math.max(1, element.getManaCost())
                * tierCoefficient(tier) * (1.0F + weightSum) * countGrowth);
        int manaCost = rawCost >= Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(1, (int) rawCost);
        return new Computation(tier, manaCost);
    }

    public static List<SpellEffect> canonicalEffects(List<SpellEffect> effects) {
        if (effects == null || effects.isEmpty()) return List.of();
        return effects.stream().filter(java.util.Objects::nonNull)
                .limit(ConfiguredSpell.MAX_EFFECTS).sorted().toList();
    }

    public static float amplifyMultiplier(List<SpellEffect> effects) {
        long stacks = effects == null ? 0 : effects.stream().filter(e -> e == SpellEffect.AMPLIFY).count();
        return (float) Math.pow(1.5F, stacks);
    }

    public static MultishotPlan multishotPlan(List<SpellEffect> effects) {
        long stacks = effects == null ? 0 : effects.stream().filter(e -> e == SpellEffect.MULTISHOT).count();
        if (stacks <= 0) return new MultishotPlan(1, 1.0F);
        int count = Math.min(7, 1 + (int) stacks * 2);
        float totalPower = stacks == 1 ? 1.5F : stacks == 2 ? 1.75F : 2.0F;
        return new MultishotPlan(count, totalPower / count);
    }

    public static int piercingExtraPenetrations(List<SpellEffect> effects) {
        return effects == null ? 0 : (int) effects.stream().filter(e -> e == SpellEffect.PIERCING).count();
    }

    public static SplitPlan splitPlan(List<SpellEffect> effects) {
        long stacks = effects == null ? 0 : effects.stream().filter(e -> e == SpellEffect.SPLIT).count();
        return new SplitPlan(Math.min(6, (int) stacks * 3));
    }

    public static boolean isEffectCompatible(SpellCarrier carrier, SpellEffect effect) {
        return carrier != null && effect != null
                && (!PROJECTILE_ONLY_EFFECTS.contains(effect) || PROJECTILE_CARRIERS.contains(carrier));
    }

    public static boolean areEffectsCompatible(SpellCarrier carrier, List<SpellEffect> effects) {
        return effects != null && effects.stream().allMatch(effect -> isEffectCompatible(carrier, effect));
    }

    public static Result validate(ConfiguredSpell spell, PlayerAscensionData playerData) {
        SpellDefinition base = SpellDefinitionRegistry.getInstance().get(spell.baseSpellId()).orElse(null);
        if (base == null) return Result.UNKNOWN_BASE_SPELL;
        if (spell.effects().size() > ConfiguredSpell.MAX_EFFECTS) return Result.TOO_MANY_EFFECTS;
        if (spell.effects().size() > playerData.getMaxSpellEffectSlots()) return Result.EFFECT_SLOTS_LOCKED;

        EnumMap<SpellEffect, Integer> counts = new EnumMap<>(SpellEffect.class);
        for (SpellEffect effect : spell.effects()) {
            if (!isEffectCompatible(base.carrier(), effect)) return Result.INCOMPATIBLE_CARRIER;
            int count = counts.merge(effect, 1, Integer::sum);
            if (count > 1 && !effect.isRepeatable()) return Result.DUPLICATE_EFFECT;
        }

        return compute(base.carrier(), spell.element(), spell.effects()).tier() <= playerData.getSpellTier()
                ? Result.VALID
                : Result.SPELL_TIER_LOCKED;
    }

    public static int calculateTier(ConfiguredSpell spell) {
        SpellDefinition base = SpellDefinitionRegistry.getInstance().get(spell.baseSpellId()).orElse(null);
        return base == null ? 12 : compute(base.carrier(), spell.element(), spell.effects()).tier();
    }

    public static int calculateManaCost(ConfiguredSpell spell) {
        SpellDefinition base = SpellDefinitionRegistry.getInstance().get(spell.baseSpellId()).orElse(null);
        if (base == null) return Integer.MAX_VALUE;

        return compute(base.carrier(), spell.element(), spell.effects()).manaCost();
    }

    public static int effectLoadTier(int effectCount) {
        return Math.max(1, Math.min(12, 1 + (Math.max(0, effectCount) + 1) / 2));
    }

    public static float tierCoefficient(int tier) {
        return TIER_COEFFICIENTS[Math.max(1, Math.min(12, tier)) - 1];
    }

}
