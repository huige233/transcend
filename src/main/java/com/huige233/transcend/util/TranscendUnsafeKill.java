package com.huige233.transcend.util;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.gameevent.GameEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public final class TranscendUnsafeKill {

    private TranscendUnsafeKill() {
    }

    public static void catchSetTrueHealth(LivingEntity living, float value) {
        if (living == null) return;

        EntityDataAccessor<Float> hp = TranscendUnsafe.dataHealthId();
        if (hp != null) {
            try {
                living.getEntityData().set(hp, value);
            } catch (Throwable ignored) {
            }
        }

        crushAllFloatHealthData(living, value);

        try {
            living.setHealth(value);
        } catch (Throwable ignored) {
        }
    }

    public static void crushSyncedHealth(Entity entity) {
        crushSyncedHealth(entity, Float.NEGATIVE_INFINITY);
    }

    public static void crushSyncedHealth(Entity entity, float value) {
        if (entity == null) return;

        if (entity instanceof LivingEntity living) {
            catchSetTrueHealth(living, value);
        } else {
            EntityDataAccessor<Float> hp = TranscendUnsafe.dataHealthId();
            if (hp != null) {
                try {
                    entity.getEntityData().set(hp, value);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void crushAllFloatHealthData(LivingEntity living, float value) {
        try {
            SynchedEntityData data = living.getEntityData();
            float reported;
            try {
                reported = living.getHealth();
            } catch (Throwable t) {
                reported = Float.NaN;
            }

            Object itemsById = TranscendUnsafe.getObject(data, "itemsById");
            if (itemsById == null) {
                Field f = TranscendUnsafe.mcField(SynchedEntityData.class, "f_135348_");
                if (f != null && TranscendUnsafe.UNSAFE != null) {
                    itemsById = TranscendUnsafe.UNSAFE.getObject(data, TranscendUnsafe.UNSAFE.objectFieldOffset(f));
                }
            }
            if (!(itemsById instanceof Map<?, ?> map)) return;

            EntityDataAccessor<Float> primary = TranscendUnsafe.dataHealthId();

            for (Object itemObj : map.values()) {
                if (itemObj == null) continue;
                try {
                    Method getValue = itemObj.getClass().getMethod("getValue");
                    Object cur = getValue.invoke(itemObj);
                    Method getAccessor = itemObj.getClass().getMethod("getAccessor");
                    Object accessor = getAccessor.invoke(itemObj);
                    if (!(accessor instanceof EntityDataAccessor<?> eda)) continue;

                    if (primary != null && eda.getId() == primary.getId()) {
                        if (cur instanceof Float) {
                            data.set((EntityDataAccessor) eda, value);
                        }
                        continue;
                    }

                    if (cur instanceof Float fCur) {
                        if (isHealthMirror(fCur, reported, living)) {
                            data.set((EntityDataAccessor) eda, value);
                        }
                    } else if (cur instanceof Double dCur) {
                        if (isHealthMirror(dCur.floatValue(), reported, living)) {
                            data.set((EntityDataAccessor) eda, (double) value);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean isHealthMirror(float cur, float reported, LivingEntity living) {
        if (Float.isNaN(cur)) return false;
        if (!Float.isNaN(reported) && Math.abs(cur - reported) < 1.0E-3F) return true;
        try {
            float max = living.getMaxHealth();
            if (Math.abs(cur - max) < 1.0E-3F && max > 0.0F) return true;
        } catch (Throwable ignored) {
        }
        return false;
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
        if (entity == null) return false;
        if (entity.isRemoved()) return false;
        if (!entity.isAlive()) return false;
        if (entity.level() == null) return false;
        return entity.level().getEntity(entity.getId()) == entity;
    }

    public static boolean neutralizeStaticRespawnBoss(Entity target) {
        if (target == null) return false;
        try {
            for (Class<?> c = target.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                    try {
                        f.setAccessible(true);
                        Object v = f.get(target);
                        if (v instanceof ServerBossEvent be) {
                            be.setVisible(false);
                            be.removeAllPlayers();
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
