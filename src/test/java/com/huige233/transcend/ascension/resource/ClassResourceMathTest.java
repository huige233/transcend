package com.huige233.transcend.ascension.resource;

import com.huige233.transcend.ascension.MageClass;
import com.huige233.transcend.spell.SpellElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassResourceMathTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void canonicalClassMatchUsesCanonicalElement() {
        assertTrue(ClassResourceMath.isCanonicalClassMatch(MageClass.PYROMANCER, SpellElement.FIRE));
        assertTrue(ClassResourceMath.isCanonicalClassMatch(MageClass.ABYSSWALKER, SpellElement.CHAOS));
        assertFalse(ClassResourceMath.isCanonicalClassMatch(MageClass.CRYOMANCER, SpellElement.METAL));
    }

    @Test
    void passiveDamageMultipliersAreElementAndClassSpecific() {
        assertEquals(1.6F, ClassResourceMath.spellDamageMultiplier(
                MageClass.PYROMANCER, SpellElement.FIRE, 100.0F), EPSILON);
        assertEquals(1.0F, ClassResourceMath.spellDamageMultiplier(
                MageClass.PYROMANCER, SpellElement.CHAOS, 100.0F), EPSILON);
        assertEquals(2.0F, ClassResourceMath.spellDamageMultiplier(
                MageClass.ABYSSWALKER, SpellElement.CHAOS, 0.0F), EPSILON);
        assertEquals(1.0F, ClassResourceMath.spellDamageMultiplier(
                MageClass.ABYSSWALKER, SpellElement.CHAOS, 100.0F), EPSILON);
    }

    @Test
    void finiteClampBoundsCorruptAndExtremeValues() {
        assertEquals(0.0F, ClassResourceMath.clampFinite(Float.NaN, 100.0F), EPSILON);
        assertEquals(0.0F, ClassResourceMath.clampFinite(Float.POSITIVE_INFINITY, 100.0F), EPSILON);
        assertEquals(0.0F, ClassResourceMath.clampFinite(-5.0F, 100.0F), EPSILON);
        assertEquals(100.0F, ClassResourceMath.clampFinite(125.0F, 100.0F), EPSILON);
        assertEquals(8.0F, ClassResourceMath.clampFinite(100.0F, 8.0F), EPSILON);
    }

    @Test
    void thresholdCrossingDoesNotPollOrRetriggerAboveThreshold() {
        assertTrue(ClassResourceMath.crossedThreshold(79.9F, 80.0F, 80.0F));
        assertFalse(ClassResourceMath.crossedThreshold(80.0F, 90.0F, 80.0F));
        assertFalse(ClassResourceMath.crossedThreshold(Float.NaN, 90.0F, 80.0F));
    }

    @Test
    void groundMovementRejectsAirborneSpectatorAndTeleportLikeDistance() {
        assertTrue(ClassResourceMath.isGroundMovement(0.06D, true, false, 0.05D));
        assertTrue(ClassResourceMath.isGroundMovement(1.5D, true, false, 0.02D));
        assertFalse(ClassResourceMath.isGroundMovement(1.5001D, true, false, 0.02D));
        assertFalse(ClassResourceMath.isGroundMovement(0.5D, false, false, 0.02D));
        assertFalse(ClassResourceMath.isGroundMovement(0.5D, true, true, 0.02D));
        assertFalse(ClassResourceMath.isGroundMovement(Double.NaN, true, false, 0.02D));
    }
}
