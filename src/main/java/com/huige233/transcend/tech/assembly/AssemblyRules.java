package com.huige233.transcend.tech.assembly;


/** 计算工艺熟练度等级、递减经验收益、等级上限、失败概率和材料回收数量。 */
public final class AssemblyRules {
    public static final int MAX_LEVEL = 100;
    public static final int MAX_XP = xpForLevel(MAX_LEVEL);

    private AssemblyRules() {}

    public static int xpForLevel(int level) {
        int bounded = Math.max(0, Math.min(MAX_LEVEL, level));
        return 25 * bounded * (bounded + 1);
    }

    public static int level(int xp) {
        int result = 0;
        while (result < MAX_LEVEL && xp >= xpForLevel(result + 1)) result++;
        return result;
    }

    
    public static int baseGain(int tier) {
        return Math.max(1, Math.min(140, Math.max(1, Math.min(7, tier)) * 20));
    }

    
    public static int gain(int xp, int tier) {
        return gain(xp, tier, 0);
    }

    public static int gain(int xp, int tier, int highGainCount) {
        int level = level(xp);
        int softCap = 8 + Math.max(1, Math.min(7, tier)) * 4;
        double factor = highGainCount <= softCap ? 1.0D
                : Math.max(0.1D, 1.0D / (1.0D + (highGainCount - softCap) * 0.25D));
        int amount = (int) Math.floor(baseGain(tier) * factor);
        int levelCap = Math.min(MAX_LEVEL, Math.max(1, Math.min(7, tier)) * 15);
        return Math.max(0, Math.min(amount, xpForLevel(levelCap) - Math.max(0, xp)));
    }

    public static double failureChance(double base, int xp) {
        return Math.max(0.0D, Math.min(1.0D, base) - level(xp) * 0.001D);
    }

    public static int salvage(int baseCount) {
        return (int) ((long) Math.max(0, baseCount) * 15 / 100);
    }
}
