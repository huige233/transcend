package com.huige233.transcend.handle;

import com.huige233.transcend.util.OmniCompatUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class OmniSwordGuardHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!OmniCompatUtil.playerHoldingTranscendSword(player)) return;
        if (!OmniCompatUtil.isMetapotentLikeDamage(event.getSource())) return;
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!OmniCompatUtil.playerHoldingTranscendSword(player)) return;
        if (!OmniCompatUtil.isMetapotentLikeDamage(event.getSource())) return;
        event.setCanceled(true);
        event.setAmount(0.0F);
        player.hurtTime = 0;
        player.deathTime = 0;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!OmniCompatUtil.playerHoldingTranscendSword(player)) return;
        if (!OmniCompatUtil.isMetapotentLikeDamage(event.getSource())) return;
        event.setCanceled(true);
        event.setAmount(0.0F);
        player.hurtTime = 0;
        player.deathTime = 0;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!OmniCompatUtil.playerHoldingTranscendSword(player)) return;
        if (!OmniCompatUtil.isMetapotentLikeDamage(event.getSource())) return;
        event.setCanceled(true);
        player.setHealth(Math.max(1.0F, player.getMaxHealth()));
        player.hurtTime = 0;
        player.deathTime = 0;
    }
}
