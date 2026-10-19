package com.huige233.transcend.tech.shield;

import com.huige233.transcend.tech.rank.TechRank;

/** 发射器承载、电容续航和通用减伤随科技等级提高；模块仅在 B 级以后开放。 */
public record TechShieldTierProfile(float capacity, float reduction, int slots, int defenseTier,
                                    boolean allTypes, float referenceShotDamage) {
    public static TechShieldTierProfile of(TechRank rank) {
        return switch (rank) {
            case E -> new TechShieldTierProfile(200, 0.00F, 0, 3, false, 8);
            case D -> new TechShieldTierProfile(500, 0.05F, 0, 4, false, 12);
            case C -> new TechShieldTierProfile(1200, 0.10F, 0, 5, false, 20);
            case B -> new TechShieldTierProfile(3200, 0.20F, 2, 6, false, 32);
            case A -> new TechShieldTierProfile(9000, 0.30F, 3, 7, false, 48);
            case S -> new TechShieldTierProfile(20000, 0.40F, 4, 8, true, 72);
            case EX -> new TechShieldTierProfile(30000, 0.50F, 5, 9, true, 110);
        };
    }
}
