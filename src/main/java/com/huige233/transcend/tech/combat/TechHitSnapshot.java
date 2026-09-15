package com.huige233.transcend.tech.combat;

import com.huige233.transcend.combat.damage.DamageClassification;
import com.huige233.transcend.entity.projectile.ParticleBolt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;


/** 为一次受伤事件捕获伤害分类、弹体穿透参数和协同护盾穿透强度，供所有装备护盾共用。 */
public record TechHitSnapshot(DamageClassification classification, PenetrationProfile penetration,
                             int shieldStrength) {
    public static TechHitSnapshot capture(LivingEntity target, DamageSource source) {
        DamageClassification classification = DamageClassification.snapshot(source);
        PenetrationProfile profile = source.getDirectEntity() instanceof ParticleBolt bolt
                ? bolt.penetration() : PenetrationProfile.none();
        int strength = profile.shieldStrength();
        if (source.getDirectEntity() instanceof ParticleBolt bolt && strength > 0) {
            strength = CoordinatedPenetration.strength(target, bolt, strength, Math.min(7, strength),
                    target.level().getGameTime());
        }
        return new TechHitSnapshot(classification, profile, strength);
    }
}
