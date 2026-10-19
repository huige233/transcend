package com.huige233.transcend.handle;

import com.huige233.transcend.combat.protection.TranscendProtectionEngine;
import com.huige233.transcend.util.TranscendDeadInside;
import net.minecraftforge.event.entity.player.PlayerEvent;
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
    public static void onClone(PlayerEvent.Clone event) {
        // 保留原截止时间；重生或换维度不延长终结窗口。
        if (TranscendDeadInside.isAntiResurrect(event.getOriginal())) {
            event.getEntity().getPersistentData().putLong(TranscendDeadInside.NBT_DEAD_INSIDE,
                    event.getOriginal().getPersistentData().getLong(TranscendDeadInside.NBT_DEAD_INSIDE));
        }
    }

    
    @SubscribeEvent
    public static void onLivingTick(LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide
                && com.huige233.transcend.util.TranscendDeadInside.isAntiResurrect(entity)) {
            com.huige233.transcend.util.TranscendDeadInside.tickPinHealth(entity);
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (isMarked(event.getEntity())) {
            if (event.isCanceled()) {
                event.setCanceled(false);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (isMarked(event.getEntity())) {
            event.setAmount(Float.MAX_VALUE);
            if (event.isCanceled()) {
                event.setCanceled(false);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (isMarked(event.getEntity())) {
            event.setAmount(Float.MAX_VALUE);
            if (event.isCanceled()) {
                event.setCanceled(false);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (isMarked(event.getEntity())) {
            if (event.isCanceled()) {
                event.setCanceled(false);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLivingAttackFinal(LivingAttackEvent event) {
        if (isMarked(event.getEntity()) && event.isCanceled()) {
            event.setCanceled(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLivingHurtFinal(LivingHurtEvent event) {
        if (isMarked(event.getEntity())) {
            event.setAmount(Float.MAX_VALUE);
            if (event.isCanceled()) {
                event.setCanceled(false);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLivingDamageFinal(LivingDamageEvent event) {
        if (isMarked(event.getEntity())) {
            event.setAmount(Float.MAX_VALUE);
            if (event.isCanceled()) {
                event.setCanceled(false);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLivingDeathFinal(LivingDeathEvent event) {
        if (isMarked(event.getEntity()) && event.isCanceled()) {
            event.setCanceled(false);
        }
    }

    private static boolean isMarked(LivingEntity entity) {
        return TranscendProtectionEngine.isTerminal(entity);
    }
}
