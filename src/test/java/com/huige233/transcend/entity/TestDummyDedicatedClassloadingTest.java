package com.huige233.transcend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
import java.util.regex.Pattern;
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
import org.objectweb.asm.tree.VarInsnNode;

/** 分析假人源码和字节码，确保公共类不链接客户端界面且只有服务端授权通过后才能发送开屏包。 */
class TestDummyDedicatedClassloadingTest {
    private static final Path DUMMY_SOURCE = Path.of(
            "src/main/java/com/huige233/transcend/entity/TestDummy.java");
    private static final String DUMMY_OWNER =
            "com/huige233/transcend/entity/TestDummy";
    private static final String SCREEN_OWNER =
            "com/huige233/transcend/" + "client/TestDummyScreen";
    private static final String CLIENT_PREFIX = "net/minecraft/" + "client";
    private static final String SERVER_PLAYER_OWNER =
            "net/minecraft/server/level/ServerPlayer";
    private static final String PLAYER_OWNER = "net/minecraft/world/entity/player/Player";
    private static final String LEVEL_OWNER = "net/minecraft/world/level/Level";
    private static final String HAND_OWNER = "net/minecraft/world/InteractionHand";
    private static final String RESULT_OWNER = "net/minecraft/world/InteractionResult";
    private static final String NETWORK_HANDLER_OWNER =
            "com/huige233/transcend/handle/NetworkHandler";
    private static final String SIMPLE_CHANNEL_OWNER =
            "net/minecraftforge/network/simple/SimpleChannel";
    private static final String PACKET_DISTRIBUTOR_OWNER =
            "net/minecraftforge/network/PacketDistributor";
    private static final String PACKET_TARGET_DESC =
            "Lnet/minecraftforge/network/PacketDistributor$PacketTarget;";
    private static final String PACKET_OWNER =
            "com/huige233/transcend/network/S2COpenTestDummyScreen";
    private static final String MOB_INTERACT_DESC =
            "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)"
                    + "Lnet/minecraft/world/InteractionResult;";
    private static final Pattern AUTHORIZED_SOURCE = Pattern.compile(
            "\\s*if\\s*\\(\\s*!player\\.isShiftKeyDown\\(\\)\\s*\\|\\|\\s*"
                    + "hand\\s*!=\\s*InteractionHand\\.MAIN_HAND\\s*\\)\\s*\\{\\s*"
                    + "return\\s+InteractionResult\\.PASS\\s*;\\s*}\\s*"
                    + "if\\s*\\(\\s*player\\.level\\(\\)\\.isClientSide\\s*\\)\\s*\\{\\s*"
                    + "return\\s+InteractionResult\\.SUCCESS\\s*;\\s*}\\s*"
                    + "if\\s*\\(\\s*!\\(\\s*player\\s+instanceof\\s+ServerPlayer\\s+serverPlayer"
                    + "\\s*\\)\\s*\\|\\|\\s*!canConfigure\\(\\s*serverPlayer\\s*\\)\\s*\\)"
                    + "\\s*\\{\\s*return\\s+InteractionResult\\.FAIL\\s*;\\s*}\\s*"
                    + "NetworkHandler\\.CHANNEL\\.send\\s*\\(\\s*"
                    + "PacketDistributor\\.PLAYER\\.with\\s*\\(\\s*\\(\\)\\s*->\\s*serverPlayer\\s*\\)"
                    + "\\s*,\\s*new\\s+S2COpenTestDummyScreen\\s*\\(\\s*this\\.getId\\(\\)\\s*\\)"
                    + "\\s*\\)\\s*;\\s*return\\s+InteractionResult\\.CONSUME\\s*;\\s*");

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
    void screenOpenUsesPlayToClientPacketAfterServerAuthorization() throws IOException {
        String source = Files.readString(DUMMY_SOURCE);
        String body = mobInteractBody(source);
        assertTrue(AUTHORIZED_SOURCE.matcher(body).matches(),
                "TestDummy must authorize on the server before requesting the client screen");
        assertEquals(1, count(source,
                "import com.huige233.transcend.handle.NetworkHandler;"));
        assertEquals(1, count(source,
                "import com.huige233.transcend.network.S2COpenTestDummyScreen;"));
        assertEquals(1, count(source,
                "import net.minecraftforge.network.PacketDistributor;"));
        assertEquals(1, count(body, "NetworkHandler.CHANNEL.send"),
                "mobInteract must contain exactly one send boundary");
        assertEquals(1, count(body, "PacketDistributor.PLAYER.with(() -> serverPlayer)"),
                "The packet target must be supplied by the authorized ServerPlayer");
        assertEquals(1, count(body, "new S2COpenTestDummyScreen(this.getId())"),
                "The packet must carry this TestDummy entity ID");
        assertFalse(body.contains("sidedSuccess"));
        assertFalse(body.contains("setScreen"));
        assertFalse(source.contains("net.minecraft.client"));
        assertFalse(source.contains("com.huige233.transcend.client.TestDummyScreen"));
        assertFalse(body.contains("new TestDummyScreen("));

        ClassNode dummy = classNode(classBytes());
        MethodNode interaction = method(dummy, "mobInteract", MOB_INTERACT_DESC);
        List<AbstractInsnNode> code = instructions(interaction);

        int shiftCall = uniqueIndex(code, instruction -> methodCall(instruction,
                PLAYER_OWNER, "isShiftKeyDown", "()Z"), "shift-key test");
        int handField = uniqueIndex(code, instruction -> fieldAccess(instruction,
                Opcodes.GETSTATIC, HAND_OWNER, "MAIN_HAND",
                "Lnet/minecraft/world/InteractionHand;"), "main-hand test");
        int levelCall = uniqueIndex(code, instruction -> methodCall(instruction,
                PLAYER_OWNER, "level", "()Lnet/minecraft/world/level/Level;"),
                "logical-level lookup");
        int clientSide = uniqueIndex(code, instruction -> fieldAccess(instruction,
                Opcodes.GETFIELD, LEVEL_OWNER, "isClientSide", "Z"),
                "logical-side test");
        int serverPlayerTest = uniqueIndex(code, instruction -> typeInstruction(instruction,
                Opcodes.INSTANCEOF, SERVER_PLAYER_OWNER), "ServerPlayer test");
        int authorization = uniqueIndex(code, instruction -> methodCall(instruction,
                DUMMY_OWNER, "canConfigure", "(Lnet/minecraft/server/level/ServerPlayer;)Z"),
                "server authorization");
        int send = uniqueIndex(code, instruction -> methodCall(instruction,
                SIMPLE_CHANNEL_OWNER, "send", '(' + PACKET_TARGET_DESC
                        + "Ljava/lang/Object;)V"), "player-targeted send");

        assertEquals(shiftCall + 1,
                conditionalBetween(code, shiftCall, handField, "shift branch"));
        int shiftBranch = shiftCall + 1;
        int handBranch = conditionalBetween(code, handField, levelCall, "hand branch");
        assertEquals(clientSide, levelCall + 1,
                "Logical-side field must come directly from player.level()");
        int clientBranch = conditionalBetween(code, clientSide, serverPlayerTest,
                "logical-side branch");
        int serverPlayerBranch = conditionalBetween(code, serverPlayerTest, authorization,
                "ServerPlayer branch");
        int authorizationBranch = conditionalBetween(code, authorization, send,
                "authorization branch");
        assertEquals(Opcodes.IFEQ, code.get(shiftBranch).getOpcode());
        assertEquals(Opcodes.IF_ACMPEQ, code.get(handBranch).getOpcode());
        assertEquals(Opcodes.IFEQ, code.get(clientBranch).getOpcode());
        assertEquals(Opcodes.IFEQ, code.get(serverPlayerBranch).getOpcode());
        assertEquals(Opcodes.IFNE, code.get(authorizationBranch).getOpcode());

        int pass = resultIndex(code, "PASS");
        int success = resultIndex(code, "SUCCESS");
        int fail = resultIndex(code, "FAIL");
        int consume = resultIndex(code, "CONSUME");
        assertImmediateReturn(code, pass, "PASS");
        assertImmediateReturn(code, success, "SUCCESS");
        assertImmediateReturn(code, fail, "FAIL");
        assertImmediateReturn(code, consume, "CONSUME");
        assertEquals(4, code.stream().filter(instruction ->
                instruction.getOpcode() == Opcodes.ARETURN).count(),
                "mobInteract must have exactly four result returns");

        assertExclusiveOutcomes(interaction, code, shiftBranch, pass, handField,
                "Non-shift must return PASS while shift continues to the hand check");
        assertExclusiveOutcomes(interaction, code, handBranch, pass, clientSide,
                "Off-hand must return PASS while main-hand continues to logical-side handling");
        assertExclusiveOutcomes(interaction, code, clientBranch, success, serverPlayerTest,
                "Client shift/main-hand must return SUCCESS without entering server handling");
        assertExclusiveOutcomes(interaction, code, serverPlayerBranch, fail, authorization,
                "A logical-server non-ServerPlayer must return FAIL without authorization");
        assertExclusiveOutcomes(interaction, code, authorizationBranch, fail, send,
                "Failed authorization must return FAIL and only successful authorization may send");
        assertFalse(reachable(interaction, code,
                outcomeWithout(interaction, code, clientBranch, serverPlayerTest), send, -1),
                "The client SUCCESS path must not send");

        int checkcast = uniqueIndex(code, instruction -> typeInstruction(instruction,
                Opcodes.CHECKCAST, SERVER_PLAYER_OWNER), "authorized ServerPlayer cast");
        assertVar(code, checkcast - 1, Opcodes.ALOAD, 1,
                "The cast must use the interacting player");
        VarInsnNode store = assertInstanceOf(VarInsnNode.class, code.get(checkcast + 1));
        assertEquals(Opcodes.ASTORE, store.getOpcode());
        int serverPlayerLocal = store.var;
        assertVar(code, authorization - 2, Opcodes.ALOAD, 0,
                "canConfigure must be invoked on this TestDummy");
        assertVar(code, authorization - 1, Opcodes.ALOAD, serverPlayerLocal,
                "canConfigure must receive the authorized ServerPlayer local");

        int channel = uniqueIndex(code, instruction -> fieldAccess(instruction,
                Opcodes.GETSTATIC, NETWORK_HANDLER_OWNER, "CHANNEL",
                "Lnet/minecraftforge/network/simple/SimpleChannel;"),
                "NetworkHandler channel");
        int playerTarget = uniqueIndex(code, instruction -> fieldAccess(instruction,
                Opcodes.GETSTATIC, PACKET_DISTRIBUTOR_OWNER, "PLAYER",
                "Lnet/minecraftforge/network/PacketDistributor;"),
                "player packet distributor");
        int supplier = uniqueIndex(code, InvokeDynamicInsnNode.class::isInstance,
                "authorized-player supplier");
        int with = uniqueIndex(code, instruction -> methodCall(instruction,
                PACKET_DISTRIBUTOR_OWNER, "with",
                "(Ljava/util/function/Supplier;)" + PACKET_TARGET_DESC),
                "player target construction");
        assertEquals(channel + 1, playerTarget);
        assertVar(code, playerTarget + 1, Opcodes.ALOAD, serverPlayerLocal,
                "The target supplier must capture the authorized ServerPlayer local");
        assertEquals(playerTarget + 2, supplier);
        assertEquals(playerTarget + 3, with);

        InvokeDynamicInsnNode targetSupplier =
                assertInstanceOf(InvokeDynamicInsnNode.class, code.get(supplier));
        assertEquals("get", targetSupplier.name);
        assertEquals("(Lnet/minecraft/server/level/ServerPlayer;)Ljava/util/function/Supplier;",
                targetSupplier.desc);
        assertEquals("java/lang/invoke/LambdaMetafactory", targetSupplier.bsm.getOwner());
        assertEquals("metafactory", targetSupplier.bsm.getName());
        List<Handle> implementationHandles = new ArrayList<>();
        for (Object argument : targetSupplier.bsmArgs) {
            if (argument instanceof Handle handle) {
                implementationHandles.add(handle);
            }
        }
        assertEquals(1, implementationHandles.size(),
                "The target supplier must have one implementation method");
        Handle supplierHandle = implementationHandles.get(0);
        assertEquals(Opcodes.H_INVOKESTATIC, supplierHandle.getTag());
        assertEquals(DUMMY_OWNER, supplierHandle.getOwner());
        assertTrue(supplierHandle.getName().matches("lambda\\$mobInteract\\$\\d+"));
        assertEquals("(Lnet/minecraft/server/level/ServerPlayer;)"
                        + "Lnet/minecraft/server/level/ServerPlayer;",
                supplierHandle.getDesc());

        List<MethodNode> syntheticMethods = dummy.methods.stream()
                .filter(method -> (method.access & Opcodes.ACC_SYNTHETIC) != 0)
                .toList();
        assertEquals(1, syntheticMethods.size(),
                "Only the mobInteract target-supplier lambda may be synthetic");
        MethodNode supplierMethod = syntheticMethods.get(0);
        assertEquals(supplierHandle.getName(), supplierMethod.name);
        assertEquals(supplierHandle.getDesc(), supplierMethod.desc);
        assertTrue((supplierMethod.access & Opcodes.ACC_STATIC) != 0);
        List<AbstractInsnNode> supplierCode = instructions(supplierMethod);
        assertEquals(2, supplierCode.size(),
                "The target supplier must return its sole captured ServerPlayer unchanged");
        assertVar(supplierCode, 0, Opcodes.ALOAD, 0,
                "The supplier must load its sole ServerPlayer parameter");
        assertEquals(Opcodes.ARETURN, supplierCode.get(1).getOpcode());

        int packetNew = uniqueIndex(code, instruction -> typeInstruction(instruction,
                Opcodes.NEW, PACKET_OWNER), "screen-open packet allocation");
        int entityId = uniqueIndex(code, instruction -> methodCall(instruction,
                DUMMY_OWNER, "getId", "()I"), "this entity ID");
        int packetConstructor = uniqueIndex(code, instruction -> methodCall(instruction,
                PACKET_OWNER, "<init>", "(I)V"), "screen-open packet constructor");
        assertEquals(with + 1, packetNew);
        assertEquals(Opcodes.DUP, code.get(packetNew + 1).getOpcode());
        assertVar(code, packetNew + 2, Opcodes.ALOAD, 0,
                "The packet ID must be read from this TestDummy");
        assertEquals(packetNew + 3, entityId);
        assertEquals(packetNew + 4, packetConstructor);
        assertEquals(packetConstructor + 1, send);
        assertEquals(send + 1, consume,
                "The authorized send must return CONSUME immediately");
        assertTrue(dominates(interaction, clientSide, send));
        assertTrue(dominates(interaction, serverPlayerTest, send));
        assertTrue(dominates(interaction, authorization, send),
                "canConfigure(serverPlayer) must dominate the only send");
        assertEquals(1, classCallCount(dummy, SIMPLE_CHANNEL_OWNER, "send"),
                "No other TestDummy path may send a packet");
        assertEquals(1, classFieldCount(dummy, NETWORK_HANDLER_OWNER, "CHANNEL"),
                "No other TestDummy path may access the channel");
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

    private static int resultIndex(List<AbstractInsnNode> code, String name) {
        return uniqueIndex(code, instruction -> fieldAccess(instruction, Opcodes.GETSTATIC,
                RESULT_OWNER, name, "Lnet/minecraft/world/InteractionResult;"),
                "InteractionResult." + name);
    }

    private static void assertImmediateReturn(List<AbstractInsnNode> code, int result,
                                              String name) {
        assertTrue(result + 1 < code.size(), () -> name + " result has no return");
        assertEquals(Opcodes.ARETURN, code.get(result + 1).getOpcode(),
                () -> name + " must be returned immediately");
    }

    private static int conditionalBetween(List<AbstractInsnNode> code, int after, int before,
                                          String description) {
        return uniqueIndex(code.subList(after + 1, before),
                instruction -> instruction instanceof JumpInsnNode
                        && instruction.getOpcode() != Opcodes.GOTO,
                description) + after + 1;
    }

    private static void assertExclusiveOutcomes(MethodNode method,
                                                List<AbstractInsnNode> code,
                                                int branch, int first, int second,
                                                String message) {
        List<Integer> outcomes = successors(method, code, branch);
        assertEquals(2, outcomes.size(), message);
        int firstCount = 0;
        int secondCount = 0;
        for (int outcome : outcomes) {
            boolean reachesFirst = reachable(method, code, outcome, first, -1);
            boolean reachesSecond = reachable(method, code, outcome, second, -1);
            assertTrue(reachesFirst ^ reachesSecond, message);
            if (reachesFirst) {
                firstCount++;
            } else {
                secondCount++;
            }
        }
        assertEquals(1, firstCount, message);
        assertEquals(1, secondCount, message);
    }

    private static int outcomeWithout(MethodNode method, List<AbstractInsnNode> code,
                                      int branch, int excludedTarget) {
        List<Integer> outcomes = successors(method, code, branch).stream()
                .filter(outcome -> !reachable(method, code, outcome, excludedTarget, -1))
                .toList();
        assertEquals(1, outcomes.size(), "Expected exactly one bypass outcome");
        return outcomes.get(0);
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

    private static void assertVar(List<AbstractInsnNode> code, int index, int opcode,
                                  int variable, String message) {
        VarInsnNode instruction = assertInstanceOf(VarInsnNode.class, code.get(index), message);
        assertEquals(opcode, instruction.getOpcode(), message);
        assertEquals(variable, instruction.var, message);
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

    private static boolean typeInstruction(AbstractInsnNode instruction, int opcode,
                                           String descriptor) {
        return instruction instanceof TypeInsnNode type
                && type.getOpcode() == opcode
                && type.desc.equals(descriptor);
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

    private static int classFieldCount(ClassNode owner, String fieldOwner, String name) {
        int count = 0;
        for (MethodNode method : owner.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof FieldInsnNode field
                        && field.owner.equals(fieldOwner) && field.name.equals(name)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static String mobInteractBody(String source) {
        String signature = "public InteractionResult mobInteract(Player player, InteractionHand hand)";
        int method = source.indexOf(signature);
        assertTrue(method >= 0, "Missing exact mobInteract source signature");
        assertEquals(-1, source.indexOf(signature, method + signature.length()),
                "mobInteract source signature must occur exactly once");
        int openingBrace = source.indexOf('{', method + signature.length());
        int closingBrace = matchingBrace(source, openingBrace);
        return source.substring(openingBrace + 1, closingBrace - 1);
    }

    private static int matchingBrace(String source, int openingBrace) {
        assertTrue(openingBrace >= 0, "Missing mobInteract opening brace");
        int depth = 0;
        for (int index = openingBrace; index < source.length(); index++) {
            char character = source.charAt(index);
            if (character == '{') {
                depth++;
            } else if (character == '}' && --depth == 0) {
                return index + 1;
            }
        }
        throw new AssertionError("Unterminated mobInteract source body");
    }

    private static int count(String source, String token) {
        int result = 0;
        int from = 0;
        while ((from = source.indexOf(token, from)) >= 0) {
            result++;
            from += token.length();
        }
        return result;
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
