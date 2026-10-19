package com.huige233.transcend.util;

import com.huige233.transcend.combat.attack.HealthAccess;
import com.huige233.transcend.combat.protection.TranscendHealthDataGuard;
import com.huige233.transcend.combat.protection.TranscendProtectionEngine;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 统一处理神器免伤、免死、抗击退和拒绝锁敌，并维护复活后的临时保护。 */
@Mod.EventBusSubscriber(modid = "transcend", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TranscendDefense {
    public static final String NBT_INVULNERABLE_TIME = "transcend_invulnerable_time";
    public static final int INVULNERABLE_TICKS = 300;

    private TranscendDefense() {}

    public static void tickInvulnerable(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide) return;
        int ticks = entity.getPersistentData().getInt(NBT_INVULNERABLE_TIME);
        if (ticks <= 1) entity.getPersistentData().remove(NBT_INVULNERABLE_TIME);
        else entity.getPersistentData().putInt(NBT_INVULNERABLE_TIME, ticks - 1);
    }

    public static boolean hasReviveInvulnerability(LivingEntity entity) {
        return TranscendProtectionEngine.state(entity).temporaryProtection();
    }

    public static void grantReviveInvulnerable(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide) return;
        entity.getPersistentData().putInt(NBT_INVULNERABLE_TIME, INVULNERABLE_TICKS);
    }

    private static void repairEventTarget(LivingEntity entity) {
        if (TranscendProtectionEngine.isProtected(entity)) {
            TranscendProtectionEngine.repair(entity);
        } else {
            HealthAccess.write(entity, TranscendHealthDataGuard.safeMax(entity));
            entity.deathTime = 0;
            entity.hurtTime = 0;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onAttack(LivingAttackEvent event) {
        if (!TranscendProtectionEngine.blocksEventDamage(event.getEntity())) return;
        repairEventTarget(event.getEntity());
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onHurt(LivingHurtEvent event) {
        if (!TranscendProtectionEngine.blocksEventDamage(event.getEntity())) return;
        repairEventTarget(event.getEntity());
        event.setAmount(0);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onDamage(LivingDamageEvent event) {
        if (!TranscendProtectionEngine.blocksEventDamage(event.getEntity())) return;
        repairEventTarget(event.getEntity());
        event.setAmount(0);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onDeath(LivingDeathEvent event) {
        if (!TranscendProtectionEngine.blocksEventDamage(event.getEntity())) return;
        repairEventTarget(event.getEntity());
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onKnockback(LivingKnockBackEvent event) {
        if (TranscendProtectionEngine.blocksEventDamage(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (TranscendProtectionEngine.blocksEventDamage(event.getNewTarget())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onHeal(LivingHealEvent event) {
        if (TranscendProtectionEngine.blocksHealing(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        TranscendProtectionEngine.repair(event.getEntity());
        // 玩家窗口在 TranscendUtil 的 END 阶段递减，其余生物在此递减。
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player)) {
            tickInvulnerable(event.getEntity());
        }
    }
}
