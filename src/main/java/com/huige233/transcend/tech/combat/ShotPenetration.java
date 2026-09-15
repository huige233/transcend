package com.huige233.transcend.tech.combat;


/** 将一次射击的百分比伤害预算分摊到各弹体，并应用天狼星模块的穿透参数。 */
public final class ShotPenetration {
    public static final int SIRIUS_STRENGTH = 4;
    public static final float SIRIUS_PERCENT = 10.0F;

    private ShotPenetration() {}

    public static PenetrationProfile perProjectile(PenetrationProfile base, boolean sirius, int count) {
        int projectiles = Math.max(1, count);
        if (!sirius) {
            return new PenetrationProfile(base.armorStrength(), base.shieldStrength(), base.bossStrength(),
                    base.maxHealthDamagePercent() / projectiles);
        }
        return new PenetrationProfile(Math.max(base.armorStrength(), SIRIUS_STRENGTH),
                Math.max(base.shieldStrength(), SIRIUS_STRENGTH),
                Math.max(base.bossStrength(), SIRIUS_STRENGTH), SIRIUS_PERCENT / projectiles);
    }
}
