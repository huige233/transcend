package com.huige233.transcend.agent;

import com.huige233.transcend.util.TranscendGuard;
import net.minecraft.world.entity.LivingEntity;

/**
 * 被 {@link TranscendOmniPatch} 注入到 {@code LivingEntity.getHealth/isAlive/isDeadOrDying} 的<b>返回值</b>上的钩子。
 *
 * <p>因为本注入是<b>最后</b>注册到 omni 那个 Instrumentation 上的(在 omni、梦幻终焉之后),所以这三个方法的
 * 返回值<b>最终</b>都经过这里 —— 受保护玩家一律读成「满血 / 活着 / 没死」,谁在里层改都被这里盖回去。</p>
 */
public final class TranscendAgentHook {

    private TranscendAgentHook() {
    }

    public static float health(float original, LivingEntity entity) {
        try {
            if (TranscendGuard.isProtected(entity)) {
                return Math.max(1.0F, entity.getMaxHealth());
            }
        } catch (Throwable ignored) {
        }
        return original;
    }

    public static boolean alive(boolean original, LivingEntity entity) {
        try {
            if (TranscendGuard.isProtected(entity)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return original;
    }

    public static boolean deadOrDying(boolean original, LivingEntity entity) {
        try {
            if (TranscendGuard.isProtected(entity)) {
                return false;
            }
        } catch (Throwable ignored) {
        }
        return original;
    }
}
