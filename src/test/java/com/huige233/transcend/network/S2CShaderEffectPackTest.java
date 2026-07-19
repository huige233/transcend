package com.huige233.transcend.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class S2CShaderEffectPackTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void everyExistingFactoryRoundTripsAllRendererParameters() throws ReflectiveOperationException {
        List<ExpectedPacket> cases = List.of(
                new ExpectedPacket(S2CShaderEffectPack.EffectType.CIRCLE,
                        new Vec3(1.25, -2.5, 3.75), new Vec3(4.5, 0, 0),
                        0.1F, 0.2F, 0.3F, 40, 32, "pentagram", null),
                new ExpectedPacket(S2CShaderEffectPack.EffectType.SHOCKWAVE,
                        new Vec3(-4, 5.5, 6), new Vec3(7.25, 0, 0),
                        0.4F, 0.5F, 0.6F, 28, 0, "", null),
                new ExpectedPacket(S2CShaderEffectPack.EffectType.SHIELD_RIPPLE,
                        new Vec3(8, 9, -10), new Vec3(2.75, 0, 0),
                        0.7F, 0.8F, 0.9F, 16, 0, "", null),
                new ExpectedPacket(S2CShaderEffectPack.EffectType.BEAM,
                        new Vec3(11, 12.5, 13), new Vec3(-14, 15, 16.5),
                        0.15F, 0.35F, 0.55F, 22, 0, "slash", new Vec3(-14, 15, 16.5))
        );
        List<S2CShaderEffectPack> packets = List.of(
                S2CShaderEffectPack.circle(cases.get(0).center(), 4.5F, 0.1F, 0.2F, 0.3F, 40, 32, "pentagram"),
                S2CShaderEffectPack.shockwave(cases.get(1).center(), 7.25F, 0.4F, 0.5F, 0.6F, 28),
                S2CShaderEffectPack.shieldRipple(cases.get(2).center(), 2.75F, 0.7F, 0.8F, 0.9F, 16),
                S2CShaderEffectPack.beam(cases.get(3).center(), cases.get(3).toOrSize(),
                        0.15F, 0.35F, 0.55F, 22, "slash")
        );

        for (int i = 0; i < packets.size(); i++) {
            assertPacket(cases.get(i), decode(packets.get(i)));
        }
    }

    @Test
    void nullableFactoryInputsRetainExistingFallbackSemantics() throws ReflectiveOperationException {
        S2CShaderEffectPack circle = decode(S2CShaderEffectPack.circle(
                Vec3.ZERO, 1, 1, 1, 1, 1, 8, null));
        S2CShaderEffectPack beam = decode(S2CShaderEffectPack.beam(
                Vec3.ZERO, new Vec3(1, 2, 3), 1, 1, 1, 1, null));

        assertEquals("", field(circle, "pattern"));
        assertNull(logicalTarget(circle));
        assertEquals("beam", field(beam, "pattern"));
        assertEquals(new Vec3(1, 2, 3), logicalTarget(beam));
    }

    @Test
    void representativeDecodedPayloadSnapshotIsStable() throws ReflectiveOperationException {
        List<S2CShaderEffectPack> decoded = List.of(
                decode(S2CShaderEffectPack.circle(new Vec3(1.25, -2.5, 3.75),
                        4.5F, 0.1F, 0.2F, 0.3F, 40, 32, "pentagram")),
                decode(S2CShaderEffectPack.shockwave(new Vec3(-4, 5.5, 6),
                        7.25F, 0.4F, 0.5F, 0.6F, 28)),
                decode(S2CShaderEffectPack.shieldRipple(new Vec3(8, 9, -10),
                        2.75F, 0.7F, 0.8F, 0.9F, 16)),
                decode(S2CShaderEffectPack.beam(new Vec3(11, 12.5, 13), new Vec3(-14, 15, 16.5),
                        0.15F, 0.35F, 0.55F, 22, "slash"))
        );
        String snapshot = decoded.stream().map(S2CShaderEffectPackTest::toJson).toList().toString();
        String expected = "[{\"type\":\"CIRCLE\",\"position\":[1.25,-2.50,3.75],\"target\":null,"
                + "\"size\":[4.50,0.00,0.00],\"color\":[0.10,0.20,0.30],\"lifetime\":40,\"segments\":32,\"pattern\":\"pentagram\"}, "
                + "{\"type\":\"SHOCKWAVE\",\"position\":[-4.00,5.50,6.00],\"target\":null,"
                + "\"size\":[7.25,0.00,0.00],\"color\":[0.40,0.50,0.60],\"lifetime\":28,\"segments\":0,\"pattern\":\"\"}, "
                + "{\"type\":\"SHIELD_RIPPLE\",\"position\":[8.00,9.00,-10.00],\"target\":null,"
                + "\"size\":[2.75,0.00,0.00],\"color\":[0.70,0.80,0.90],\"lifetime\":16,\"segments\":0,\"pattern\":\"\"}, "
                + "{\"type\":\"BEAM\",\"position\":[11.00,12.50,13.00],\"target\":[-14.00,15.00,16.50],"
                + "\"size\":null,\"color\":[0.15,0.35,0.55],\"lifetime\":22,\"segments\":0,\"pattern\":\"slash\"}]";
        assertEquals(expected, snapshot);
        System.out.println("TASK5_PACKET_SNAPSHOT=" + snapshot);
    }

    private static S2CShaderEffectPack decode(S2CShaderEffectPack original) {
        ByteBuf backing = Unpooled.buffer();
        try {
            FriendlyByteBuf buffer = new FriendlyByteBuf(backing);
            original.write(buffer);
            return new S2CShaderEffectPack(buffer);
        } finally {
            backing.release();
        }
    }

    private static void assertPacket(ExpectedPacket expected, S2CShaderEffectPack actual)
            throws ReflectiveOperationException {
        assertEquals(expected.type(), field(actual, "type"));
        assertEquals(expected.center(), field(actual, "center"));
        assertEquals(expected.toOrSize(), field(actual, "toOrSize"));
        assertEquals(expected.r(), (float) field(actual, "r"), EPSILON);
        assertEquals(expected.g(), (float) field(actual, "g"), EPSILON);
        assertEquals(expected.b(), (float) field(actual, "b"), EPSILON);
        assertEquals(expected.lifetime(), field(actual, "lifetime"));
        assertEquals(expected.segments(), field(actual, "segments"));
        assertEquals(expected.pattern(), field(actual, "pattern"));
        assertEquals(expected.logicalTarget(), logicalTarget(actual));
    }

    private static Vec3 logicalTarget(S2CShaderEffectPack packet) throws ReflectiveOperationException {
        return field(packet, "type") == S2CShaderEffectPack.EffectType.BEAM
                ? (Vec3) field(packet, "toOrSize")
                : null;
    }

    private static Object field(S2CShaderEffectPack packet, String name) throws ReflectiveOperationException {
        Field field = S2CShaderEffectPack.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(packet);
    }

    private static String toJson(S2CShaderEffectPack packet) {
        try {
            S2CShaderEffectPack.EffectType type = (S2CShaderEffectPack.EffectType) field(packet, "type");
            Vec3 center = (Vec3) field(packet, "center");
            Vec3 toOrSize = (Vec3) field(packet, "toOrSize");
            Vec3 target = logicalTarget(packet);
            String targetJson = target == null ? "null" : vector(target);
            String sizeJson = target == null ? vector(toOrSize) : "null";
            return String.format(Locale.ROOT,
                    "{\"type\":\"%s\",\"position\":%s,\"target\":%s,\"size\":%s,"
                            + "\"color\":[%.2f,%.2f,%.2f],\"lifetime\":%d,\"segments\":%d,\"pattern\":\"%s\"}",
                    type, vector(center), targetJson, sizeJson,
                    (float) field(packet, "r"), (float) field(packet, "g"), (float) field(packet, "b"),
                    (int) field(packet, "lifetime"), (int) field(packet, "segments"), field(packet, "pattern"));
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    private static String vector(Vec3 vector) {
        return String.format(Locale.ROOT, "[%.2f,%.2f,%.2f]", vector.x, vector.y, vector.z);
    }

    private record ExpectedPacket(S2CShaderEffectPack.EffectType type, Vec3 center, Vec3 toOrSize,
                                  float r, float g, float b, int lifetime, int segments,
                                  String pattern, Vec3 logicalTarget) {}
}
