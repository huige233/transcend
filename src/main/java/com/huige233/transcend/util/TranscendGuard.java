package com.huige233.transcend.util;

import com.huige233.transcend.items.TranscendShield;
import com.huige233.transcend.items.tools.TranscendSword;
import com.huige233.transcend.mixinitf.ITranscendMarked;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 编辑权限守卫：主手/副手/背包持有 transcend 剑、或已装备全套超越甲、或持超越盾者为受保护对象。 */
public final class TranscendGuard {

    private TranscendGuard() {
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
        if (entity == null || entity.level().isClientSide) return false;
        if (!(entity instanceof Player player)) return false;

        if (player.getInventory() == null) return false;
        if (entity instanceof ITranscendMarked marked && marked.transcend$isMarked()) return false;

        return ArmorUtils.fullEquipped(player)
                || TranscendShield.hasTranscendShield(player)
                || hasTranscendSword(player);
    }

    public static boolean blocksPierceSetHealth(LivingEntity entity, float health) {
        if (!isProtected(entity)) return false;
        return Float.isNaN(health) || health < entity.getHealth();
    }

    /** 每 tick 强制回复阶段（满血 / 清负面 / 清火 / 清减益/吸收 2000）。 */
    public static void enforce(Player player) {
        if (player == null || player.level().isClientSide) return;
        float max = player.getMaxHealth();
        if (!Float.isFinite(max) || max <= 0.0F) max = 1.0F;
        if (player.getHealth() < max) {
            player.setHealth(max);
        }
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
