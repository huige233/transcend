package com.mega.uom.util.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class PlayerInvulnerableEntityData {
    public static String FE_INVULNERABLE = "feInvulnerable";
    public static EntityDataAccessor<Boolean> FE_INVULNERABLE_DATA;

    public static boolean isInvul(LivingEntity player) {
        if (!(player instanceof Player)) return false;
        return player.getEntityData().get(FE_INVULNERABLE_DATA);
    }

    public static void setInvul(Player player, boolean value) {
        if (!value && isInvul(player)) {
            EntityASMUtil.setHealthDelta(player, 0.0F);
            EntityActuallyHurt.catchSetTrueHealth(player, player.getMaxHealth());
        }
        player.getEntityData().set(FE_INVULNERABLE_DATA, value);
    }
}
