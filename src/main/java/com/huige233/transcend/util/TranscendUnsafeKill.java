package com.huige233.transcend.util;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 通用 Unsafe 击杀(进攻)—— 直接对原版 {@code Entity/LivingEntity} 的字节码字段下手,
 * 不再为每个 mod 写一个反射兼容类。被 coremod 包装的方法、被门控覆写的 mutator 一律绕过。
 *
 * <p>仅保留一处<b>不可通用</b>的残留:某些「不死 boss」的真身存在自己的<b>静态列表</b>里每 tick 重生傀儡
 * (如 omni 的 Metapotent Flashfur),通用的「移除实体」拦不住重生 —— 只能按类名直接把那个静态 ArrayList
 * 用 Unsafe 写空。它内联在这里,不再单独成类。</p>
 */
public final class TranscendUnsafeKill {

    // 唯一不可通用的残留:真身在静态列表里每 tick 重生傀儡的 boss,需要直接清空那个列表。
    private static final String META_PROXY_CLASS =
            "flashfur.omnimobs.entities.metapotent_flashfur.MetapotentFlashfurEntity";
    private static final String META_LEVEL_CLASS =
            "flashfur.omnimobs.entities.metapotent_flashfur.MetapotentFlashfurLevel";

    // omni 自家实体只能用它自己的 EntityUtil 杀:多个血量字段 + delta + actuallyAlive 撒谎,通用「写同步血 0」杀不掉。
    private static final String OMNI_PREFIX = "flashfur.omnimobs.";
    private static final String OMNI_ENTITY_UTIL = "flashfur.omnimobs.util.EntityUtil";

    private TranscendUnsafeKill() {
    }

    public static boolean isOmni(Entity entity) {
        return entity != null && entity.getClass().getName().startsWith(OMNI_PREFIX);
    }

    /** 把同步血量直接写成 0;omni 实体再调它自家 EntityUtil 把所有血量字段/同步数据/setHealth 方法全压成 0。 */
    public static void crushSyncedHealth(Entity entity) {
        EntityDataAccessor<Float> hp = TranscendUnsafe.dataHealthId();
        if (hp != null) {
            try {
                entity.getEntityData().set(hp, 0.0F);
            } catch (Throwable ignored) {
            }
        }
        if (entity instanceof LivingEntity && isOmni(entity)) {
            omniInvoke("setAllHealthFields", entity);
            omniInvoke("setAllSyncedHealthData", entity);
            omniInvoke("runSetHealthMethods", entity);
        }
    }

    /** 通用强制移除:omni 实体优先走它自家 forceRemove(直接从 level 里剔除),再写 removalReason + 标准 remove。 */
    public static void forceRemove(Entity entity, Entity.RemovalReason reason) {
        if (entity == null) return;
        crushSyncedHealth(entity);
        omniForceRemove(entity, reason);
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

    /** 通用判活:omni 实体的 isAlive/getHealth 会撒谎,以它自己的 actuallyAlive 为准。 */
    public static boolean stillPresent(Entity entity) {
        if (entity == null) return false;
        if (omniActuallyAlive(entity)) return true;
        if (entity.isRemoved()) return false;
        if (!entity.isAlive()) return false;
        if (entity.level() == null) return false;
        return entity.level().getEntity(entity.getId()) == entity;
    }

    private static void omniInvoke(String method, Entity entity) {
        try {
            Class<?> u = Class.forName(OMNI_ENTITY_UTIL);
            u.getMethod(method, LivingEntity.class, float.class).invoke(null, entity, 0.0F);
        } catch (Throwable ignored) {
        }
    }

    private static void omniForceRemove(Entity entity, Entity.RemovalReason reason) {
        if (!isOmni(entity)) return;
        try {
            Class<?> u = Class.forName(OMNI_ENTITY_UTIL);
            u.getMethod("forceRemove", Entity.class, Entity.RemovalReason.class).invoke(null, entity, reason);
        } catch (Throwable ignored) {
        }
    }

    private static boolean omniActuallyAlive(Entity entity) {
        if (!(entity instanceof LivingEntity) || !isOmni(entity)) return false;
        try {
            Class<?> u = Class.forName(OMNI_ENTITY_UTIL);
            Object r = u.getMethod("actuallyAlive", LivingEntity.class).invoke(null, entity);
            return r instanceof Boolean b && b;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * 唯一的 mod 专属残留(内联,无专类):真身在静态列表里每 tick 重生傀儡的 boss。
     * 用 Unsafe 把那个静态 ArrayList 写空 → 真身不再被 tick → 不再重生傀儡。返回 true 表示已处理。
     */
    public static boolean neutralizeStaticRespawnBoss(Entity target) {
        if (target == null || !META_PROXY_CLASS.equals(target.getClass().getName())) {
            return false;
        }
        try {
            Object boss = TranscendUnsafe.getObject(target, "metapotentFlashfur");
            if (boss != null) {
                Object bossEvent = TranscendUnsafe.getObject(boss, "bossEvent");
                if (bossEvent instanceof ServerBossEvent be) {
                    be.setVisible(false);
                    be.removeAllPlayers();
                }
                TranscendUnsafe.putBoolean(boss, "removed", true);
            }
            Class<?> levelClass = Class.forName(META_LEVEL_CLASS);
            Object list = TranscendUnsafe.getStatic(levelClass, "metapotentFlashfurList");
            TranscendUnsafe.emptyArrayList(list);
            try {
                levelClass.getMethod("updateEventBus").invoke(null);
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
        forceRemove(target, Entity.RemovalReason.KILLED);
        return true;
    }
}
