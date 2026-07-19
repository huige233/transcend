package com.huige233.transcend.util;

import com.huige233.transcend.items.TranscendShield;
import com.huige233.transcend.items.tools.TranscendSword;
import com.huige233.transcend.mixinitf.ITranscendMarked;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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
}
