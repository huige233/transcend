package com.huige233.transcend.util;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * 通用 Unsafe 工具:直接读写原版 Minecraft/Forge 对象的字段、以及替换对象的 class 指针。
 *
 * <p>所有「针对其他 mod 的攻击/防御」都收敛到这里 —— 不再反射各家 mod 的内部类,而是直接对
 * 原版 {@code Entity/LivingEntity} 的字节码字段下手:被 coremod 包过的 getter/setter 一律绕过,
 * 被门控的方法一律不走。字段按名字在继承链上查找,因此官方/SRG 映射都能命中。</p>
 */
public final class TranscendUnsafe {

    public static final Unsafe UNSAFE = resolveUnsafe();

    private TranscendUnsafe() {
    }

    // ── 原版 MC 字段(运行时是 SRG 名,dev 是官方名);用 ObfuscationReflectionHelper 两边都能命中 ──
    public static final String SRG_ENTITY_DATA = "f_19804_";      // Entity.entityData (final)
    public static final String SRG_REMOVAL_REASON = "f_146795_";  // Entity.removalReason
    private static final String SRG_DATA_HEALTH_ID = "f_20961_";  // LivingEntity.DATA_HEALTH_ID

    private static EntityDataAccessor<Float> dataHealthId;
    private static boolean healthIdResolved;

    /** 按 SRG 名解析原版字段(dev=官方名 / prod=SRG 名 都能命中);失败返回 null。 */
    public static Field mcField(Class<?> owner, String srg) {
        try {
            return ObfuscationReflectionHelper.findField(owner, srg);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 用 Unsafe 按 SRG 名写原版字段(对象引用),绕过 final / 被包装的 setter。 */
    public static void putMcObject(Object target, Class<?> owner, String srg, Object value) {
        if (UNSAFE == null || target == null) return;
        Field f = mcField(owner, srg);
        if (f == null) return;
        UNSAFE.putObject(target, UNSAFE.objectFieldOffset(f), value);
    }

    /** 原版的 {@code LivingEntity.DATA_HEALTH_ID} 访问器(锁血与击杀都要用)。 */
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

    /** 在继承链上查找字段(含父类的私有字段)。 */
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

    /** 把 type 声明的所有非静态实例字段从 src 拷到 dst(跳过 skip 列出的字段名)。 */
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

    /** 读取静态字段(被门控的 mod 静态列表也能直接拿到)。 */
    @SuppressWarnings("unchecked")
    public static <T> T getStatic(Class<?> owner, String field) {
        if (UNSAFE == null) return null;
        Field f = findField(owner, field);
        if (f == null) return null;
        Object base = UNSAFE.staticFieldBase(f);
        return (T) UNSAFE.getObject(base, UNSAFE.staticFieldOffset(f));
    }

    /**
     * 把 ArrayList(含被覆写 mutator 的子类)清空:直接写父类 {@code size=0}、{@code elementData} 换空数组,
     * 绕过任何被覆写/门控的 mutator 和 JPMS。
     */
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

    /**
     * 用 Unsafe 改写对象头里的 class 指针,使现有实例「变成」另一个类 —— 后续虚方法分派走目标类的覆写。
     * 目标类必须是源对象类的子类且不新增实例字段(布局兼容)。这是「不反射 mod、只动原版字节码」实现
     * 通用无敌/击杀的核心手段(参考 starlight 的攻击/防御模式)。
     */
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
