package com.huige233.transcend.util;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public final class TranscendUnsafe {

    public static final Unsafe UNSAFE = resolveUnsafe();

    private TranscendUnsafe() {
    }

    public static final String SRG_ENTITY_DATA = "f_19804_";
    public static final String SRG_REMOVAL_REASON = "f_146795_";
    private static final String SRG_DATA_HEALTH_ID = "f_20961_";

    private static EntityDataAccessor<Float> dataHealthId;
    private static boolean healthIdResolved;

    public static Field mcField(Class<?> owner, String srg) {
        try {
            return ObfuscationReflectionHelper.findField(owner, srg);
        } catch (Throwable t) {
            return null;
        }
    }

    public static void putMcObject(Object target, Class<?> owner, String srg, Object value) {
        if (UNSAFE == null || target == null) return;
        Field f = mcField(owner, srg);
        if (f == null) return;
        UNSAFE.putObject(target, UNSAFE.objectFieldOffset(f), value);
    }

    @SuppressWarnings("unchecked")
    public static EntityDataAccessor<Float> dataHealthId() {
        if (!healthIdResolved) {
            healthIdResolved = true;
            try {
                Field f = mcField(LivingEntity.class, SRG_DATA_HEALTH_ID);
                if (f != null) dataHealthId = (EntityDataAccessor<Float>) f.get(null);
            } catch (Throwable ignored) {
            }
        }
        return dataHealthId;
    }

    private static Unsafe resolveUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (Throwable t) {
            try {
                java.lang.reflect.Constructor<Unsafe> c = Unsafe.class.getDeclaredConstructor();
                c.setAccessible(true);
                return c.newInstance();
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    private static Field findField(Class<?> owner, String name) {
        for (Class<?> c = owner; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    public static void putObject(Object target, String field, Object value) {
        if (UNSAFE == null || target == null) return;
        Field f = findField(target.getClass(), field);
        if (f == null) return;
        UNSAFE.putObject(target, UNSAFE.objectFieldOffset(f), value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getObject(Object target, String field) {
        if (UNSAFE == null || target == null) return null;
        Field f = findField(target.getClass(), field);
        if (f == null) return null;
        return (T) UNSAFE.getObject(target, UNSAFE.objectFieldOffset(f));
    }

    public static void putFloat(Object target, String field, float value) {
        if (UNSAFE == null || target == null) return;
        Field f = findField(target.getClass(), field);
        if (f == null) return;
        UNSAFE.putFloat(target, UNSAFE.objectFieldOffset(f), value);
    }

    public static void putInt(Object target, String field, int value) {
        if (UNSAFE == null || target == null) return;
        Field f = findField(target.getClass(), field);
        if (f == null) return;
        UNSAFE.putInt(target, UNSAFE.objectFieldOffset(f), value);
    }

    public static void putBoolean(Object target, String field, boolean value) {
        if (UNSAFE == null || target == null) return;
        Field f = findField(target.getClass(), field);
        if (f == null) return;
        UNSAFE.putBoolean(target, UNSAFE.objectFieldOffset(f), value);
    }

    public static void copyFields(Class<?> type, Object src, Object dst, String... skip) {
        if (UNSAFE == null || src == null || dst == null) return;
        java.util.Set<String> skipSet = new java.util.HashSet<>(java.util.Arrays.asList(skip));
        for (Field f : type.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
            if (skipSet.contains(f.getName())) continue;
            long off = UNSAFE.objectFieldOffset(f);
            Class<?> t = f.getType();
            if (t == int.class) UNSAFE.putInt(dst, off, UNSAFE.getInt(src, off));
            else if (t == long.class) UNSAFE.putLong(dst, off, UNSAFE.getLong(src, off));
            else if (t == boolean.class) UNSAFE.putBoolean(dst, off, UNSAFE.getBoolean(src, off));
            else if (t == float.class) UNSAFE.putFloat(dst, off, UNSAFE.getFloat(src, off));
            else if (t == double.class) UNSAFE.putDouble(dst, off, UNSAFE.getDouble(src, off));
            else if (t == byte.class) UNSAFE.putByte(dst, off, UNSAFE.getByte(src, off));
            else if (t == short.class) UNSAFE.putShort(dst, off, UNSAFE.getShort(src, off));
            else if (t == char.class) UNSAFE.putChar(dst, off, UNSAFE.getChar(src, off));
            else UNSAFE.putObject(dst, off, UNSAFE.getObject(src, off));
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getStatic(Class<?> owner, String field) {
        if (UNSAFE == null) return null;
        Field f = findField(owner, field);
        if (f == null) return null;
        Object base = UNSAFE.staticFieldBase(f);
        return (T) UNSAFE.getObject(base, UNSAFE.staticFieldOffset(f));
    }

    public static void emptyArrayList(Object list) {
        if (UNSAFE == null || !(list instanceof java.util.ArrayList)) return;
        Field size = findField(java.util.ArrayList.class, "size");
        if (size != null) {
            UNSAFE.putInt(list, UNSAFE.objectFieldOffset(size), 0);
        }
        Field data = findField(java.util.ArrayList.class, "elementData");
        if (data != null) {
            UNSAFE.putObject(list, UNSAFE.objectFieldOffset(data), new Object[0]);
        }
    }

    public static void setClass(Object object, Class<?> targetClass) {
        if (UNSAFE == null || object == null || targetClass == null) return;
        try {
            try {
                UNSAFE.ensureClassInitialized(targetClass);
            } catch (Throwable ignored) {
            }
            int klass = UNSAFE.getIntVolatile(UNSAFE.allocateInstance(targetClass), UNSAFE.addressSize());
            UNSAFE.putIntVolatile(object, UNSAFE.addressSize(), klass);
        } catch (Throwable ignored) {
        }
    }
}
