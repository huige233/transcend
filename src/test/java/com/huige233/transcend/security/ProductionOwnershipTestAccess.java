package com.huige233.transcend.security;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import sun.misc.Unsafe;

final class ProductionOwnershipTestAccess {
    private static final Unsafe UNSAFE = unsafe();

    private ProductionOwnershipTestAccess() {
    }

    static <T> T allocate(Class<T> type) {
        try {
            return type.cast(UNSAFE.allocateInstance(type));
        } catch (InstantiationException exception) {
            throw new AssertionError("Cannot allocate production type " + type.getName(), exception);
        }
    }

    static UUID loadDummyOwner(CompoundTag tag) {
        prepareMinecraftEntityStatics();
        return (UUID) invokeStaticRequired(com.huige233.transcend.entity.TestDummy.class,
                "loadOwnerFromNbt", new Class<?>[]{CompoundTag.class}, tag);
    }

    static void saveDummyOwner(CompoundTag tag, UUID owner) {
        prepareMinecraftEntityStatics();
        invokeStaticRequired(com.huige233.transcend.entity.TestDummy.class,
                "saveOwnerToNbt", new Class<?>[]{CompoundTag.class, UUID.class}, tag, owner);
    }

    static UUID claimDummyOwner(UUID currentOwner, UUID serverSender, boolean adminBypass) {
        prepareMinecraftEntityStatics();
        return (UUID) invokeStaticRequired(com.huige233.transcend.entity.TestDummy.class,
                "claimLegacyOwner", new Class<?>[]{UUID.class, UUID.class, boolean.class},
                currentOwner, serverSender, adminBypass);
    }

    static boolean authorize(Object target, UUID serverSender, boolean adminBypass) {
        Method method = Arrays.stream(target.getClass().getDeclaredMethods())
                .filter(candidate -> candidate.getReturnType() == boolean.class)
                .filter(candidate -> Arrays.equals(candidate.getParameterTypes(),
                        new Class<?>[]{UUID.class, boolean.class}))
                .filter(candidate -> candidate.getName().equals("canPlayerConfigure")
                        || candidate.getName().equals("canConfigure")
                        || candidate.getName().equals("claimOrAuthorize")
                        || candidate.getName().equals("authorizeConfiguration"))
                .findFirst()
                .orElseGet(() -> fail("Missing production ownership seam on " + target.getClass().getSimpleName()
                        + ": expected boolean canPlayerConfigure/canConfigure(UUID serverSender, boolean adminBypass)"));
        return (boolean) invoke(method, Modifier.isStatic(method.getModifiers()) ? null : target,
                serverSender, adminBypass);
    }

    static boolean validate(Class<?> packetType, Class<?>[] parameterTypes, Object... arguments) {
        Method method = Arrays.stream(packetType.getDeclaredMethods())
                .filter(candidate -> candidate.getReturnType() == boolean.class)
                .filter(candidate -> Arrays.equals(candidate.getParameterTypes(), parameterTypes))
                .filter(candidate -> candidate.getName().equals("validateRequest")
                        || candidate.getName().equals("isAuthorizedRequest"))
                .findFirst()
                .orElseGet(() -> fail("Missing executable packet validation seam on "
                        + packetType.getSimpleName() + " for " + Arrays.toString(parameterTypes)));
        return (boolean) invoke(method, null, arguments);
    }

    static Object field(Object target, String name) {
        try {
            Field field = findField(target.getClass(), name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Cannot read production field " + name, exception);
        }
    }

    static void setField(Object target, String name, Object value) {
        try {
            Field field = findField(target.getClass(), name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Cannot initialize production field " + name, exception);
        }
    }

    private static Object invokeStaticRequired(Class<?> type, String name, Class<?>[] parameterTypes,
                                               Object... arguments) {
        Method method;
        try {
            method = type.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException exception) {
            return fail("Missing production codec seam " + type.getSimpleName() + "." + name);
        }
        if (!Modifier.isStatic(method.getModifiers())) {
            return fail("Production codec seam must be static: " + type.getSimpleName() + "." + name);
        }
        return invoke(method, null, arguments);
    }

    private static Object invoke(Method method, Object target, Object... arguments) {
        try {
            method.setAccessible(true);
            return method.invoke(target, arguments);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof AssertionError assertionError) {
                throw assertionError;
            }
            throw new AssertionError("Production seam threw " + cause, cause);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Cannot invoke production method " + method, exception);
        }
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> cursor = type;
        while (cursor != null) {
            try {
                return cursor.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Unsafe unsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unsafe is required for constructor-free Forge production fixtures", exception);
        }
    }

    private static synchronized void prepareMinecraftEntityStatics() {
        SharedConstants.tryDetectVersion();
        try {
            Field bootstrappedField = Bootstrap.class.getDeclaredField("isBootstrapped");
            UNSAFE.putBooleanVolatile(UNSAFE.staticFieldBase(bootstrappedField),
                    UNSAFE.staticFieldOffset(bootstrappedField), true);
            BuiltInRegistries.REGISTRY.size();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Cannot prepare Minecraft entity statics for production codec test", exception);
        }
    }
}
