package com.huige233.transcend.spell.config;

import com.huige233.transcend.spell.SpellCarrier;
import com.huige233.transcend.spell.SpellEffect;
import com.huige233.transcend.spell.SpellElement;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SpellConfigurationRulesTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void tierCoefficientCurveIsExactAndBounded() {
        float[] expected = {1.0F, 1.15F, 1.30F, 1.50F, 1.75F, 2.05F,
                2.40F, 2.80F, 3.25F, 3.75F, 4.30F, 5.00F};
        for (int tier = 1; tier <= expected.length; tier++) {
            assertEquals(expected[tier - 1], SpellConfigurationRules.tierCoefficient(tier), EPSILON);
        }
        assertEquals(1.0F, SpellConfigurationRules.tierCoefficient(0), EPSILON);
        assertEquals(5.0F, SpellConfigurationRules.tierCoefficient(99), EPSILON);
    }

    @Test
    void tierIsMaximumComponentAndEffectLoadTier() {
        assertEquals(6, SpellConfigurationRules.compute(
                SpellCarrier.ORB, SpellElement.CHAOS, List.of(SpellEffect.MARK)).tier());
        assertEquals(4, SpellConfigurationRules.compute(
                SpellCarrier.ORB, SpellElement.FIRE,
                List.of(SpellEffect.MARK, SpellEffect.ROOT, SpellEffect.BLIGHT,
                        SpellEffect.HEALING, SpellEffect.SHIELD)).tier());
    }

    @Test
    void costUsesTierCurveCountGrowthAndRepeatedStackGrowth() {
        assertEquals(2, SpellConfigurationRules.compute(
                SpellCarrier.ORB, SpellElement.FIRE, List.of()).manaCost());
        assertEquals(4, SpellConfigurationRules.compute(
                SpellCarrier.ORB, SpellElement.FIRE, List.of(SpellEffect.AMPLIFY)).manaCost());
        assertEquals(6, SpellConfigurationRules.compute(
                SpellCarrier.ORB, SpellElement.FIRE,
                List.of(SpellEffect.AMPLIFY, SpellEffect.AMPLIFY)).manaCost());
    }

    @Test
    void costAndTierIgnoreInputOrderWhileExecutionPreservesInsertionOrder() {
        List<SpellEffect> first = List.of(SpellEffect.MARK, SpellEffect.AMPLIFY, SpellEffect.EXPLOSION);
        List<SpellEffect> second = List.of(SpellEffect.EXPLOSION, SpellEffect.MARK, SpellEffect.AMPLIFY);
        assertEquals(SpellConfigurationRules.compute(SpellCarrier.ARROW, SpellElement.WATER, first),
                SpellConfigurationRules.compute(SpellCarrier.ARROW, SpellElement.WATER, second));
        assertEquals(first, SpellConfigurationRules.canonicalEffects(first),
                "configured effects must execute in insertion order");
        assertEquals(second, SpellConfigurationRules.canonicalEffects(second),
                "a different insertion order must remain observably different");
        assertNotEquals(SpellConfigurationRules.canonicalEffects(first),
                SpellConfigurationRules.canonicalEffects(second));
    }

    @Test
    void amplifyAppliesOncePerStack() {
        assertEquals(1.0F, SpellConfigurationRules.amplifyMultiplier(List.of()), EPSILON);
        assertEquals(1.5F, SpellConfigurationRules.amplifyMultiplier(List.of(SpellEffect.AMPLIFY)), EPSILON);
        assertEquals(3.375F, SpellConfigurationRules.amplifyMultiplier(List.of(
                SpellEffect.AMPLIFY, SpellEffect.AMPLIFY, SpellEffect.AMPLIFY)), EPSILON);
    }

    @Test
    void multishotCapsAtSevenAndNormalizesTotalPower() {
        assertMultishot(1, 1.0F, List.of());
        assertMultishot(3, 1.5F, List.of(SpellEffect.MULTISHOT));
        assertMultishot(5, 1.75F, List.of(SpellEffect.MULTISHOT, SpellEffect.MULTISHOT));
        assertMultishot(7, 2.0F, List.of(
                SpellEffect.MULTISHOT, SpellEffect.MULTISHOT, SpellEffect.MULTISHOT,
                SpellEffect.MULTISHOT));
    }

    @Test
    void enumIsExactlyTheCanonicalSeventeenAndOldIdsMigrate() {
        assertEquals(Set.of(SpellEffect.AMPLIFY, SpellEffect.PIERCING, SpellEffect.SPLIT,
                SpellEffect.HOMING, SpellEffect.HEALING, SpellEffect.SHIELD, SpellEffect.EXPLOSION,
                SpellEffect.LIFESTEAL, SpellEffect.CHAIN_LIGHTNING, SpellEffect.MULTISHOT,
                SpellEffect.SLOWFIELD, SpellEffect.MARK, SpellEffect.ROOT, SpellEffect.BLIGHT,
                SpellEffect.CURSE, SpellEffect.OVERLOAD, SpellEffect.SHATTER), Set.of(SpellEffect.values()));
        assertEquals(17, SpellEffect.values().length);
        assertEquals(SpellEffect.SPLIT, SpellEffect.getById("bounce"));
        assertEquals(SpellEffect.EXPLOSION, SpellEffect.getById("delayed"));
        assertEquals(SpellEffect.AMPLIFY, SpellEffect.getById("quickcast"));
        assertEquals(SpellEffect.ROOT, SpellEffect.getById("gravity_well"));
        assertEquals(SpellEffect.MULTISHOT, SpellEffect.getById("echo"));
        assertEquals(SpellEffect.PIERCING, SpellEffect.getById("armor_break"));
        assertEquals(SpellEffect.BLIGHT, SpellEffect.getById("lingering"));
        assertEquals(SpellEffect.LIFESTEAL, SpellEffect.getById("devour"));
        assertEquals(SpellEffect.SHIELD, SpellEffect.getById("absorb"));
        assertEquals(SpellEffect.SHIELD, SpellEffect.getById("reflect"));
        assertEquals(SpellEffect.BLIGHT, SpellEffect.getById("weaken"));
        assertEquals(SpellEffect.OVERLOAD, SpellEffect.getById("unstable"));
        assertEquals(SpellEffect.HOMING, SpellEffect.getById("summon_wisp"));
        assertEquals(SpellEffect.SHIELD, SpellEffect.getById("summon_guardian"));
    }

    @Test
    void legacyCarrierAndElementIdsResolveToCanonicalComponents() {
        assertAll("legacy carrier and element aliases",
                () -> assertAll("legacy carrier aliases",
                        () -> assertEquals(SpellCarrier.TRAP, SpellCarrier.getById("spike")),
                        () -> assertEquals(SpellCarrier.DASH, SpellCarrier.getById("teleport")),
                        () -> assertEquals(SpellCarrier.NOVA, SpellCarrier.getById("ring")),
                        () -> assertEquals(SpellCarrier.BEAM, SpellCarrier.getById("breath")),
                        () -> assertEquals(SpellCarrier.NOVA, SpellCarrier.getById("ground")),
                        () -> assertEquals(SpellCarrier.ORB, SpellCarrier.getById("summon"))),
                () -> assertAll("legacy element aliases",
                        () -> assertEquals(SpellElement.METAL, SpellElement.getById("holy")),
                        () -> assertEquals(SpellElement.METAL, SpellElement.getById("arcane")),
                        () -> assertEquals(SpellElement.METAL, SpellElement.getById("light")),
                        () -> assertEquals(SpellElement.METAL, SpellElement.getById("sonic")),
                        () -> assertEquals(SpellElement.WOOD, SpellElement.getById("wind")),
                        () -> assertEquals(SpellElement.WOOD, SpellElement.getById("nature")),
                        () -> assertEquals(SpellElement.WOOD, SpellElement.getById("blood")),
                        () -> assertEquals(SpellElement.WATER, SpellElement.getById("ice")),
                        () -> assertEquals(SpellElement.FIRE, SpellElement.getById("thunder")),
                        () -> assertEquals(SpellElement.FIRE, SpellElement.getById("lightning")),
                        () -> assertEquals(SpellElement.EARTH, SpellElement.getById("poison")),
                        () -> assertEquals(SpellElement.EARTH, SpellElement.getById("dark")),
                        () -> assertEquals(SpellElement.EARTH, SpellElement.getById("acid")),
                        () -> assertEquals(SpellElement.CHAOS, SpellElement.getById("void")),
                        () -> assertEquals(SpellElement.CHAOS, SpellElement.getById("time")),
                        () -> assertEquals(SpellElement.CHAOS, SpellElement.getById("space")),
                        () -> assertEquals(SpellElement.CHAOS, SpellElement.getById("eldritch"))));
    }

    @Test
    void unknownNonEmptyComponentIdsNeverBecomeValidFallbacks() {
        String unknown = "not_a_spell_component";
        assertAll("unknown IDs",
                () -> assertNull(SpellCarrier.getById(unknown), "unknown carrier must be rejected"),
                () -> assertNull(SpellElement.getById(unknown), "unknown element must be rejected"),
                () -> assertNull(SpellEffect.getById(unknown), "unknown effect must be rejected"));
    }

    @Test
    void projectileOnlyCompatibilityIsEnforced() {
        for (SpellEffect effect : List.of(SpellEffect.PIERCING, SpellEffect.SPLIT,
                SpellEffect.HOMING, SpellEffect.MULTISHOT)) {
            assertTrue(SpellConfigurationRules.isEffectCompatible(SpellCarrier.ORB, effect));
            assertTrue(SpellConfigurationRules.isEffectCompatible(SpellCarrier.ARROW, effect));
            assertTrue(SpellConfigurationRules.isEffectCompatible(SpellCarrier.VORTEX, effect));
            assertTrue(SpellConfigurationRules.isEffectCompatible(SpellCarrier.TRAP, effect));
            assertFalse(SpellConfigurationRules.isEffectCompatible(SpellCarrier.BEAM, effect));
        }
        for (SpellEffect effect : SpellEffect.values()) {
            if (!List.of(SpellEffect.PIERCING, SpellEffect.SPLIT, SpellEffect.HOMING,
                    SpellEffect.MULTISHOT).contains(effect)) {
                for (SpellCarrier carrier : SpellCarrier.values()) {
                    assertTrue(SpellConfigurationRules.isEffectCompatible(carrier, effect));
                }
            }
        }
    }

    @Test
    void pierceAndSplitPlansAreBounded() {
        assertEquals(3, SpellConfigurationRules.piercingExtraPenetrations(List.of(
                SpellEffect.PIERCING, SpellEffect.PIERCING, SpellEffect.PIERCING)));
        assertEquals(3, SpellConfigurationRules.splitPlan(List.of(SpellEffect.SPLIT)).childCount());
        assertEquals(6, SpellConfigurationRules.splitPlan(List.of(
                SpellEffect.SPLIT, SpellEffect.SPLIT, SpellEffect.SPLIT)).childCount());
    }

    private static void assertMultishot(int count, float totalPower, List<SpellEffect> effects) {
        SpellConfigurationRules.MultishotPlan plan = SpellConfigurationRules.multishotPlan(effects);
        assertEquals(count, plan.projectileCount());
        assertEquals(totalPower, plan.projectileCount() * plan.powerPerProjectile(), EPSILON);
    }
}
