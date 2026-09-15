package com.mega.uom.event.eventhandler.common;

import com.mega.uom.common.capability.ModCapabilities;
import com.mega.uom.common.capability.entity.runic.IRunicShieldCapability;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class CapabilityHandler {
    @Mod.EventBusSubscriber
    public static class Entity {
        @SubscribeEvent
        public static void onLivingTick(LivingEvent.LivingTickEvent event) {
            IRunicShieldCapability cap;
            if ((cap = ModCapabilities.getCapability(event.getEntity(), ModCapabilities.MAGIC_SHIELD_EC)) != null)
                cap.tick(event.getEntity());
            if ((cap = ModCapabilities.getCapability(event.getEntity(), ModCapabilities.FE_SHIELD_EC)) != null)
                cap.tick(event.getEntity());
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onLivingAttack(LivingAttackEvent event) {
            IRunicShieldCapability cap;
            if ((cap = ModCapabilities.getCapability(event.getEntity(), ModCapabilities.MAGIC_SHIELD_EC)) != null) {
                cap.onHurt(event.getEntity(), event.getSource(), event.getAmount(), event);
            }
            if ((cap = ModCapabilities.getCapability(event.getEntity(), ModCapabilities.FE_SHIELD_EC)) != null) {
                cap.onHurt(event.getEntity(), event.getSource(), event.getAmount(), event);
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onLivingHurt(LivingHurtEvent event) {
            IRunicShieldCapability cap;
            if ((cap = ModCapabilities.getCapability(event.getEntity(), ModCapabilities.MAGIC_SHIELD_EC)) != null) {
                cap.dealDamage(event.getEntity(), event.getSource(), event.getAmount(), event);
            }
            if ((cap = ModCapabilities.getCapability(event.getEntity(), ModCapabilities.FE_SHIELD_EC)) != null) {
                cap.dealDamage(event.getEntity(), event.getSource(), event.getAmount(), event);
            }
        }
    }
}
