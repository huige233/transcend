package com.huige233.transcend.util;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

   
                                                              
                              
                                        
                                                        
                                                                    
                                        
                                
   
/** 执行带防复活时限的生命清零与强制死亡，并以粒子效果和世界清理结束目标实体。 */
public final class TranscendDeadInside {

    
    public static final String NBT_DEAD_INSIDE = "transcend_dead_inside";

    private TranscendDeadInside() {
    }

    
    public static boolean isAntiResurrect(LivingEntity entity) {
        long until = entity.getPersistentData().getLong(NBT_DEAD_INSIDE);
        if (until == 1L || until > System.currentTimeMillis()) return true;
        if (until != 0L) entity.getPersistentData().remove(NBT_DEAD_INSIDE);
        return false;
    }

       
                            
      
                           
                                              
       
    public static void apply(LivingEntity target, @Nullable Player attacker) {
        if (target == null || target.level().isClientSide) return;
        if (!(target.level() instanceof ServerLevel level)) return;

        
        antiResurrect(target, 60_000L);

        DamageSource killSource = attacker != null
                ? level.damageSources().playerAttack(attacker)
                : level.damageSources().genericKill();

        if (attacker != null) {
            target.invulnerableTime = 0;
            target.hurt(killSource, 0.0F);
            target.setLastHurtByPlayer(attacker);
        }
        target.invulnerableTime = 0;

        if (target instanceof EnderDragon dragon) {
            dragon.kill();
        } else {
            setHealthZero(target);
            try {
                target.die(killSource);
            } catch (Throwable ignored) {
            }
        }
        despawn(level, target);
    }

    
    public static void tickPinHealth(LivingEntity entity) {
        if (!isAntiResurrect(entity)) return;
        entity.setHealth(0.0F);
        if (entity.isAlive()) {
            try {
                entity.die(entity.level().damageSources().genericKill());
            } catch (Throwable ignored) {
            }
        }
    }

    
    public static void antiResurrect(LivingEntity entity, long durationMs) {
        long until = durationMs <= 0 ? 1L : System.currentTimeMillis() + durationMs;
        entity.getPersistentData().putLong(NBT_DEAD_INSIDE, until);
    }

    
    private static void setHealthZero(LivingEntity entity) {
        try {
            var dataAccessor = TranscendUnsafe.dataHealthId();
            if (dataAccessor != null) {
                entity.getEntityData().set(dataAccessor, 0.0F);
            }
        } catch (Throwable ignored) {
        }
        try {
            entity.setHealth(0.0F);
        } catch (Throwable ignored) {
        }
    }

    
    private static void despawn(ServerLevel level, LivingEntity entity) {
        level.sendParticles(ParticleTypes.ASH,
                entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                50, entity.getBbWidth(), entity.getBbHeight() / 2, entity.getBbWidth(), 0.02);
        level.sendParticles(ParticleTypes.WHITE_ASH,
                entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                50, entity.getBbWidth(), entity.getBbHeight() / 2, entity.getBbWidth(), 0.02);

        TranscendEntityPurge.purgeFromLevel(entity, true);
        hardRemove(entity);
    }

    
    private static void hardRemove(Entity entity) {
        try {
            entity.discard();
        } catch (Throwable ignored) {
        }
        if (!entity.isRemoved()) {
            TranscendUnsafeKill.forceRemove(entity, Entity.RemovalReason.KILLED);
        }
    }
}
