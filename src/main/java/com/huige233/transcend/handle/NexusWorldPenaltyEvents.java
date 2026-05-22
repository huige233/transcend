package com.huige233.transcend.handle;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.world.nexus.NexusWorldPenalty;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * 枢纽破碎的全局惩罚事件处理器。
 *
 * - 脆弱(FRAILTY)：承伤 +15%，护甲/韧性 -4
 * - 怜悯(MERCY)：正面buff持续时间 -40%，等级 -1
 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NexusWorldPenaltyEvents {

    private static final UUID NEXUS_ARMOR_MODIFIER_UUID =
            UUID.fromString("b8f7a3c1-9d4e-4a2b-8f1c-3e5d7a9b2c4f");
    private static final UUID NEXUS_TOUGHNESS_MODIFIER_UUID =
            UUID.fromString("c9a8b4d2-0e5f-4b3c-9a2d-4f6e8a0c3d5a");
    private static final String NEXUS_ARMOR_MODIFIER_NAME = "nexus_frailty_armor";
    private static final String NEXUS_TOUGHNESS_MODIFIER_NAME = "nexus_frailty_toughness";

    // ─── 脆弱(FRAILTY)：承伤 +15% ───────────────────────────────────
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;

        MinecraftServer server = target.getServer();
        if (server == null || !NexusWorldPenalty.hasAnyPenalty(server)) return;

        // 脆弱：承伤 +15%
        float defMult = NexusWorldPenalty.getDamageTakenMultiplier(server);
        if (defMult > 1.0F) {
            event.setAmount(event.getAmount() * defMult);
        }
    }

    // ─── 脆弱(FRAILTY)：护甲/韧性 -4 — 在 tick 中持续应用 ──────────
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        // 每 20 tick 检查一次（降低性能开销）
        if (entity.tickCount % 20 != 0) return;

        MinecraftServer server = entity.getServer();
        if (server == null) return;

        float armorReduction = NexusWorldPenalty.getArmorReduction(server);
        float toughnessReduction = NexusWorldPenalty.getToughnessReduction(server);

        if (armorReduction > 0) {
            applyModifier(entity, Attributes.ARMOR, NEXUS_ARMOR_MODIFIER_UUID,
                    NEXUS_ARMOR_MODIFIER_NAME, -armorReduction);
        } else {
            removeModifier(entity, Attributes.ARMOR, NEXUS_ARMOR_MODIFIER_UUID);
        }

        if (toughnessReduction > 0) {
            applyModifier(entity, Attributes.ARMOR_TOUGHNESS, NEXUS_TOUGHNESS_MODIFIER_UUID,
                    NEXUS_TOUGHNESS_MODIFIER_NAME, -toughnessReduction);
        } else {
            removeModifier(entity, Attributes.ARMOR_TOUGHNESS, NEXUS_TOUGHNESS_MODIFIER_UUID);
        }
    }

    // ─── 怜悯(MERCY)：正面buff持续时间 -40%，等级 -1 ────────────────
    @SubscribeEvent
    public static void onMobEffectAdded(MobEffectEvent.Added event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        MinecraftServer server = entity.getServer();
        if (server == null) return;

        float durationMult = NexusWorldPenalty.getPositiveEffectDurationMultiplier(server);
        int ampOffset = NexusWorldPenalty.getPositiveEffectAmplifierOffset(server);
        if (durationMult >= 1.0F && ampOffset >= 0) return;

        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null || !instance.getEffect().isBeneficial()) return;
        if (instance.getDuration() <= 40) return;

        int newDuration = Math.max(20, (int)(instance.getDuration() * durationMult));
        int newAmplifier = Math.max(0, instance.getAmplifier() + ampOffset);

        entity.removeEffect(instance.getEffect());
        entity.addEffect(new MobEffectInstance(
                instance.getEffect(),
                newDuration,
                newAmplifier,
                instance.isAmbient(),
                instance.isVisible(),
                instance.showIcon()));
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private static void applyModifier(LivingEntity entity,
                                       net.minecraft.world.entity.ai.attributes.Attribute attr,
                                       UUID uuid, String name, double amount) {
        AttributeInstance attrInst = entity.getAttribute(attr);
        if (attrInst == null) return;
        if (attrInst.getModifier(uuid) == null) {
            attrInst.addTransientModifier(new AttributeModifier(
                    uuid, name, amount, AttributeModifier.Operation.ADDITION));
        }
    }

    private static void removeModifier(LivingEntity entity,
                                        net.minecraft.world.entity.ai.attributes.Attribute attr,
                                        UUID uuid) {
        AttributeInstance attrInst = entity.getAttribute(attr);
        if (attrInst != null && attrInst.getModifier(uuid) != null) {
            attrInst.removeModifier(uuid);
        }
    }
}
