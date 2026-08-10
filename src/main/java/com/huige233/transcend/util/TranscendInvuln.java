package com.huige233.transcend.util;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;

/** 无敌(免疫)工具类。 */
public final class TranscendInvuln {

    private TranscendInvuln() {
    }

    public static void apply(LivingEntity entity, boolean protectedNow) {
        if (entity == null || entity.level().isClientSide || !protectedNow) return;

        try {
            EntityDataAccessor<Float> hp = TranscendUnsafe.dataHealthId();
            if (hp != null) {
                entity.getEntityData().set(hp, entity.getMaxHealth());
            }
        } catch (Throwable ignored) {
        }

        TranscendUnsafe.putMcObject(entity, net.minecraft.world.entity.Entity.class,
                TranscendUnsafe.SRG_REMOVAL_REASON, null);
        entity.deathTime = 0;
        entity.hurtTime = 0;
    }
}
