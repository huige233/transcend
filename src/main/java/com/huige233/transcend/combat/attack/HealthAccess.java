package com.huige233.transcend.combat.attack;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.Capability;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.ToDoubleFunction;


/** 通过显式注册的字段、同步数据或能力适配器读写实体生命值，并保留原版回退路径。 */
public final class HealthAccess {
    /** 约定生命值读取、写入及适配器可用性检查接口。 */
    public interface Adapter<T> {
        double get(T entity);
        boolean set(T entity, double value);
        default boolean available(T entity) { return true; }
    }
    
    /** 注册生命值适配器并按最近已注册父类解析和缓存匹配结果。 */
    public static final class Registry {
        private final ConcurrentHashMap<Class<?>, Adapter<?>> registered = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<Class<?>, Optional<Adapter<?>>> resolved = new ConcurrentHashMap<>();
        public synchronized <T> void register(Class<T> type, Adapter<? super T> adapter) {
            registered.put(Objects.requireNonNull(type), Objects.requireNonNull(adapter)); resolved.clear();
        }
        @SuppressWarnings("unchecked")
        public synchronized <T> Optional<Adapter<T>> find(Class<? extends T> type) {
            return (Optional<Adapter<T>>) (Optional<?>) resolved.computeIfAbsent(type, key -> {
                for (Class<?> c = key; c != null; c = c.getSuperclass()) {
                    Adapter<?> adapter = registered.get(c);
                    if (adapter != null) return Optional.of(adapter);
                }
                return Optional.empty();
            });
        }
    }
    private static final Registry REGISTRY = new Registry();
    private HealthAccess() {}
    public static <T extends LivingEntity> void register(Class<T> type, Adapter<? super T> adapter) { REGISTRY.register(type, adapter); }
    public static Optional<Adapter<LivingEntity>> find(LivingEntity entity) { return REGISTRY.find(entity.getClass()); }
    public static float read(LivingEntity entity) {
        try {
            var adapter = find(entity);
            if (adapter.isPresent() && adapter.get().available(entity)) {
                double value = adapter.get().get(entity);
                if (Double.isFinite(value)) return (float) Math.max(0, Math.min(Float.MAX_VALUE, value));
            }
        } catch (RuntimeException ignored) {                                                 }
        return entity.getHealth();
    }
    public static boolean write(LivingEntity entity, float value) {
        if (!Float.isFinite(value) || value < 0) return false;
        boolean written = false;
        try {
            var adapter = find(entity);
            if (adapter.isPresent() && adapter.get().available(entity)) written = adapter.get().set(entity, value);
        } catch (RuntimeException ignored) {                                           }
        entity.setHealth(value);
        return written || entity.getHealth() == value;
    }
    public static <T> Adapter<T> explicitField(Class<T> owner, String exactName) {
        final Field field;
        try {
            field = owner.getDeclaredField(exactName);
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())
                    || (field.getType() != float.class && field.getType() != double.class))
                throw new IllegalArgumentException("health field must be mutable float/double");
            field.setAccessible(true);
        } catch (ReflectiveOperationException e) { throw new IllegalArgumentException("Explicit health field unavailable", e); }
        return new Adapter<>() {
            public double get(T entity) {
                try { return ((Number) field.get(entity)).doubleValue(); }
                catch (IllegalAccessException e) { throw new IllegalStateException(e); }
            }
            public boolean set(T entity, double value) {
                if (!Double.isFinite(value) || value < 0) return false;
                try {
                    if (field.getType() == float.class) field.setFloat(entity, (float) Math.min(Float.MAX_VALUE, value));
                    else field.setDouble(entity, value);
                    return true;
                } catch (IllegalAccessException e) { return false; }
            }
        };
    }
    public static <T extends LivingEntity> Adapter<T> synced(EntityDataAccessor<Float> accessor) {
        return new Adapter<>() {
            public double get(T entity) { return entity.getEntityData().get(accessor); }
            public boolean set(T entity, double value) {
                if (!Double.isFinite(value) || value < 0) return false;
                entity.getEntityData().set(accessor, (float) Math.min(Float.MAX_VALUE, value)); return true;
            }
        };
    }
    public static <T extends LivingEntity, C> Adapter<T> capability(Capability<C> capability,
            ToDoubleFunction<C> reader, BiConsumer<C, Double> writer) {
        return new Adapter<>() {
            public boolean available(T entity) { return entity.getCapability(capability).isPresent(); }
            public double get(T entity) { return entity.getCapability(capability).resolve().map(reader::applyAsDouble).orElse(Double.NaN); }
            public boolean set(T entity, double value) {
                if (!Double.isFinite(value) || value < 0) return false;
                var instance = entity.getCapability(capability).resolve();
                instance.ifPresent(c -> writer.accept(c, value)); return instance.isPresent();
            }
        };
    }
}
