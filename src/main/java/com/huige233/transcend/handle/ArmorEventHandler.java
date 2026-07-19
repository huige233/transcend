package com.huige233.transcend.handle;

import com.huige233.transcend.mixinitf.ITranscendMarked;
import com.huige233.transcend.util.ArmorUtils;
import com.huige233.transcend.util.SwordUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ArmorEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onArmorDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player instanceof ITranscendMarked m && m.transcend$isMarked()) return;
            if (ArmorUtils.fullEquipped(player)) {
                event.setCanceled(true);
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onArmorHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player instanceof ITranscendMarked m && m.transcend$isMarked()) return;
            if (ArmorUtils.fullEquipped(player)) {
                event.setCanceled(true);
                Entity source = event.getSource().getEntity();
                if (source instanceof LivingEntity living && !(source instanceof Player)) {
                    SwordUtil.annihilate(living, player);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onArmorAttacked(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player instanceof ITranscendMarked m && m.transcend$isMarked()) return;
            if (ArmorUtils.fullEquipped(player)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onArmorDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player instanceof ITranscendMarked m && m.transcend$isMarked()) return;
            if (ArmorUtils.fullEquipped(player)) {
                event.setAmount(0.0f);
                player.hurtTime = 0;
                player.deathTime = 0;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onArmorKnockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (ArmorUtils.fullEquipped(player)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onArmorTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof ServerPlayer player) {
            if (ArmorUtils.fullEquipped(player)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (ArmorUtils.fullEquipped(event.getOriginal())) {
            event.getEntity().setHealth(event.getEntity().getMaxHealth());
        }
    }
}
