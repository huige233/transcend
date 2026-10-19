package com.huige233.transcend.combat.attack;

import com.huige233.transcend.util.TranscendUnsafe;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 超越自己的多层生命账本。它不依赖 OmniMobs、梦幻终焉或任何第三方 API，
 * 而是从实体的合法可变生命字段、同步生命数据和原版生命入口建立本地快照。
 */
public final class TranscendHealthLedger {
    private static final Map<Class<?>, Field[]> FIELDS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method[]> SETTERS = new ConcurrentHashMap<>();
    private static final String[] HEALTH_WORDS = {"health", "hp", "life", "hitpoint", "hit_points"};
    private static final String[] REJECTED_WORDS = {"maxhealth", "maximumhealth", "shield", "energy",
            "stamina", "mana", "absorption", "armor", "armour", "barrier"};

    private TranscendHealthLedger() {}

    public static void forceSet(LivingEntity entity, float value) {
        if (entity == null || entity.level().isClientSide || !Float.isFinite(value)) return;
        float finite = Math.max(0.0F, value);
        // 先建立候选快照；不能在写入原版生命后再盲扫，否则 maxHealth、护盾或能量字段
        // 可能因为名字相似而被误判为生命池。
        Field[] fields = FIELDS.computeIfAbsent(entity.getClass(), c -> discoverFields(c, entity));
        Method[] setters = SETTERS.computeIfAbsent(entity.getClass(), c -> discoverSetters(c, entity));
        // 先写自己的显式适配器；如果实体没有适配器，再回退到原版入口。
        HealthAccess.write(entity, finite);
        writeVanillaSyncedHealth(entity, finite);
        writeDiscoveredFields(entity, fields, finite);
        invokeDiscoveredSetters(entity, setters, finite);
        // 第三方 setter 可能在调用过程中回写原版血量，最后再做一次有限值收敛。
        writeVanillaSyncedHealth(entity, finite);
    }

    /**
     * 读取超越账本能观察到的生命值。显式适配器优先，原版值作为最终回退。
     * 该方法只读，不会触发任何第三方 setter，适合在修复后做收敛校验。
     */
    public static float read(LivingEntity entity) {
        if (entity == null) return 0.0F;
        float vanilla = entity.getHealth();
        try {
            float adapted = HealthAccess.read(entity);
            if (Float.isFinite(adapted) && adapted >= 0.0F) return adapted;
        } catch (Throwable ignored) { }
        return Float.isFinite(vanilla) ? Math.max(0.0F, vanilla) : 0.0F;
    }

    /** 在强制写入后再次校验原版同步生命，避免 setter 回写 NaN/负数。 */
    public static void converge(LivingEntity entity, float value) {
        if (entity == null || entity.level().isClientSide) return;
        float finite = Float.isFinite(value) ? Math.max(0.0F, value) : 0.0F;
        try {
            if (!Float.isFinite(entity.getHealth()) || Math.abs(entity.getHealth() - finite) > 0.0001F) {
                writeVanillaSyncedHealth(entity, finite);
            }
        } catch (Throwable ignored) {
            writeVanillaSyncedHealth(entity, finite);
        }
    }

    private static void writeVanillaSyncedHealth(LivingEntity entity, float value) {
        try {
            EntityDataAccessor<Float> id = TranscendUnsafe.dataHealthId();
            if (id != null) entity.getEntityData().set(id, value);
        } catch (Throwable ignored) {}
    }

    private static void writeDiscoveredFields(LivingEntity entity, Field[] fields, float value) {
        for (Field field : fields) {
            try {
                if (field.getType() == float.class) field.setFloat(entity, value);
                else if (field.getType() == double.class) field.setDouble(entity, value);
                else if (field.getType() == int.class) field.setInt(entity, (int) value);
                else if (field.getType() == long.class) field.setLong(entity, (long) value);
            } catch (Throwable ignored) {}
        }
    }

    private static void invokeDiscoveredSetters(LivingEntity entity, Method[] setters, float value) {
        for (Method method : setters) {
            try {
                if (method.getParameterTypes()[0] == float.class || method.getParameterTypes()[0] == Float.class)
                    method.invoke(entity, value);
                else if (method.getParameterTypes()[0] == double.class || method.getParameterTypes()[0] == Double.class)
                    method.invoke(entity, (double) value);
            } catch (Throwable ignored) {}
        }
    }

    private static Field[] discoverFields(Class<?> type, LivingEntity sample) {
        java.util.ArrayList<Field> result = new java.util.ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())
                        || field.isSynthetic()) continue;
                if (!isHealthName(field.getName()) || isRejectedName(field.getName())) continue;
                if (field.getType() != float.class && field.getType() != double.class
                        && field.getType() != int.class && field.getType() != long.class) continue;
                try {
                    field.setAccessible(true);
                    // 只有明确的生命字段，或当前值与原版生命值/上限相近的字段才纳入账本。
                    // 这一步避免把 mana、energy、护盾等同为生命。
                    if (isPlausibleHealthField(field, sample)) result.add(field);
                } catch (Throwable ignored) {}
            }
        }
        return result.toArray(Field[]::new);
    }

    private static Method[] discoverSetters(Class<?> type, LivingEntity sample) {
        java.util.ArrayList<Method> result = new java.util.ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method method : c.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1
                        || !method.getName().toLowerCase(java.util.Locale.ROOT).startsWith("set")
                        || !isHealthName(method.getName()) || isRejectedName(method.getName())
                        || method.getName().equalsIgnoreCase("setHealth")) continue;
                Class<?> p = method.getParameterTypes()[0];
                if (p != float.class && p != Float.class && p != double.class && p != Double.class) continue;
                try { method.setAccessible(true); result.add(method); } catch (Throwable ignored) {}
            }
        }
        return result.toArray(Method[]::new);
    }

    private static boolean isHealthName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String word : HEALTH_WORDS) if (lower.contains(word)) return true;
        return false;
    }

    private static boolean isRejectedName(String name) {
        String lower = name.toLowerCase(Locale.ROOT).replace("_", "");
        for (String word : REJECTED_WORDS) if (lower.contains(word)) return true;
        return false;
    }

    private static boolean isPlausibleHealthField(Field field, LivingEntity sample) {
        String name = field.getName().toLowerCase(Locale.ROOT);
        if (name.equals("health") || name.equals("hp") || name.equals("life")
                || name.equals("hitpoints") || name.equals("hit_points")) return true;
        try {
            Object raw = field.get(sample);
            if (!(raw instanceof Number n)) return false;
            double value = n.doubleValue();
            if (!Double.isFinite(value) || value < 0) return false;
            double current = sample.getHealth();
            double maximum = sample.getMaxHealth();
            double tolerance = Math.max(4.0D, Math.max(Math.abs(current), Math.abs(maximum)) * 0.05D);
            return Math.abs(value - current) <= tolerance || Math.abs(value - maximum) <= tolerance;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
