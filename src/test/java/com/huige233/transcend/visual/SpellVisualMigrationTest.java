package com.huige233.transcend.visual;

import com.huige233.transcend.magic.MagicCircleGeometry;
import com.huige233.transcend.network.S2CShaderEffectPack;
import com.huige233.transcend.network.S2CParticleBatchPack;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.joml.Vector3f;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SpellVisualMigrationTest {
    private static final Path MAIN = Path.of("src/main/java/com/huige233/transcend");
    private static final Pattern CLIENT_VISUAL_LINK = Pattern.compile(
            "com\\.huige233\\.transcend\\.client\\.(?:renderer\\.ShaderSpellRenderer|magic(?:\\.[A-Za-z_$][\\w$]*)?)|\\bShaderSpellRenderer\\b");
    private static final Pattern COMMENTS_AND_STRINGS = Pattern.compile(
            "(?s)/\\*.*?\\*/|//[^\\r\\n]*|\"(?:\\\\.|[^\"\\\\])*\"");
    private static final List<String> COMMON_CLASSES = List.of(
            "spell/SpellProjectile", "spell/ElementReaction", "items/TranscendWand", "spell/SpellDamageService");

    @Test
    void unchangedPacketFactoriesCharacterizeSpellReactionAndWandPayloadOrder() {
        List<S2CShaderEffectPack> packets = List.of(
                ServerVisualBroadcaster.shockwavePacket(new Vec3(4, 63.08, -2),
                        2.9F, 1.0F, 0.3F, 0.1F, 18),
                ServerVisualBroadcaster.beamPacket(new Vec3(4, 64.8, -2), new Vec3(4, 63.1, -2),
                        1.0F, 1.0F, 0.55F, 12, "beam"),
                ServerVisualBroadcaster.shockwavePacket(new Vec3(8, 65, 8),
                        2.2F, 0.25F, 0.75F, 1.0F, 14),
                ServerVisualBroadcaster.shieldRipplePacket(new Vec3(8, 65, 8),
                        1.5F, 0.2F, 0.6F, 0.8F, 12),
                ServerVisualBroadcaster.beamPacket(new Vec3(1, 66, 1), new Vec3(2, 66, 1),
                        0.9F, 0.2F, 0.1F, 6, "slash"),
                ServerVisualBroadcaster.beamPacket(new Vec3(1, 66, 1), new Vec3(31, 66, 1),
                        0.9F, 0.2F, 0.1F, 8, "beam"),
                ServerVisualBroadcaster.beamPacket(new Vec3(1, 64, 1), new Vec3(1, 64, 1),
                        0.9F, 0.2F, 0.1F, 10, "nova"),
                ServerVisualBroadcaster.circlePacket(new Vec3(3, 64.06, 3),
                        2.5F, 0.9F, 0.2F, 0.1F, 15, 24, "hexagram"),
                ServerVisualBroadcaster.shieldRipplePacket(new Vec3(3, 64.45, 3),
                        2.0F, 0.9F, 0.2F, 0.1F, 15),
                ServerVisualBroadcaster.shockwavePacket(new Vec3(3, 64.1, 3),
                        3.125F, 0.9F, 0.2F, 0.1F, 21));

        assertEquals("[SHOCKWAVE@(4.00,63.08,-2.00)->2.90#18/0/, "
                        + "BEAM@(4.00,64.80,-2.00)->(4.00,63.10,-2.00)#12/0/beam, "
                        + "SHOCKWAVE@(8.00,65.00,8.00)->2.20#14/0/, "
                        + "SHIELD_RIPPLE@(8.00,65.00,8.00)->1.50#12/0/, "
                        + "BEAM@(1.00,66.00,1.00)->(2.00,66.00,1.00)#6/0/slash, "
                        + "BEAM@(1.00,66.00,1.00)->(31.00,66.00,1.00)#8/0/beam, "
                        + "BEAM@(1.00,64.00,1.00)->(1.00,64.00,1.00)#10/0/nova, "
                        + "CIRCLE@(3.00,64.06,3.00)->2.50#15/24/hexagram, "
                        + "SHIELD_RIPPLE@(3.00,64.45,3.00)->2.00#15/0/, "
                        + "SHOCKWAVE@(3.00,64.10,3.00)->3.13#21/0/]",
                packets.stream().map(SpellVisualMigrationTest::summary).toList().toString());
    }

    @Test
    void serverGeometryPreservesLegacyCircleAndLineCoordinates() {
        Vector3f axis = new Vector3f(0.25F, 1.0F, -0.5F);
        assertEntriesEqual(
                MagicCircleGeometry.buildCircle(1.25, -2.5, 3.75, 4.5, 17, 0.375, axis),
                ServerVisualGeometry.circle(1.25, -2.5, 3.75, 4.5, 17, 0.375, axis));
        assertEntriesEqual(
                MagicCircleGeometry.buildLine(-1.0, 2.0, -3.0, 4.0, -5.0, 6.0, 13),
                ServerVisualGeometry.line(-1.0, 2.0, -3.0, 4.0, -5.0, 6.0, 13));
    }

    @Test
    void scopedCommonSourcesHaveNoClientVisualLinkage() throws IOException {
        for (String className : COMMON_CLASSES) {
            Path source = MAIN.resolve(className + ".java");
            String code = COMMENTS_AND_STRINGS.matcher(Files.readString(source)).replaceAll(" ");
            assertFalse(CLIENT_VISUAL_LINK.matcher(code).find(), source.toString());
        }
    }

    @Test
    void compiledCommonClassesHaveNoClientVisualConstantPoolLinkage() throws IOException {
        ClassLoader loader = SpellVisualMigrationTest.class.getClassLoader();
        for (String className : COMMON_CLASSES) {
            String classResource = "com/huige233/transcend/" + className + ".class";
            byte[] classBytes;
            try (var input = loader.getResourceAsStream(classResource)) {
                if (input == null) throw new IOException("Missing compiled class " + classResource);
                classBytes = input.readAllBytes();
            }
            String constantPool = new String(classBytes, StandardCharsets.ISO_8859_1);
            assertFalse(constantPool.contains("com/huige233/transcend/client/renderer/ShaderSpellRenderer"),
                    classResource);
            assertFalse(constantPool.contains("com/huige233/transcend/client/magic/"), classResource);
        }
    }

    private static String summary(S2CShaderEffectPack packet) {
        ByteBuf backing = Unpooled.buffer();
        try {
            FriendlyByteBuf buffer = new FriendlyByteBuf(backing);
            packet.write(buffer);
            S2CShaderEffectPack decoded = new S2CShaderEffectPack(buffer);
            String type = field(decoded, "type").toString();
            Vec3 center = (Vec3) field(decoded, "center");
            Vec3 targetOrSize = (Vec3) field(decoded, "toOrSize");
            String destination = type.equals("BEAM")
                    ? String.format(Locale.ROOT, "(%.2f,%.2f,%.2f)", targetOrSize.x, targetOrSize.y, targetOrSize.z)
                    : String.format(Locale.ROOT, "%.2f", targetOrSize.x);
            return String.format(Locale.ROOT, "%s@(%.2f,%.2f,%.2f)->%s#%d/%d/%s",
                    type, center.x, center.y, center.z, destination,
                    (int) field(decoded, "lifetime"), (int) field(decoded, "segments"), field(decoded, "pattern"));
        } finally {
            backing.release();
        }
    }

    private static Object field(S2CShaderEffectPack packet, String name) {
        try {
            Field field = S2CShaderEffectPack.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(packet);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    private static void assertEntriesEqual(List<S2CParticleBatchPack.ParticleEntry> expected,
                                           List<S2CParticleBatchPack.ParticleEntry> actual) {
        assertEquals(expected.size(), actual.size());
        for (int index = 0; index < expected.size(); index++) {
            S2CParticleBatchPack.ParticleEntry left = expected.get(index);
            S2CParticleBatchPack.ParticleEntry right = actual.get(index);
            assertEquals(left.x, right.x, 1.0E-9, "x at " + index);
            assertEquals(left.y, right.y, 1.0E-9, "y at " + index);
            assertEquals(left.z, right.z, 1.0E-9, "z at " + index);
        }
    }
}
