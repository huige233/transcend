package com.mega.uom.util.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

public class EntityDataInjector {
    public static EntityDataAccessor<Float> MEB_DAMAGE;
    public static EntityDataAccessor<Integer> MEB_LEVEL;
    public static EntityDataAccessor<Integer> MEB_TIME;
    public static EntityDataAccessor<Optional<UUID>> MEB_SOURCE;
    public static EntityDataAccessor<Integer> FE_ARMORED_PLAYER_EVASION_TIME;
    public static String MEB_DAMAGE_NAME = "multipleEldritchBlastDamage";
    public static String MEB_LEVEL_NAME = "multipleEldritchBlastLevel";
    public static String MEB_TIME_NAME = "multipleEldritchBlastTime";
    public static String MEB_SOURCE_NAME = "multipleEldritchBlastSourceID";
    public static String FEAP_EVASION_TIME = "feArmoredPlayerEvasionTime";
    public static String ALLOW_HURT_METHOD = "feAllowHurtMethod";

    public static void setMebDamage(LivingEntity living, float damage) {
        living.getEntityData().set(MEB_DAMAGE, damage);
    }

    public static void setMebTime(LivingEntity living, int time) {
        living.getEntityData().set(MEB_TIME, time);
    }

    public static void setMebLevel(LivingEntity living, int level) {
        living.getEntityData().set(MEB_LEVEL, level);
    }

    public static void setMebSource(LivingEntity living, Optional<UUID> optionalUUID) {
        living.getEntityData().set(MEB_SOURCE, optionalUUID);
    }

    public static void setEvasionTime(LivingEntity player, int time) {
        player.getEntityData().set(FE_ARMORED_PLAYER_EVASION_TIME, time);
    }

    public static int getMebTime(LivingEntity living) {
        return living.getEntityData().get(MEB_TIME);
    }

    public static float getMebDamage(LivingEntity living) {
        return living.getEntityData().get(MEB_DAMAGE);
    }

    public static int getMebLevel(LivingEntity living) {
        return living.getEntityData().get(MEB_LEVEL);
    }

    public static Optional<UUID> getMebSource(LivingEntity living) {
        return living.getEntityData().get(MEB_SOURCE);
    }

    public static int getEvasionTime(LivingEntity living) {
        return living.getEntityData().get(FE_ARMORED_PLAYER_EVASION_TIME);
    }

    public static void setAllowHurtMethod(LivingEntity entity, boolean isTrue) {
        entity.getPersistentData().putBoolean(ALLOW_HURT_METHOD, isTrue);
    }

    public static boolean getAllowHurtMethod(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(ALLOW_HURT_METHOD);
    }
}
