package com.huige233.transcend.util;

import com.huige233.transcend.items.TranscendShield;
import com.huige233.transcend.items.tools.TranscendSword;
import com.huige233.transcend.combat.protection.TranscendProtectionEngine;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;


/** 依据护甲、剑盾和终结标记判定玩家保护资格，并提供生命、饥饿及异常状态恢复操作。 */
public final class TranscendGuard {

    private TranscendGuard() {
    }

    public static boolean hasShieldOrSword(Player player) {
        return TranscendShield.hasTranscendShield(player) || hasTranscendSword(player);
    }

    private static boolean hasTranscendSword(Player player) {
        if (player.getMainHandItem().getItem() instanceof TranscendSword) return true;
        if (player.getOffhandItem().getItem() instanceof TranscendSword) return true;
        for (ItemStack s : player.getInventory().items) {
            if (s.getItem() instanceof TranscendSword) return true;
        }
        return false;
    }

    public static boolean isProtected(LivingEntity entity) {
        return TranscendProtectionEngine.isProtected(entity);
    }

    
    public static void enforce(Player player) {
        if (!TranscendProtectionEngine.isProtected(player)) return;
        TranscendProtectionEngine.repair(player);
        try {
            player.removeAllEffects();
        } catch (Throwable ignored) {
        }
        if (player.getAbsorptionAmount() < 2000.0F) {
            player.setAbsorptionAmount(2000.0F);
        }
        player.setAirSupply(300);
        player.setRemainingFireTicks(0);
        player.fallDistance = 0.0F;
        if (player.hurtTime > 0) player.hurtTime = 0;
        if (player.deathTime > 0) player.deathTime = 0;
        player.invulnerableTime = 0;
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20);
    }

    public static void annihilateTarget(net.minecraft.world.entity.Entity target, Player attacker) {
        if (target == null || target.level().isClientSide) return;
        if (target instanceof LivingEntity living
                && living == attacker) return;
        SwordUtil.annihilate(target, attacker);
    }
}
