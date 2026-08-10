package com.huige233.transcend.entity.boss;

/** 首领法术等级计算工具类。 */
final class BossSpellTier {
    private BossSpellTier() {}

    static int calculate(int baseTier, int phaseOrdinal) {
        return Math.min(12, baseTier + phaseOrdinal);
    }
}
