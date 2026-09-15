package com.huige233.transcend.util;

import com.huige233.transcend.combat.protection.TranscendProtectionEngine;
import com.huige233.transcend.combat.protection.TranscendHealthDataGuard;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

   
                                                 
                                    
   
/** 根据统一保护判定拦截伤害、修复死亡状态，并管理复活后的临时无敌计时。 */
@Mod.EventBusSubscriber(modid = "transcend", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TranscendDefense {

    private TranscendDefense() {
    }

    public static final String NBT_INVULNERABLE_TIME = "transcend_invulnerable_time";
    public static final int INVULNERABLE_TICKS = 300;

    
    public static void tickInvulnerable(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide) return;
        if (!entity.getPersistentData().contains(NBT_INVULNERABLE_TIME)) return;
        int t = entity.getPersistentData().getInt(NBT_INVULNERABLE_TIME) - 1;
        if (t <= 0) {
            entity.getPersistentData().remove(NBT_INVULNERABLE_TIME);
        } else {
            entity.getPersistentData().putInt(NBT_INVULNERABLE_TIME, t);
        }
    }

    
    public static boolean hasReviveInvulnerability(LivingEntity entity) {
        return entity != null && !entity.level().isClientSide
                && entity.getPersistentData().getInt(NBT_INVULNERABLE_TIME) > 0;
    }
    
    public static void grantReviveInvulnerable(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide) return;
        entity.getPersistentData().putInt(NBT_INVULNERABLE_TIME, INVULNERABLE_TICKS);
    }

    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || entity.level().isClientSide) return;
        if (!TranscendProtectionEngine.blocksDamage(entity)) return;
        
        if (entity.deathTime > 0 || entity.getHealth() <= 0.0F) {
            entity.setHealth(entity.getMaxHealth());
            entity.deathTime = 0;
            entity.hurtTime = 0;
        }
        event.setCanceled(true);
    }

    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || entity.level().isClientSide) return;
        if (!TranscendProtectionEngine.blocksDamage(entity)) return;
        event.setAmount(0);
    }

       
                                                      
                                  
       
    @SubscribeEvent
    public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || entity.level().isClientSide) return;
        if (!TranscendProtectionEngine.blocksDamage(entity)) return;
        if (entity.deathTime > 0 || !entity.isAlive() || entity.getHealth() <= 0.0F) {
            entity.setHealth(Math.max(1.0F, entity.getMaxHealth()));
            entity.deathTime = 0;
            entity.hurtTime = 0;
        }
    }
}
