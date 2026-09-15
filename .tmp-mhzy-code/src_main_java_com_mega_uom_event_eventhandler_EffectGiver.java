package com.mega.uom.event.eventhandler;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class EffectGiver {
    public static void netherStarAttackEffect(Entity target) {
        if (target instanceof LivingEntity livingEntity)
            livingEntity.addEffect(new MobEffectInstance(MobEffects.WITHER, 100 - 1, 2 - 1));
    }
}
