package com.huige233.transcend.tech.combat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;


/** 约定首领防御等级与当前阶段是否允许百分比伤害的显式兼容接口。 */
public interface BossDefenseAdapter {
    int defenseTier();

    boolean allowsPercentDamage(LivingEntity target, DamageSource source);
}
