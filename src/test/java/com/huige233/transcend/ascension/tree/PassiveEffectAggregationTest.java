package com.huige233.transcend.ascension.tree;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PassiveEffectAggregationTest {
    @Test
    void dodgeAddsStatAndPassivesThenCapsOnce() {
        List<PassiveEffect> passives = List.of(
                new PassiveEffect.Dodge(0.04F), new PassiveEffect.Dodge(0.07F));

        assertEquals(0.14F, PassiveEffect.aggregateDodgeChance(0.03F, passives), 0.0001F);
        assertEquals(0.15F, PassiveEffect.aggregateDodgeChance(0.10F, passives), 0.0001F);
    }

    @Test
    void passiveDamageReductionAndFreeCastAreAdditiveAndCapped() {
        List<PassiveEffect> passives = List.of(
                new PassiveEffect.DamageReduction(0.12F),
                new PassiveEffect.DamageReduction(0.25F),
                new PassiveEffect.ManaFreeCast(0.08F),
                new PassiveEffect.ManaFreeCast(0.15F));

        assertEquals(0.30F, PassiveEffect.aggregateDamageReduction(passives), 0.0001F);
        assertEquals(0.20F, PassiveEffect.aggregateFreeCastChance(passives), 0.0001F);
    }

    @Test
    void executeSelectsSingleStrongestEligibleBonus() {
        List<PassiveEffect> passives = List.of(
                new PassiveEffect.ExecuteThreshold(0.30F, 0.15F),
                new PassiveEffect.ExecuteThreshold(0.30F, 0.25F),
                new PassiveEffect.ExecuteThreshold(0.20F, 0.40F));

        assertEquals(0.25F, PassiveEffect.strongestExecuteBonus(passives, 0.25F), 0.0001F);
        assertEquals(0.40F, PassiveEffect.strongestExecuteBonus(passives, 0.15F), 0.0001F);
        assertEquals(0.0F, PassiveEffect.strongestExecuteBonus(passives, 0.50F), 0.0001F);
    }

    @Test
    void xpConvertsMultipliersToAdditiveBonusesAndCapsFinalMultiplier() {
        List<PassiveEffect> passives = List.of(
                new PassiveEffect.XPMultiplier(1.5F),
                new PassiveEffect.XPMultiplier(1.5F),
                new PassiveEffect.XPMultiplier(2.0F));

        assertEquals(2.0F, PassiveEffect.aggregateKillXpMultiplier(0.0F,
                passives.subList(0, 2)), 0.0001F);
        assertEquals(2.5F, PassiveEffect.aggregateKillXpMultiplier(0.25F, passives), 0.0001F);
    }

    @Test
    void genericDamageHealingAddsSpellVampOnlyForSpellsAndCapsCombinedRate() {
        assertEquals(0.06F, PassiveEffect.aggregateDamageHealingRate(0.06F, 0.05F, false), 0.0001F);
        assertEquals(0.10F, PassiveEffect.aggregateDamageHealingRate(0.06F, 0.05F, true), 0.0001F);
    }

    @Test
    void undyingAggregationIsOrderIndependentAndConservative() {
        List<PassiveEffect> forward = List.of(
                new PassiveEffect.Undying(0.50F, 1200),
                new PassiveEffect.Undying(0.15F, 2400));
        List<PassiveEffect> reverse = List.of(forward.get(1), forward.get(0));
        PassiveEffect.UndyingPolicy expected = new PassiveEffect.UndyingPolicy(0.50F, 2400);

        assertEquals(expected, PassiveEffect.aggregateUndying(forward));
        assertEquals(expected, PassiveEffect.aggregateUndying(reverse));
    }
}
