package com.huige233.transcend.util;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import sun.misc.Unsafe;

import java.lang.reflect.Field;


/** 封装 Unsafe 获取、混淆字段定位和底层对象读写，并缓存实体生命同步字段的访问器。 */
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

    @SuppressWarnings("unchecked")
    public static <T> T getObject(Object target, String field) {
        if (UNSAFE == null || target == null) return null;
        Field f = findField(target.getClass(), field);
        if (f == null) return null;
        return (T) UNSAFE.getObject(target, UNSAFE.objectFieldOffset(f));
    }
}
