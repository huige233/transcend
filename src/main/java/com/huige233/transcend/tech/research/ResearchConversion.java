package com.huige233.transcend.tech.research;

/** 插件提高每组材料可处理的研究批量，不降低每研究点的真实 RF 成本。 */
public final class ResearchConversion {
    public static final long BASE_POINTS = 10;
    public static final long BASE_RF = BASE_POINTS * ResearchEnergyAccount.RF_PER_POINT;
    public static final int MAX_BATCHES = 16;
    private ResearchConversion() {}
    public static long multiplier(long power) { return Math.max(1, Math.min(Long.MAX_VALUE / BASE_RF, power)); }
    public static long points(long power) { return BASE_POINTS * multiplier(power); }
    public static long energy(long power) { return BASE_RF * multiplier(power); }
    public static int batches(int blanks, int assists, long stored, long power, boolean bulk) {
        return (int) Math.min(Math.min(Math.min(Math.max(0, blanks), Math.max(0, assists)), bulk ? MAX_BATCHES : 1),
                Math.max(0, stored) / energy(power));
    }
    public static int processingSteps(long power) { return (int) Math.max(1, Math.min(64, power)); }
}
