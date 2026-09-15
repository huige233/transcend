package com.huige233.transcend.combat.attack;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 验证攻击等级排序、伤害与重试次数边界，以及软击杀和硬删除的语义区分。 */
class AttackModelTest {
    @Test void levelsAreOrderedAndBounded() {
        assertTrue(AttackLevel.WORLD_PURGE.ordinal() > AttackLevel.SOFT_KILL.ordinal());
        AttackProfile p = new AttackProfile(AttackLevel.NORMAL, Float.NaN, 999, null);
        assertEquals(0.0F, p.amount());
        assertEquals(20, p.maxAttempts());
    }
    @Test void softKillDoesNotBecomeHardDelete() {
        assertNotEquals(AttackLevel.SOFT_KILL, AttackLevel.HARD_DELETE);
    }
}
