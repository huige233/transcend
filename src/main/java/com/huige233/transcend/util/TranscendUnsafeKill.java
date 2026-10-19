package com.huige233.transcend.util;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.gameevent.GameEvent;

import java.lang.reflect.Field;


/** 通过同步生命数据和底层移除标记压制目标状态，执行清理兜底并检查实体是否仍然存在。 */
public final class TranscendUnsafeKill {

    private TranscendUnsafeKill() {
    }

    public static void catchSetTrueHealth(LivingEntity living, float value) {
        if (living == null || living.level().isClientSide) return;
        float finite = Float.isFinite(value) ? Math.max(0.0F, value) : 0.0F;
        com.huige233.transcend.combat.attack.HealthAccess.write(living, finite);
        // 只处理确定的原版生命字段；其他同步 Float/Double 可能是能量、施法进度等。
        EntityDataAccessor<Float> health = TranscendUnsafe.dataHealthId();
        if (health != null) living.getEntityData().set(health, finite);
    }

    public static void crushSyncedHealth(Entity entity) {
        crushSyncedHealth(entity, 0.0F);
    }

    public static void crushSyncedHealth(Entity entity, float value) {
        if (entity instanceof LivingEntity living) catchSetTrueHealth(living, value);
    }

    public static void deleteStyleWipe(Entity entity) {
        if (entity == null || entity.level().isClientSide) return;
        try {
            entity.setInvisible(true);
        } catch (Throwable ignored) {
        }
        try {
            entity.tickCount = 0;
        } catch (Throwable ignored) {
        }
        try {
            entity.hurtMarked = false;
        } catch (Throwable ignored) {
        }

        if (entity instanceof LivingEntity living) {
            try {
                living.removeAllEffects();
            } catch (Throwable ignored) {
            }
            living.invulnerableTime = 0;
            living.deathTime = 19;
            living.hurtTime = 0;
            try {
                living.setAbsorptionAmount(0.0F);
            } catch (Throwable ignored) {
            }
        }

        try {
            entity.setLevelCallback(EntityInLevelCallback.NULL);
        } catch (Throwable ignored) {
            try {
                Field cb = TranscendUnsafe.mcField(Entity.class, "f_146801_");
                if (cb != null && TranscendUnsafe.UNSAFE != null) {
                    TranscendUnsafe.UNSAFE.putObject(entity,
                            TranscendUnsafe.UNSAFE.objectFieldOffset(cb),
                            EntityInLevelCallback.NULL);
                }
            } catch (Throwable ignored2) {
            }
        }

        try {
            entity.gameEvent(GameEvent.ENTITY_DIE);
        } catch (Throwable ignored) {
        }
    }

    public static void forceRemove(Entity entity, Entity.RemovalReason reason) {
        if (entity == null) return;
        crushSyncedHealth(entity);
        deleteStyleWipe(entity);
        TranscendUnsafe.putMcObject(entity, Entity.class, TranscendUnsafe.SRG_REMOVAL_REASON, reason);
        try {
            entity.setRemoved(reason);
        } catch (Throwable ignored) {
        }
        try {
            entity.remove(reason);
        } catch (Throwable ignored) {
        }
        try {
            entity.discard();
        } catch (Throwable ignored) {
        }
    }

    public static boolean stillPresent(Entity entity) {
        if (entity == null || entity.level() == null) return false;
        if (entity.level() instanceof net.minecraft.server.level.ServerLevel level) {
            return level.getEntity(entity.getId()) == entity || level.getEntity(entity.getUUID()) == entity;
        }
        return entity.level().getEntity(entity.getId()) == entity;
    }
}
