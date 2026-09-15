package com.huige233.transcend.tech.combat;


/** 统一穿透强度与防御等级的阈值规则，并计算护甲绕过和受限的最大生命百分比附伤。 */
public final class PenetrationPolicy {
    
    public static final int ORDINARY_ARMOR_TIER = 3;
    public static final int TRANSCEND_ARMOR_TIER = 10;
    public static final float MAX_HEALTH_DAMAGE_PERCENT = 20.0F;

    private PenetrationPolicy() {
    }

    public static boolean bypasses(int strength, int defenseTier) {
        return Math.max(0, strength) >= Math.max(1, defenseTier);
    }

    
    public static float directPercentDamage(PenetrationProfile profile, int defenseTier, float maxHealth) {
        if (profile == null || defenseTier < 3 || !Float.isFinite(maxHealth) || maxHealth <= 0.0F
                || !bypasses(profile.bossStrength(), defenseTier)) return 0.0F;
        return CombatNumbers.nonNegative((double) maxHealth * profile.maxHealthDamagePercent() / 100.0D);
    }

    public static float effectiveArmor(PenetrationProfile profile, int defenseTier, float armor) {
        return profile != null && defenseTier > 0 && bypasses(profile.armorStrength(), defenseTier)
                ? 0.0F : armor;
    }

    
    public static float combineDamage(float ordinary, float percentage) {
        return CombatNumbers.nonNegative((double) CombatNumbers.nonNegative(ordinary)
                + CombatNumbers.nonNegative(percentage));
    }
}
