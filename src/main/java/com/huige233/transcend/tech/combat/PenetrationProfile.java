package com.huige233.transcend.tech.combat;


/** 保存单次射击的护甲、护盾、首领穿透强度及经限幅的最大生命百分比伤害。 */
public record PenetrationProfile(int armorStrength, int shieldStrength, int bossStrength,
                                  float maxHealthDamagePercent) {
    public PenetrationProfile {
        armorStrength = Math.max(0, armorStrength);
        shieldStrength = Math.max(0, shieldStrength);
        bossStrength = Math.max(0, bossStrength);
        maxHealthDamagePercent = Math.min(PenetrationPolicy.MAX_HEALTH_DAMAGE_PERCENT,
                CombatNumbers.nonNegative(maxHealthDamagePercent));
    }

    public static PenetrationProfile none() {
        return new PenetrationProfile(0, 0, 0, 0.0F);
    }

    public int strengthForShield() {
        return shieldStrength;
    }

    public boolean hasDirectPercentDamage() {
        return bossStrength > 0 && maxHealthDamagePercent > 0.0F;
    }
}
