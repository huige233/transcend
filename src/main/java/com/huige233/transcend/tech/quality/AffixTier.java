package com.huige233.transcend.tech.quality;

import com.huige233.transcend.tech.rank.TechRank;
import java.util.Random;


/** 定义词缀强度档位，并结合装备评级的保底档位与随机上限生成词缀数值。 */
public enum AffixTier {
    TIER_1(1, 1.0F), TIER_2(2, 1.5F), TIER_3(3, 2.2F), TIER_4(4, 3.5F), TIER_5(5, 5.0F);
    public final int level; public final float mult;
    AffixTier(int level, float mult) { this.level = level; this.mult = mult; }
    public static AffixTier byLevel(int level) { return values()[Math.max(1, Math.min(5, level)) - 1]; }
    public static AffixTier byId(String id) {
        if (id == null) return TIER_1;
        String s = id.trim().toUpperCase().replace("-", "_");
        try { return s.startsWith("TIER_") ? valueOf(s) : byLevel(Integer.parseInt(s)); }
        catch (RuntimeException e) { return TIER_1; }
    }
    public static float roll(float baseValue, AffixTier tier, TechRank rank, Random rng) {
        AffixTier effective = byLevel(Math.max((tier == null ? TIER_1 : tier).level,
                (rank == null ? TechRank.C : rank).affixTierFloor));
        float ceiling = rank == null ? TechRank.C.rollCeiling : rank.rollCeiling;
        float factor = (rng == null ? new Random() : rng).nextFloat() * Math.max(0F, Math.min(1F, ceiling));
        return baseValue * effective.mult * (0.5F + 0.5F * factor);
    }
}
