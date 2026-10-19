package com.huige233.transcend.combat.protection;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 防止攻击模组通过属性层把神器目标压成零血或零护甲。
 * 只恢复首次观察到的合法基础值，不删除正常的正向装备/药水修饰符。
 */
public final class TranscendAttributeGuard {
    private static final Map<LivingEntity, Map<Attribute, Double>> BASELINES = new IdentityHashMap<>();

    private TranscendAttributeGuard() {}

    public static void enforce(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide) return;
        Map<Attribute, Double> baseline = BASELINES.computeIfAbsent(entity, ignored -> capture(entity));
        restore(entity, Attributes.MAX_HEALTH, baseline.get(Attributes.MAX_HEALTH), 1.0D);
        restore(entity, Attributes.ARMOR, baseline.get(Attributes.ARMOR), 0.0D);
        restore(entity, Attributes.ARMOR_TOUGHNESS, baseline.get(Attributes.ARMOR_TOUGHNESS), 0.0D);
    }

    private static Map<Attribute, Double> capture(LivingEntity entity) {
        Map<Attribute, Double> result = new IdentityHashMap<>();
        for (Attribute attribute : new Attribute[]{Attributes.MAX_HEALTH, Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS}) {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance != null && Double.isFinite(instance.getBaseValue())) {
                result.put(attribute, instance.getBaseValue());
            }
        }
        return result;
    }

    private static void restore(LivingEntity entity, Attribute attribute, Double baseline, double minimum) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;
        if (!Double.isFinite(instance.getBaseValue()) || instance.getBaseValue() < minimum) {
            instance.setBaseValue(baseline != null && Double.isFinite(baseline) ? baseline : minimum);
        }
        for (AttributeModifier modifier : instance.getModifiers()) {
            double amount = modifier.getAmount();
            boolean hostile = !Double.isFinite(amount)
                    || (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL && amount <= -1.0D)
                    || (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE && amount <= -1.0D)
                    || (modifier.getOperation() == AttributeModifier.Operation.ADDITION && amount < -1_000_000.0D);
            if (hostile) instance.removeModifier(modifier);
        }
    }

    public static void forget(LivingEntity entity) {
        if (entity != null) BASELINES.remove(entity);
    }
}
