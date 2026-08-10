package com.huige233.transcend.ascension;

/** 天劫规则配置常量。 */
public final class TribulationRules {
    public static final int SESSION_TICKS = 3600;
    public static final int ROUND_INTERVAL_TICKS = 60;
    public static final int TOTAL_ROUNDS = 60;
    public static final int WARNING_TICKS = 20;
    public static final double ARENA_RADIUS = 32.0D;

    private TribulationRules() {}

    public static int deviationDurationTicks(int realmRank) {
        int rank = Math.max(1, Math.min(CultivationRealm.values().length, realmRank));
        return 6000 + (rank - 1) * 2400;
    }

    public static float peakXpPenaltyFraction(float roll) {
        return 0.15F + Math.max(0.0F, Math.min(1.0F, roll)) * 0.10F;
    }
}
