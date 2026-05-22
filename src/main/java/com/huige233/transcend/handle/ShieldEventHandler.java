package com.huige233.transcend.handle;

import com.huige233.transcend.items.TranscendShield;
import com.huige233.transcend.mixinitf.ITranscendMarked;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ShieldEventHandler {

    @SubscribeEvent
    public static void onPlayerAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!isUsingShield(player)) return;

        Entity target = event.getTarget();
        if (target instanceof LivingEntity living) {
            TranscendShield.applyTimeStop(living);
            TranscendShield.reflectDamage(living, player);
        }
    }

    private static boolean isUsingShield(Player player) {
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof TranscendShield) return true;
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onShieldDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player instanceof ITranscendMarked m && m.transcend$isMarked()) return;
            if (TranscendShield.hasTranscendShield(player)) {
                event.setCanceled(true);
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onShieldAttacked(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player instanceof ITranscendMarked m && m.transcend$isMarked()) return;
            if (TranscendShield.hasTranscendShield(player)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onShieldHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player instanceof ITranscendMarked m && m.transcend$isMarked()) return;
            if (TranscendShield.hasTranscendShield(player)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onShieldDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player instanceof ITranscendMarked m && m.transcend$isMarked()) return;
            if (TranscendShield.hasTranscendShield(player)) {
                event.setAmount(0.0f);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onShieldKnockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (TranscendShield.hasTranscendShield(player)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPotionApplied(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (TranscendShield.hasTranscendShield(player)) {
                MobEffectInstance effect = event.getEffectInstance();
                if (effect != null && !effect.getEffect().isBeneficial()) {
                    event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onTimestopTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        int remaining = entity.getPersistentData().getInt("transcend_timestop");
        if (remaining > 0) {
            entity.setDeltaMovement(0, 0, 0);
            entity.hurtMarked = true;
            entity.setNoGravity(true);
            entity.getPersistentData().putInt("transcend_timestop", remaining - 1);
        } else if (remaining == 0 && entity.getPersistentData().contains("transcend_timestop")) {
            entity.setNoGravity(false);
            entity.getPersistentData().remove("transcend_timestop");
        }

        // === Armor Break 持续时间管理 ===
        int armorBreakRemaining = entity.getPersistentData().getInt("transcend_armor_break");
        if (armorBreakRemaining > 0) {
            entity.getPersistentData().putInt("transcend_armor_break", armorBreakRemaining - 1);
        } else if (armorBreakRemaining == 0 && entity.getPersistentData().contains("transcend_armor_break")) {
            // 过期：移除护甲减少 modifier
            AttributeInstance armorAttr = entity.getAttribute(Attributes.ARMOR);
            if (armorAttr != null) {
                armorAttr.removeModifier(java.util.UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901"));
            }
            entity.getPersistentData().remove("transcend_armor_break");
        }
    }
}
