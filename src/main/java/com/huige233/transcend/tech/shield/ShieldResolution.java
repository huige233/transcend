package com.huige233.transcend.tech.shield;


/** 汇总护盾结算的请求、吸收与剩余伤害、能量消耗及参与、耗尽和被穿透的层数。 */
public record ShieldResolution(float requestedDamage, float absorbedDamage, float remainingDamage,
                               float shieldEnergySpent, int participatingLayers, int depletedLayers,
                               int penetratedLayers) {
    public ShieldResolution(float requestedDamage, float absorbedDamage, float remainingDamage,
                            float shieldEnergySpent, int participatingLayers, int depletedLayers) {
        this(requestedDamage, absorbedDamage, remainingDamage, shieldEnergySpent,
                participatingLayers, depletedLayers, 0);
    }
    public boolean absorbedAny() {
        return absorbedDamage > 0.0F;
    }

    public boolean fullyAbsorbed() {
        return remainingDamage <= 0.0001F;
    }
}
