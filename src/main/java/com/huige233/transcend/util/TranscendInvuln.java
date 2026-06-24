package com.huige233.transcend.util;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;

/**
 * 通用无敌的轻量补强 —— 真正的不死靠 mixin(被保护期间 die/setHealth 空转、getHealth 在 HEAD 返回满血)。
 * 这里每 tick 只做三件事,都<b>绕过</b> setHealth(它已被 mixin 空转):
 *
 * <ul>
 *   <li>把底层同步血量直接写回满血 —— 让脱下装备的那一刻真实血量是满的(否则会被别家 mod 留在 0,一脱即死);
 *       同时下发给客户端的血量也是满血。</li>
 *   <li>把 omni 的 healthDelta 清 0(无害冗余,getHealth 在 HEAD 已短路,但保持数据干净)。</li>
 *   <li>清死亡进度字段。</li>
 * </ul>
 */
public final class TranscendInvuln {

    private static EntityDataAccessor<Float> omniDelta;
    private static boolean resolved;

    private TranscendInvuln() {
    }

    @SuppressWarnings("unchecked")
    private static void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            Class<?> ghm = Class.forName("flashfur.omnimobs.util.GetHealthModifyUtil");
            java.lang.reflect.Field f = ghm.getDeclaredField("DATA_MODIFY_GET_HEALTH_DELTA");
            f.setAccessible(true);
            omniDelta = (EntityDataAccessor<Float>) f.get(null);
        } catch (Throwable ignored) {
        }
    }

    /** 每 tick 调用;只有受保护时才补强(脱下装备即不再干预,血量恢复正常)。 */
    public static void apply(LivingEntity entity, boolean protectedNow) {
        if (entity == null || entity.level().isClientSide || !protectedNow) return;

        resolve();
        // 直接写 SynchedEntityData(不走被 mixin 空转的 setHealth):同步血量回满 + delta 归 0。
        try {
            EntityDataAccessor<Float> hp = TranscendUnsafe.dataHealthId();
            if (hp != null) {
                entity.getEntityData().set(hp, entity.getMaxHealth());
            }
            if (omniDelta != null) {
                entity.getEntityData().set(omniDelta, 0.0F);
            }
        } catch (Throwable ignored) {
        }

        TranscendUnsafe.putMcObject(entity, net.minecraft.world.entity.Entity.class,
                TranscendUnsafe.SRG_REMOVAL_REASON, null);
        entity.deathTime = 0;
        entity.hurtTime = 0;
    }
}
