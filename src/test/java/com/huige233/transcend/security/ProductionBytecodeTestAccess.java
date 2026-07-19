package com.huige233.transcend.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

final class ProductionBytecodeTestAccess {
    private static final String CONTEXT = "net/minecraftforge/network/NetworkEvent$Context";

    private ProductionBytecodeTestAccess() {
    }

    static void assertCreatorAssignment(Method placement) {
        List<Call> calls = calls(placement.getDeclaringClass(),
                method -> method.name.equals(placement.getName())
                        && method.desc.equals(Type.getMethodDescriptor(placement)));
        assertTrue(calls.stream().anyMatch(call -> call.name().equals("getUUID")),
                "setPlacedBy must derive creator UUID from the server-side placer");
        assertTrue(calls.stream().anyMatch(call -> call.owner().equals(
                        "com/huige233/transcend/block/circle/MagicCircleCoreBlockEntity")
                        && call.name().equals("setOwner")),
                "setPlacedBy must assign the placer UUID to the actual circle block entity");
    }

    static void assertHandlerDelegates(Class<?> packetType, String... mutationNames) {
        String packetOwner = Type.getInternalName(packetType);
        List<MethodCalls> methods = callsByMethod(packetType,
                method -> method.name.equals("run") || method.name.startsWith("lambda$run$"));
        boolean sawMutation = false;
        for (MethodCalls method : methods) {
            for (Call mutation : method.calls()) {
                if (!contains(mutationNames, mutation.name())) {
                    continue;
                }
                sawMutation = true;
                int sender = firstIndex(method.calls(),
                        call -> call.owner().equals(CONTEXT) && call.name().equals("getSender"));
                int validation = firstIndex(method.calls(),
                        call -> call.owner().equals(packetOwner)
                                && (call.name().equals("validateRequest")
                                || call.name().equals("isAuthorizedRequest")));
                assertTrue(sender >= 0 && sender < mutation.index(),
                        () -> packetType.getSimpleName() + " handler must derive identity from Context.getSender before "
                                + mutation.name());
                assertTrue(validation >= 0 && validation < mutation.index(),
                        () -> packetType.getSimpleName() + " handler must delegate to its validation seam before "
                                + mutation.name());
            }
        }
        assertTrue(sawMutation, () -> "No real handler mutation found in " + packetType.getSimpleName());
        assertNoClientIdentityField(packetType);
    }

    static void assertDummyPersistenceDelegatesToSuperAndCodec(Class<?> dummyType) {
        assertPersistenceMethod(dummyType, "readAdditionalSaveData", "loadOwnerFromNbt");
        assertPersistenceMethod(dummyType, "addAdditionalSaveData", "saveOwnerToNbt");
        List<Call> authorization = calls(dummyType,
                method -> method.name.equals("canConfigure") || method.name.equals("canPlayerConfigure"));
        assertTrue(authorization.stream().anyMatch(call -> call.owner().equals(Type.getInternalName(dummyType))
                        && call.name().equals("claimLegacyOwner")),
                "TestDummy configuration authorization must delegate first-claim state to the production codec");
    }

    static void assertCallsProductionMeditationSessionValidator(Class<?> packetType) {
        String packetOwner = Type.getInternalName(packetType);
        List<Call> calls = calls(packetType, method -> method.name.equals("apply"));
        int validation = firstIndex(calls,
                call -> isProductionMeditationValidator(call, packetOwner));
        assertTrue(validation >= 0,
                () -> packetType.getSimpleName() + " must call an external production meditation-session "
                        + "validator that accepts ServerPlayer and returns boolean");

        int mutation = firstIndex(calls, ProductionBytecodeTestAccess::isWandNbtMutation);
        assertTrue(mutation >= 0, () -> "No wand NBT mutation found in " + packetType.getSimpleName());
        assertTrue(validation < mutation,
                () -> packetType.getSimpleName() + " must validate the live server meditation session before "
                        + "mutating wand NBT");
    }

    private static void assertPersistenceMethod(Class<?> dummyType, String methodName, String codecName) {
        String descriptor = "(Lnet/minecraft/nbt/CompoundTag;)V";
        List<Call> calls = calls(dummyType,
                method -> method.name.equals(methodName) && method.desc.equals(descriptor));
        assertTrue(calls.stream().anyMatch(call -> call.opcode() == org.objectweb.asm.Opcodes.INVOKESPECIAL
                        && call.owner().equals("net/minecraft/world/entity/Mob")
                        && call.name().equals(methodName)),
                () -> "TestDummy." + methodName + " must invoke its superclass implementation");
        assertTrue(calls.stream().anyMatch(call -> call.owner().equals(Type.getInternalName(dummyType))
                        && call.name().equals(codecName)),
                () -> "TestDummy." + methodName + " must delegate Owner NBT to " + codecName);
    }

    private static void assertNoClientIdentityField(Class<?> packetType) {
        for (java.lang.reflect.Field field : packetType.getDeclaredFields()) {
            String identity = (field.getName() + ':' + field.getType().getName()).toLowerCase();
            assertTrue(!identity.contains("uuid") && !identity.contains("owner")
                            && !identity.contains("sender") && !identity.contains("identity")
                            && !identity.contains("serverplayer"),
                    () -> packetType.getSimpleName() + " must not carry client-supplied authorization identity: "
                            + field.getName());
        }
    }

    private static List<MethodCalls> callsByMethod(Class<?> type, Predicate<MethodNode> selector) {
        ClassNode node = classNode(type);
        List<MethodCalls> result = new ArrayList<>();
        for (MethodNode method : node.methods) {
            if (selector.test(method)) {
                result.add(new MethodCalls(method.name, calls(method)));
            }
        }
        return result;
    }

    private static List<Call> calls(Class<?> type, Predicate<MethodNode> selector) {
        List<Call> result = new ArrayList<>();
        for (MethodCalls method : callsByMethod(type, selector)) {
            result.addAll(method.calls());
        }
        return result;
    }

    private static List<Call> calls(MethodNode method) {
        List<Call> result = new ArrayList<>();
        int index = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                result.add(new Call(call.owner, call.name, call.desc, call.getOpcode(), index));
            }
            index++;
        }
        return result;
    }

    private static ClassNode classNode(Class<?> type) {
        String resource = '/' + type.getName().replace('.', '/') + ".class";
        try (InputStream input = type.getResourceAsStream(resource)) {
            if (input == null) {
                return fail("Missing production class resource " + resource);
            }
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        } catch (IOException exception) {
            throw new AssertionError("Cannot inspect production bytecode " + type.getName(), exception);
        }
    }

    private static int firstIndex(List<Call> calls, Predicate<Call> selector) {
        return calls.stream().filter(selector).mapToInt(Call::index).findFirst().orElse(-1);
    }

    private static boolean isProductionMeditationValidator(Call call, String packetOwner) {
        String ownerAndName = (call.owner() + '/' + call.name()).toLowerCase(Locale.ROOT);
        if (call.owner().equals(packetOwner)
                || !call.owner().startsWith("com/huige233/transcend/")
                || !ownerAndName.contains("meditation")
                || Type.getReturnType(call.descriptor()).getSort() != Type.BOOLEAN) {
            return false;
        }
        for (Type argument : Type.getArgumentTypes(call.descriptor())) {
            if (argument.getSort() == Type.OBJECT
                    && argument.getInternalName().equals("net/minecraft/server/level/ServerPlayer")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWandNbtMutation(Call call) {
        return call.owner().equals("net/minecraft/nbt/CompoundTag") && call.name().startsWith("put")
                || call.owner().equals("net/minecraft/nbt/ListTag") && call.name().equals("set");
    }

    private static boolean contains(String[] values, String candidate) {
        for (String value : values) {
            if (value.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    private record MethodCalls(String name, List<Call> calls) {
    }

    private record Call(String owner, String name, String descriptor, int opcode, int index) {
    }
}
