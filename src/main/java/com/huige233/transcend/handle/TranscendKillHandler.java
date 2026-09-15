package com.huige233.transcend.handle;

import com.huige233.transcend.mixinitf.ITranscendMarked;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 持续压制终结目标的复活，并在战斗事件首尾强化标记目标的致命伤害与死亡结算。 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)

public class TranscendKillHandler {

    
    @SubscribeEvent
    public static void onLivingTick(LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide
                && com.huige233.transcend.util.TranscendDeadInside.isAntiResurrect(entity)) {
            com.huige233.transcend.util.TranscendDeadInside.tickPinHealth(entity);
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (isMarked(event.getEntity())) {
            if (event.isCanceled()) {
                event.setCanceled(false);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (isMarked(event.getEntity())) {
            event.setAmount(Float.MAX_VALUE);
            if (event.isCanceled()) {
                event.setCanceled(false);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (isMarked(event.getEntity())) {
            event.setAmount(Float.MAX_VALUE);
            if (event.isCanceled()) {
                event.setCanceled(false);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (isMarked(event.getEntity())) {
            if (event.isCanceled()) {
                event.setCanceled(false);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingAttackFinal(LivingAttackEvent event) {
        if (isMarked(event.getEntity()) && event.isCanceled()) {
            event.setCanceled(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurtFinal(LivingHurtEvent event) {
        if (isMarked(event.getEntity())) {
            event.setAmount(Float.MAX_VALUE);
            if (event.isCanceled()) {
                event.setCanceled(false);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamageFinal(LivingDamageEvent event) {
        if (isMarked(event.getEntity())) {
            event.setAmount(Float.MAX_VALUE);
            if (event.isCanceled()) {
                event.setCanceled(false);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeathFinal(LivingDeathEvent event) {
        if (isMarked(event.getEntity()) && event.isCanceled()) {
            event.setCanceled(false);
        }
    }

    private static boolean isMarked(LivingEntity entity) {
        return entity instanceof ITranscendMarked marked && marked.transcend$isMarked();
    }
}
