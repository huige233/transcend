package com.huige233.transcend.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpellDamageMathTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void auraGuardAppliesEachCrossedTierMultiplicatively() {
        assertEquals(0.02F, SpellDamageMath.tierSuppressionMultiplier(7, 5, true), EPSILON);
        assertEquals(2.0F, SpellDamageMath.applyTierSuppression(100.0F, 7, 5, true), EPSILON);
    }

    @Test
    void normalSuppressionUsesFiftyFiveThenSixtyFivePercent() {
        assertEquals(0.1575F, SpellDamageMath.tierSuppressionMultiplier(7, 5, false), EPSILON);
        assertEquals(15.75F, SpellDamageMath.applyTierSuppression(100.0F, 7, 5, false), EPSILON);
    }

    @Test
    void spellTierGapAboveThreeIsImmune() {
        assertEquals(0.0F, SpellDamageMath.tierSuppressionMultiplier(7, 3, false), EPSILON);
        assertEquals(0.0F, SpellDamageMath.tierSuppressionMultiplier(7, 3, true), EPSILON);
    }

    @Test
    void resistanceBaselineAndVulnerabilityMatchDesign() {
        assertEquals(1.0F, SpellDamageMath.elementalResistanceMultiplier(0.05F), EPSILON);
        assertEquals(1.25F, SpellDamageMath.elementalResistanceMultiplier(0.04F), EPSILON);
        assertEquals(1.5F, SpellDamageMath.elementalResistanceMultiplier(0.03F), EPSILON);
        assertEquals(2.0F, SpellDamageMath.elementalResistanceMultiplier(0.01F), EPSILON);
        assertEquals(2.0F, SpellDamageMath.elementalResistanceMultiplier(-1.0F), EPSILON);
    }

    @Test
    void resistanceIgnoreIsSubtractiveAndBounded() {
        assertEquals(0.25F, SpellDamageMath.effectiveElementalResistance(0.40F, 0.15F), EPSILON);
        assertEquals(0.10F, SpellDamageMath.effectiveElementalResistance(0.40F, 1.00F), EPSILON);
        assertEquals(0.40F, SpellDamageMath.effectiveElementalResistance(0.40F, -0.20F), EPSILON);
    }

    @Test
    void generationReactionBonusScalesAndCapsMultiplier() {
        assertEquals(1.50F, SpellDamageMath.reactionGenerationMultiplier(0.0F), EPSILON);
        assertEquals(1.80F, SpellDamageMath.reactionGenerationMultiplier(0.2F), EPSILON);
        assertEquals(7.50F, SpellDamageMath.reactionGenerationMultiplier(9.0F), EPSILON);
        assertEquals(1.50F, SpellDamageMath.reactionGenerationMultiplier(-1.0F), EPSILON);
    }

    @Test
    void pipelineContextAggregatesResistanceAndIgnoreOnce() {
        SpellDamageMath.Calculation result = SpellDamageMath.calculate(
                new SpellDamageMath.Context(100.0F, 4, 4, false, 0.40F, 0.15F));

        assertEquals(100.0F, result.tierAdjustedDamage(), EPSILON);
        assertEquals(0.25F, result.effectiveResistance(), EPSILON);
        assertEquals(78.94737F, result.finalDamage(), EPSILON);
    }

    @Test
    void pipelineContextAppliesTierBeforeElementalResistance() {
        SpellDamageMath.Calculation result = SpellDamageMath.calculate(
                new SpellDamageMath.Context(100.0F, 5, 4, false,
                        SpellDamageMath.BASE_RESISTANCE, 0.0F));

        assertEquals(45.0F, result.tierAdjustedDamage(), EPSILON);
        assertEquals(45.0F, result.finalDamage(), EPSILON);
    }
}
