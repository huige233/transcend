package com.huige233.transcend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

/** 分析假人源码和字节码，确保公共类不链接客户端界面且只有服务端授权通过后才能发送开屏包。 */
class TestDummyDedicatedClassloadingTest {
    private static final String DUMMY_OWNER =
            "com/huige233/transcend/entity/TestDummy";
    private static final String SCREEN_OWNER =
            "com/huige233/transcend/" + "client/TestDummyScreen";
    private static final String CLIENT_PREFIX = "net/minecraft/" + "client";
    private static final String LEVEL_OWNER = "net/minecraft/world/level/Level";
    private static final String RESULT_OWNER = "net/minecraft/world/InteractionResult";
    private static final String SIMPLE_CHANNEL_OWNER =
            "net/minecraftforge/network/simple/SimpleChannel";
    private static final String MOB_INTERACT_DESC =
            "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)"
                    + "Lnet/minecraft/world/InteractionResult;";
    @Test
    void testDummyCommonBytecodeDoesNotLinkClientGuiClasses() throws IOException {
        byte[] bytecode = classBytes();
        String constantPool = new String(bytecode, StandardCharsets.ISO_8859_1);
        assertFalse(constantPool.contains(CLIENT_PREFIX + '/'),
                "TestDummy common bytecode must not link net.minecraft.client classes");
        assertFalse(constantPool.contains("net.minecraft.client"),
                "TestDummy constant pool must not contain dotted client class names");
        assertFalse(constantPool.contains(SCREEN_OWNER),
                "TestDummy common bytecode must not link TestDummyScreen");
        assertFalse(constantPool.contains(SCREEN_OWNER.replace('/', '.')),
                "TestDummy constant pool must not contain dotted TestDummyScreen names");

        ClassNode dummy = classNode(bytecode);
        for (FieldNode field : dummy.fields) {
            assertAllowed(field.desc, "field descriptor " + field.name);
            assertAllowed(field.signature, "field signature " + field.name);
            assertFalse(referencesForbidden(field.value),
                    () -> "TestDummy field constant links client GUI code: " + field.name);
        }
        for (MethodNode method : dummy.methods) {
            assertAllowed(method.desc, "method descriptor " + method.name);
            assertAllowed(method.signature, "method signature " + method.name);
            for (String exception : method.exceptions) {
                assertAllowed(exception, "method exception " + method.name);
            }
            for (AbstractInsnNode instruction : method.instructions) {
                assertFalse(referencesForbidden(instruction),
                        () -> "TestDummy instruction links client GUI code in "
                                + method.name + method.desc);
            }
        }
    }

    @Test
    void equipmentAndScreenChangesRequireServerAuthorization() throws IOException {
        ClassNode dummy = classNode(classBytes());
        MethodNode interaction = method(dummy, "mobInteract", MOB_INTERACT_DESC);
        List<AbstractInsnNode> code = instructions(interaction);
        List<Integer> authorizations = new ArrayList<>();
        List<Integer> protectedOperations = new ArrayList<>();
        for (int i = 0; i < code.size(); i++) {
            if (methodCall(code.get(i), DUMMY_OWNER, "canConfigure",
                    "(Lnet/minecraft/server/level/ServerPlayer;)Z")) authorizations.add(i);
            if (code.get(i) instanceof MethodInsnNode call
                    && ((call.owner.equals(SIMPLE_CHANNEL_OWNER) && call.name.equals("send"))
                    || call.name.equals("setItemSlot") || call.name.equals("setItemInHand"))) {
                protectedOperations.add(i);
            }
        }
        assertEquals(3, protectedOperations.size(), "Equipment swap and screen send must be covered");
        assertFalse(authorizations.isEmpty());
        for (int authorization : authorizations) {
            int branch = authorization + 1;
            assertEquals(Opcodes.IFNE, code.get(branch).getOpcode(),
                    "Only successful authorization may continue");
            assertTrue(fieldAccess(code.get(branch + 1), Opcodes.GETSTATIC, RESULT_OWNER,
                    "FAIL", "Lnet/minecraft/world/InteractionResult;"));
            assertImmediateReturn(code, branch + 1, "Denied authorization");
            for (int operation : protectedOperations) {
                assertFalse(reachable(interaction, code, branch + 1, operation, -1),
                        "Denied authorization must not mutate equipment or send packets");
            }
        }
        int clientSide = uniqueIndex(code, instruction -> fieldAccess(instruction,
                Opcodes.GETFIELD, LEVEL_OWNER, "isClientSide", "Z"), "logical-side test");
        for (int operation : protectedOperations) {
            assertTrue(dominates(interaction, clientSide, operation));
            assertTrue(authorizations.stream().anyMatch(auth -> dominates(interaction, auth, operation)),
                    "Every mutation/send must be dominated by authorization");
        }
        assertEquals(1, classCallCount(dummy, SIMPLE_CHANNEL_OWNER, "send"));
        String network = Files.readString(Path.of(
                "src/main/java/com/huige233/transcend/handle/NetworkHandler.java"));
        assertTrue(network.contains("S2COpenTestDummyScreen::run, Optional.of(NetworkDirection.PLAY_TO_CLIENT)"));
    }

    private static byte[] classBytes() throws IOException {
        String resource = TestDummy.class.getName().replace('.', '/') + ".class";
        try (InputStream input = TestDummy.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input, "Missing compiled TestDummy class");
            return input.readAllBytes();
        }
    }

    private static ClassNode classNode(byte[] bytecode) {
        ClassNode node = new ClassNode();
        new ClassReader(bytecode).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return node;
    }

    private static MethodNode method(ClassNode owner, String name, String descriptor) {
        List<MethodNode> matches = owner.methods.stream()
                .filter(method -> method.name.equals(name) && method.desc.equals(descriptor))
                .toList();
        assertEquals(1, matches.size(), () -> "Missing or duplicate method " + name + descriptor);
        return matches.get(0);
    }

    private static List<AbstractInsnNode> instructions(MethodNode method) {
        List<AbstractInsnNode> result = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction.getOpcode() >= 0) {
                result.add(instruction);
            }
        }
        return result;
    }

    private static int uniqueIndex(List<AbstractInsnNode> code,
                                   Predicate<AbstractInsnNode> selector,
                                   String description) {
        List<Integer> matches = new ArrayList<>();
        for (int index = 0; index < code.size(); index++) {
            if (selector.test(code.get(index))) {
                matches.add(index);
            }
        }
        assertEquals(1, matches.size(),
                () -> description + " must occur exactly once in mobInteract bytecode");
        return matches.get(0);
    }

    private static void assertImmediateReturn(List<AbstractInsnNode> code, int result,
                                              String name) {
        assertTrue(result + 1 < code.size(), () -> name + " result has no return");
        assertEquals(Opcodes.ARETURN, code.get(result + 1).getOpcode(),
                () -> name + " must be returned immediately");
    }

    private static boolean dominates(MethodNode method, int dominator, int target) {
        List<AbstractInsnNode> code = instructions(method);
        return reachable(method, code, 0, target, -1)
                && !reachable(method, code, 0, target, dominator);
    }

    private static boolean reachable(MethodNode method, List<AbstractInsnNode> code,
                                     int start, int target, int blocked) {
        Set<Integer> seen = new HashSet<>();
        ArrayDeque<Integer> pending = new ArrayDeque<>();
        pending.add(start);
        while (!pending.isEmpty()) {
            int current = pending.removeFirst();
            if (current == blocked || !seen.add(current)) {
                continue;
            }
            if (current == target) {
                return true;
            }
            pending.addAll(successors(method, code, current));
        }
        return false;
    }

    private static List<Integer> successors(MethodNode method, List<AbstractInsnNode> code,
                                            int index) {
        AbstractInsnNode instruction = code.get(index);
        int opcode = instruction.getOpcode();
        List<Integer> result = new ArrayList<>();
        if (instruction instanceof JumpInsnNode jump) {
            result.add(labelTarget(method, code, jump.label));
            if (opcode != Opcodes.GOTO && opcode != Opcodes.JSR && index + 1 < code.size()) {
                result.add(index + 1);
            }
            return result;
        }
        if (instruction instanceof LookupSwitchInsnNode lookup) {
            result.add(labelTarget(method, code, lookup.dflt));
            for (LabelNode label : lookup.labels) {
                result.add(labelTarget(method, code, label));
            }
            return result;
        }
        if (instruction instanceof TableSwitchInsnNode table) {
            result.add(labelTarget(method, code, table.dflt));
            for (LabelNode label : table.labels) {
                result.add(labelTarget(method, code, label));
            }
            return result;
        }
        if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)
                || opcode == Opcodes.ATHROW) {
            return result;
        }
        if (index + 1 < code.size()) {
            result.add(index + 1);
        }
        return result;
    }

    private static int labelTarget(MethodNode method, List<AbstractInsnNode> code,
                                   LabelNode label) {
        Map<AbstractInsnNode, Integer> indices = new IdentityHashMap<>();
        for (int index = 0; index < code.size(); index++) {
            indices.put(code.get(index), index);
        }
        AbstractInsnNode target = label;
        while (target != null && target.getOpcode() < 0) {
            target = target.getNext();
        }
        Integer index = indices.get(target);
        assertNotNull(index, () -> "Branch label in " + method.name + " has no target");
        return index;
    }

    private static boolean fieldAccess(AbstractInsnNode instruction, int opcode,
                                       String owner, String name, String descriptor) {
        return instruction instanceof FieldInsnNode field
                && field.getOpcode() == opcode
                && field.owner.equals(owner)
                && field.name.equals(name)
                && field.desc.equals(descriptor);
    }

    private static boolean methodCall(AbstractInsnNode instruction, String owner,
                                      String name, String descriptor) {
        return instruction instanceof MethodInsnNode call
                && call.owner.equals(owner)
                && call.name.equals(name)
                && call.desc.equals(descriptor);
    }

    private static int classCallCount(ClassNode owner, String callOwner, String name) {
        int count = 0;
        for (MethodNode method : owner.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                        && call.owner.equals(callOwner) && call.name.equals(name)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void assertAllowed(String value, String context) {
        if (value != null) {
            assertFalse(referencesForbidden(value), () -> context + " links client GUI code");
        }
    }

    private static boolean referencesForbidden(AbstractInsnNode instruction) {
        if (instruction instanceof FieldInsnNode field) {
            return referencesForbidden(field.owner) || referencesForbidden(field.desc);
        }
        if (instruction instanceof MethodInsnNode call) {
            return referencesForbidden(call.owner) || referencesForbidden(call.desc);
        }
        if (instruction instanceof TypeInsnNode type) {
            return referencesForbidden(type.desc);
        }
        if (instruction instanceof MultiANewArrayInsnNode array) {
            return referencesForbidden(array.desc);
        }
        if (instruction instanceof LdcInsnNode constant) {
            return referencesForbidden(constant.cst);
        }
        if (instruction instanceof InvokeDynamicInsnNode dynamic) {
            if (referencesForbidden(dynamic.desc) || referencesForbidden(dynamic.bsm)) {
                return true;
            }
            for (Object argument : dynamic.bsmArgs) {
                if (referencesForbidden(argument)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean referencesForbidden(Object value) {
        if (value instanceof String text) {
            return referencesForbidden(text);
        }
        if (value instanceof Type type) {
            return referencesForbidden(type.getDescriptor());
        }
        if (value instanceof Handle handle) {
            return referencesForbidden(handle.getOwner())
                    || referencesForbidden(handle.getDesc());
        }
        if (value instanceof ConstantDynamic dynamic) {
            if (referencesForbidden(dynamic.getDescriptor())
                    || referencesForbidden(dynamic.getBootstrapMethod())) {
                return true;
            }
            for (int index = 0; index < dynamic.getBootstrapMethodArgumentCount(); index++) {
                if (referencesForbidden(dynamic.getBootstrapMethodArgument(index))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean referencesForbidden(String value) {
        return value.contains(CLIENT_PREFIX)
                || value.contains("net.minecraft.client")
                || value.contains(SCREEN_OWNER)
                || value.contains(SCREEN_OWNER.replace('/', '.'));
    }
}
