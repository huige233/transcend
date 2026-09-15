package com.huige233.transcend.combat.protection;
import com.huige233.transcend.util.TranscendUnsafe;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
/** 校验生命值数值并将服务端实体的同步生命数据修复为有效最大生命值。 */
public final class TranscendHealthDataGuard {
 private TranscendHealthDataGuard() {}
 public static boolean valid(float v){return Float.isFinite(v)&&v>0.0F;}
 public static float safeMax(LivingEntity e){float m=e.getMaxHealth();return Float.isFinite(m)&&m>0?m:1.0F;}
 public static float safeValue(LivingEntity e,float value){return valid(value)&&value<=safeMax(e)?value:safeMax(e);}
 public static void repair(LivingEntity e){if(e==null||e.level().isClientSide)return; EntityDataAccessor<Float> id=TranscendUnsafe.dataHealthId(); if(id!=null)e.getEntityData().set(id,safeMax(e));}
}
