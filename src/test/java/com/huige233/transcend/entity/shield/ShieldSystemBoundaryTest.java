package com.huige233.transcend.entity.shield;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 验证护盾伤害清洗会拒绝负数及非有限值，同时保留正常伤害数值。 */
class ShieldSystemBoundaryTest {
    @Test
    void sanitizeDamageRejectsNonFiniteAndNegativeInputs() {
        assertEquals(0.0F, ShieldSystem.sanitizeDamage(-4.0F));
        assertEquals(0.0F, ShieldSystem.sanitizeDamage(Float.NaN));
        assertEquals(0.0F, ShieldSystem.sanitizeDamage(Float.POSITIVE_INFINITY));
        assertEquals(3.5F, ShieldSystem.sanitizeDamage(3.5F));
    }
}
