package com.huige233.transcend.effect;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.init.ModEffects;
import com.huige233.transcend.util.EntityCompatUtil;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Transcend.MODID)
public class EffectEventHandler {
    private static final String SOUL_SHOCK_PROC_TAG = "transcend_soul_shock_proc";

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance antiHeal = entity.getEffect(ModEffects.ANTI_HEAL.get());
        if (antiHeal != null) {
            float mult = AntiHealEffect.getHealMultiplier(antiHeal.getAmplifier());
            if (mult <= 0) {
                event.setCanceled(true);
            } else {
                event.setAmount(event.getAmount() * mult);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance annihilation = entity.getEffect(ModEffects.ANNIHILATION.get());
        if (annihilation != null) {
            float mult = AnnihilationEffect.getDamageMultiplier(annihilation.getAmplifier());
            event.setAmount(event.getAmount() * mult);
        }

        MobEffectInstance soulShock = entity.getEffect(ModEffects.SOUL_SHOCK.get());
        if (soulShock != null
                && event.getAmount() > 0
                && !entity.getPersistentData().getBoolean(SOUL_SHOCK_PROC_TAG)
                && !EntityCompatUtil.isProtectedPlayer(entity)) {
            int amp = soulShock.getAmplifier();
            float chance = SoulShockEffect.getTriggerChance(amp);
            if (entity.getRandom().nextFloat() < chance) {
                float bonusVoidDamage = SoulShockEffect.getVoidBonusDamage(amp);
                entity.getPersistentData().putBoolean(SOUL_SHOCK_PROC_TAG, true);
                try {
                    entity.hurt(entity.damageSources().wither(), bonusVoidDamage);
                } finally {
                    entity.getPersistentData().putBoolean(SOUL_SHOCK_PROC_TAG, false);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEnderPearl(EntityTeleportEvent.EnderPearl event) {
        if (event.getEntity() instanceof Player p && (p.isCreative() || p.isSpectator())) return;
        if (event.getEntity() instanceof LivingEntity le && le.getPersistentData().getBoolean("transcend_tp_lock")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onChorusFruit(EntityTeleportEvent.ChorusFruit event) {
        if (event.getEntity() instanceof Player p && (p.isCreative() || p.isSpectator())) return;
        if (event.getEntity().getPersistentData().getBoolean("transcend_tp_lock")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEnderEntity(EntityTeleportEvent.EnderEntity event) {
        if (event.getEntity().getPersistentData().getBoolean("transcend_tp_lock")) {
            event.setCanceled(true);
        }
    }
}
