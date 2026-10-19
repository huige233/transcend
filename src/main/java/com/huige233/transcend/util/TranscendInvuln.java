package com.huige233.transcend.util;

import com.huige233.transcend.combat.protection.TranscendProtectionEngine;
import net.minecraft.world.entity.LivingEntity;

/** 在服务端直接恢复受保护实体的同步生命值，并清除受伤和死亡动画计时。 */
public final class TranscendInvuln {

    private TranscendInvuln() {
    }

    public static void apply(LivingEntity entity, boolean protectedNow) {
        if (entity == null || entity.level().isClientSide || !protectedNow) return;

        TranscendProtectionEngine.repair(entity);
    }
}
