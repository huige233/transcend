package com.huige233.transcend.entity.boss;

final class BossSpellTier {
    private BossSpellTier() {}

    static int calculate(int baseTier, int phaseOrdinal) {
        return Math.min(12, baseTier + phaseOrdinal);
    }
}
