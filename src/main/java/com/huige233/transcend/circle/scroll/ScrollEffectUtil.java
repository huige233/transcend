package com.huige233.transcend.circle.scroll;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.AABB;

/**
 * 咒卷效果共用工具方法。
 */
final class ScrollEffectUtil {

    private ScrollEffectUtil() {}

    /** 判定是否 Boss（不可随意秒杀的对象） */
    static boolean isBoss(Entity entity) {
        return entity instanceof EnderDragon
                || entity instanceof WitherBoss
                || entity instanceof Warden
                || entity.getType() == EntityType.ENDER_DRAGON
                || entity.getType() == EntityType.WITHER
                || entity.getType() == EntityType.WARDEN;
    }

    /** 判定是否敌对生物（怪物类别） */
    static boolean isHostile(LivingEntity entity) {
        return entity.getType().getCategory() == MobCategory.MONSTER;
    }

    /** 判定是否亡灵 */
    static boolean isUndead(LivingEntity entity) {
        return entity.getMobType() == MobType.UNDEAD;
    }

    /** 构造以 pos 为中心、给定半径的轴向包围盒 */
    static AABB radiusBox(BlockPos pos, double radius) {
        return new AABB(
                pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
                pos.getX() + radius + 1, pos.getY() + radius + 1, pos.getZ() + radius + 1
        );
    }
}
