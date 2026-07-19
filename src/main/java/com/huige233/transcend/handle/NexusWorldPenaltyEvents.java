package com.huige233.transcend.handle;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.world.nexus.NexusWorldPenalty;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NexusWorldPenaltyEvents {

    private static final UUID NEXUS_HP_MODIFIER_UUID =
            UUID.fromString("a3f1c4e7-2b8d-4f0a-9c6e-1d5b7a3c9f2e");
    private static final String NEXUS_HP_MODIFIER_NAME = "nexus_frailty_maxhp";

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 20 != 0) return;

        double mod = NexusWorldPenalty.getMaxHealthModifier(sp);
        if (mod < 0) {
            applyMultiplyMod(sp, Attributes.MAX_HEALTH, NEXUS_HP_MODIFIER_UUID,
                    NEXUS_HP_MODIFIER_NAME, mod);

            float maxHp = (float) sp.getAttributeValue(Attributes.MAX_HEALTH);
            if (sp.getHealth() > maxHp) sp.setHealth(maxHp);
        } else {
            removeMod(sp, Attributes.MAX_HEALTH, NEXUS_HP_MODIFIER_UUID);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        float mult = NexusWorldPenalty.getHealMultiplier(sp);
        if (mult < 1.0F) {
            event.setAmount(event.getAmount() * mult);
        }
    }

    private static void applyMultiplyMod(ServerPlayer sp,
                                          net.minecraft.world.entity.ai.attributes.Attribute attr,
                                          UUID uuid, String name, double amount) {
        AttributeInstance inst = sp.getAttribute(attr);
        if (inst == null) return;
        if (inst.getModifier(uuid) == null) {
            inst.addTransientModifier(new AttributeModifier(
                    uuid, name, amount, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    private static void removeMod(ServerPlayer sp,
                                   net.minecraft.world.entity.ai.attributes.Attribute attr,
                                   UUID uuid) {
        AttributeInstance inst = sp.getAttribute(attr);
        if (inst != null && inst.getModifier(uuid) != null) {
            inst.removeModifier(uuid);
        }
    }
}
