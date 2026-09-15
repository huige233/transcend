package com.huige233.transcend.visual;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import com.huige233.transcend.network.S2CShaderEffectPack;
import com.huige233.transcend.network.S2CGlitterBatchPack;
import com.huige233.transcend.network.S2CParticleBatchPack;
import com.huige233.transcend.network.S2CRuneBatchPack;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证视觉广播参数、维度与距离筛选、无效输入拒绝及多阶段效果包的发送顺序和公共侧隔离。 */
class ServerVisualBroadcasterTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void shaderFactoriesPreserveEveryRendererArgumentAndOrder() throws ReflectiveOperationException {
        Vec3 circleCenter = new Vec3(1.25, 2.5, -3.75);
        Vec3 shockwaveCenter = new Vec3(4.0, -5.5, 6.25);
        Vec3 rippleCenter = new Vec3(-7.0, 8.5, 9.0);
        Vec3 beamFrom = new Vec3(10.0, 11.25, 12.5);
        Vec3 beamTo = new Vec3(-13.0, 14.75, 15.0);

        S2CShaderEffectPack circle = decode(ServerVisualBroadcaster.circlePacket(
                circleCenter, 4.5F, 0.1F, 0.2F, 0.3F, 40, 32, "pentagram"));
        S2CShaderEffectPack shockwave = decode(ServerVisualBroadcaster.shockwavePacket(
                shockwaveCenter, 7.25F, 0.4F, 0.5F, 0.6F, 28));
        S2CShaderEffectPack ripple = decode(ServerVisualBroadcaster.shieldRipplePacket(
                rippleCenter, 2.75F, 0.7F, 0.8F, 0.9F, 16));
        S2CShaderEffectPack beam = decode(ServerVisualBroadcaster.beamPacket(
                beamFrom, beamTo, 0.15F, 0.35F, 0.55F, 22, "slash"));

        assertPacket(circle, "CIRCLE", circleCenter, new Vec3(4.5, 0, 0),
                0.1F, 0.2F, 0.3F, 40, 32, "pentagram");
        assertPacket(shockwave, "SHOCKWAVE", shockwaveCenter, new Vec3(7.25, 0, 0),
                0.4F, 0.5F, 0.6F, 28, 0, "");
        assertPacket(ripple, "SHIELD_RIPPLE", rippleCenter, new Vec3(2.75, 0, 0),
                0.7F, 0.8F, 0.9F, 16, 0, "");
        assertPacket(beam, "BEAM", beamFrom, beamTo,
                0.15F, 0.35F, 0.55F, 22, 0, "slash");
    }

    @Test
    void nearRecipientBoundaryPinsRadiusDimensionAndDistributorWiring() throws Exception {
        Vec3 center = new Vec3(12.5, -3.25, 99.75);
        String dimension = "test:alpha";
        String otherDimension = "test:beta";

        assertTrue(ServerVisualBroadcaster.isRecipient(dimension, center, dimension,
                center.add(ServerVisualBroadcaster.SHADER_BROADCAST_RADIUS - 0.001, 0, 0),
                ServerVisualBroadcaster.SHADER_BROADCAST_RADIUS));
        assertFalse(ServerVisualBroadcaster.isRecipient(dimension, center, dimension,
                center.add(ServerVisualBroadcaster.SHADER_BROADCAST_RADIUS, 0, 0),
                ServerVisualBroadcaster.SHADER_BROADCAST_RADIUS));
        assertFalse(ServerVisualBroadcaster.isRecipient(dimension, center, otherDimension, center,
                ServerVisualBroadcaster.SHADER_BROADCAST_RADIUS));

        assertTrue(ServerVisualBroadcaster.isRecipient(dimension, center, dimension,
                center.add(63.999, 0, 0), ServerVisualBroadcaster.PARTICLE_BROADCAST_RADIUS));
        assertFalse(ServerVisualBroadcaster.isRecipient(dimension, center, dimension,
                center.add(64.000, 0, 0), ServerVisualBroadcaster.PARTICLE_BROADCAST_RADIUS));
        assertFalse(ServerVisualBroadcaster.isRecipient(dimension, center, otherDimension, center,
                ServerVisualBroadcaster.PARTICLE_BROADCAST_RADIUS));

        String source = Files.readString(Path.of(
                "src/main/java/com/huige233/transcend/visual/ServerVisualBroadcaster.java"));
        assertTrue(source.contains("new PacketDistributor.TargetPoint(center.x, center.y, center.z, radius, dimension)"));
        assertTrue(source.contains("PacketDistributor.NEAR.with(() -> point)"));
    }

    @Test
    void malformedVisualArgumentsFailBeforePacketDistribution() {
        assertThrows(NullPointerException.class, () -> ServerVisualBroadcaster.circlePacket(
                null, 1, 1, 1, 1, 10, 16, "hexagram"));
        assertThrows(IllegalArgumentException.class, () -> ServerVisualBroadcaster.circlePacket(
                Vec3.ZERO, Float.NaN, 1, 1, 1, 10, 16, "hexagram"));
        assertThrows(IllegalArgumentException.class, () -> ServerVisualBroadcaster.circlePacket(
                Vec3.ZERO, 1, Float.POSITIVE_INFINITY, 1, 1, 10, 16, "hexagram"));
        assertThrows(IllegalArgumentException.class, () -> ServerVisualBroadcaster.circlePacket(
                Vec3.ZERO, 1, 1, 1, 1, 0, 16, "hexagram"));
        assertThrows(IllegalArgumentException.class, () -> ServerVisualBroadcaster.circlePacket(
                Vec3.ZERO, 1, 1, 1, 1, 10, -1, "hexagram"));

        assertThrows(IllegalArgumentException.class, () -> ServerVisualBroadcaster.particleBatch(
                null, Vec3.ZERO, List.<S2CParticleBatchPack.ParticleEntry>of(),
                Float.NaN, 1, 1, 1, 10, true));
        assertThrows(IllegalArgumentException.class, () -> ServerVisualBroadcaster.runeBatch(
                null, Vec3.ZERO, List.<S2CRuneBatchPack.RuneEntry>of(),
                1, 1, 1, Float.NaN, 10, true));
        assertThrows(IllegalArgumentException.class, () -> ServerVisualBroadcaster.glitterBatch(
                null, Vec3.ZERO, List.<S2CGlitterBatchPack.GlitterEntry>of(),
                1, 1, 1, 1, 0, true));
    }

    @Test
    void commonBroadcasterHasNoClientImportsAndCoversExistingVisualPackets() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/huige233/transcend/visual/ServerVisualBroadcaster.java"));

        assertFalse(source.contains("net.minecraft.client"));
        assertFalse(source.contains("com.huige233.transcend.client"));
        assertTrue(source.contains("public static void circle("));
        assertTrue(source.contains("public static void shockwave("));
        assertTrue(source.contains("public static void shieldRipple("));
        assertTrue(source.contains("public static void beam("));
        assertTrue(source.contains("public static void particleBatch("));
        assertTrue(source.contains("public static void vanillaParticleBatch("));
        assertTrue(source.contains("public static void runeBatch("));
        assertTrue(source.contains("public static void glitterBatch("));
    }

    @Test
    void representativeSpellReactionAndBossSequencesSerializeInOrder() {
        List<S2CShaderEffectPack> spell = List.of(
                ServerVisualBroadcaster.shieldRipplePacket(new Vec3(1, 64, 1),
                        0.9F, 1.0F, 0.3F, 0.1F, 12),
                ServerVisualBroadcaster.beamPacket(new Vec3(0, 64, 1), new Vec3(1, 64, 1),
                        1.0F, 0.3F, 0.1F, 10, "beam"),
                ServerVisualBroadcaster.shockwavePacket(new Vec3(4, 63.08, -2),
                        2.9F, 1.0F, 0.3F, 0.1F, 18));
        List<S2CShaderEffectPack> reaction = List.of(
                ServerVisualBroadcaster.shockwavePacket(new Vec3(8, 65, 8),
                        2.2F, 0.25F, 0.75F, 1.0F, 14),
                ServerVisualBroadcaster.shieldRipplePacket(new Vec3(8, 65, 8),
                        1.5F, 0.2F, 0.6F, 0.8F, 12));
        List<S2CShaderEffectPack> boss = List.of(
                ServerVisualBroadcaster.circlePacket(new Vec3(-5, 70.1, 3),
                        6.0F, 0.15F, 0.0F, 0.2F, 80, 32, "pentagram"),
                ServerVisualBroadcaster.shockwavePacket(new Vec3(-5, 70.2, 3),
                        8.5F, 1.0F, 0.9F, 0.3F, 28),
                ServerVisualBroadcaster.beamPacket(new Vec3(-5, 85, 3), new Vec3(-5, 70.1, 3),
                        1.0F, 0.35F, 0.08F, 40, "meteor"));

        String snapshot = "spell=" + sequence(spell) + "\nreaction=" + sequence(reaction)
                + "\nboss=" + sequence(boss);
        assertEquals("spell=[SHIELD_RIPPLE@(1.00,64.00,1.00)->0.90#12/0/, "
                        + "BEAM@(0.00,64.00,1.00)->(1.00,64.00,1.00)#10/0/beam, "
                        + "SHOCKWAVE@(4.00,63.08,-2.00)->2.90#18/0/]\n"
                        + "reaction=[SHOCKWAVE@(8.00,65.00,8.00)->2.20#14/0/, "
                        + "SHIELD_RIPPLE@(8.00,65.00,8.00)->1.50#12/0/]\n"
                        + "boss=[CIRCLE@(-5.00,70.10,3.00)->6.00#80/32/pentagram, "
                        + "SHOCKWAVE@(-5.00,70.20,3.00)->8.50#28/0/, "
                        + "BEAM@(-5.00,85.00,3.00)->(-5.00,70.10,3.00)#40/0/meteor]",
                snapshot);
        System.out.println("TASK10_SEQUENCE_SNAPSHOT=" + snapshot.replace('\n', '|'));
    }

    @Test
    void repeatedAvatarMeteorScheduleIsRepresentableAsOrderedStatelessPackets() {
        List<String> scheduled = List.of(
                "age=0:" + packetSummary(ServerVisualBroadcaster.circlePacket(
                         new Vec3(2, 70, 2), 3.5F, 1, 0.35F, 0.12F, 18, 40, "pentagram")),
                "age=0:" + packetSummary(ServerVisualBroadcaster.shieldRipplePacket(
                         new Vec3(2, 70, 2), 2.975F, 1, 0.65F, 0.2F, 16)),
                "age=0:" + packetSummary(ServerVisualBroadcaster.circlePacket(
                        new Vec3(2, 70.05, 2), 1.925F, 1, 0.8F, 0.3F, 14, 28, "hexagram")),
                "age=0:" + packetSummary(ServerVisualBroadcaster.shockwavePacket(
                        new Vec3(2, 70, 2), 2.45F, 1, 0.4F, 0.1F, 20)),
                "age=1:" + packetSummary(ServerVisualBroadcaster.circlePacket(
                        new Vec3(2, 70, 2), 3.5F, 1, 0.35F, 0.12F, 18, 40, "pentagram")),
                "age=4:" + packetSummary(ServerVisualBroadcaster.circlePacket(
                        new Vec3(2, 70, 2), 3.5F, 1, 0.35F, 0.12F, 18, 40, "pentagram")),
                "age=4:" + packetSummary(ServerVisualBroadcaster.shieldRipplePacket(
                        new Vec3(2, 70, 2), 2.975F, 1, 0.65F, 0.2F, 16)),
                "age=6:" + packetSummary(ServerVisualBroadcaster.circlePacket(
                        new Vec3(2, 70, 2), 3.5F, 1, 0.35F, 0.12F, 18, 40, "pentagram")),
                "age=6:" + packetSummary(ServerVisualBroadcaster.circlePacket(
                        new Vec3(2, 70.05, 2), 1.925F, 1, 0.8F, 0.3F, 14, 28, "hexagram")),
                "age=12:" + packetSummary(ServerVisualBroadcaster.circlePacket(
                        new Vec3(2, 70, 2), 3.5F, 1, 0.35F, 0.12F, 18, 40, "pentagram")),
                "age=12:" + packetSummary(ServerVisualBroadcaster.shieldRipplePacket(
                        new Vec3(2, 70, 2), 2.975F, 1, 0.65F, 0.2F, 16)),
                "age=12:" + packetSummary(ServerVisualBroadcaster.circlePacket(
                        new Vec3(2, 70.05, 2), 1.925F, 1, 0.8F, 0.3F, 14, 28, "hexagram")),
                "age=12:" + packetSummary(ServerVisualBroadcaster.shockwavePacket(
                        new Vec3(2, 70, 2), 2.45F, 1, 0.4F, 0.1F, 20))
        );

        assertEquals(List.of(
                "age=0:CIRCLE@(2.00,70.00,2.00)->3.50#18/40/pentagram",
                "age=0:SHIELD_RIPPLE@(2.00,70.00,2.00)->2.97#16/0/",
                "age=0:CIRCLE@(2.00,70.05,2.00)->1.92#14/28/hexagram",
                "age=0:SHOCKWAVE@(2.00,70.00,2.00)->2.45#20/0/",
                "age=1:CIRCLE@(2.00,70.00,2.00)->3.50#18/40/pentagram",
                "age=4:CIRCLE@(2.00,70.00,2.00)->3.50#18/40/pentagram",
                "age=4:SHIELD_RIPPLE@(2.00,70.00,2.00)->2.97#16/0/",
                "age=6:CIRCLE@(2.00,70.00,2.00)->3.50#18/40/pentagram",
                "age=6:CIRCLE@(2.00,70.05,2.00)->1.92#14/28/hexagram",
                "age=12:CIRCLE@(2.00,70.00,2.00)->3.50#18/40/pentagram",
                "age=12:SHIELD_RIPPLE@(2.00,70.00,2.00)->2.97#16/0/",
                "age=12:CIRCLE@(2.00,70.05,2.00)->1.92#14/28/hexagram",
                "age=12:SHOCKWAVE@(2.00,70.00,2.00)->2.45#20/0/"), scheduled);
    }

    @Test
    void taskTwelveBossPillarSequenceRoundTripsInEmissionOrder() {
        Vec3 boss = new Vec3(4, 70, -3);
        Vec3 oldTeleport = new Vec3(4, 70.1, -3);
        Vec3 newTeleport = new Vec3(9, 71.1, 2);
        Vec3 pillar = new Vec3(-6, 65.5, 8);
        List<S2CShaderEffectPack> packets = List.of(
                ServerVisualBroadcaster.circlePacket(boss.add(0, 0.5, 0), 2.0F,
                        1.0F, 0.3F, 0.2F, 18, 30, "hexagram"),
                ServerVisualBroadcaster.circlePacket(boss.add(0, 0.8, 0), 4.0F,
                        1.0F, 0.3F, 0.2F, 22, 30, "pentagram"),
                ServerVisualBroadcaster.circlePacket(boss.add(0, 1.1, 0), 6.0F,
                        1.0F, 0.3F, 0.2F, 26, 30, "hexagram"),
                ServerVisualBroadcaster.shockwavePacket(boss.add(0, 0.1, 0), 16.0F,
                        0.7F, 0.2F, 0.1F, 40),
                ServerVisualBroadcaster.beamPacket(boss.add(0, 4.5, 0), boss.add(0, 0.5, 0),
                        0.7F, 0.2F, 0.1F, 24, "beam"),
                ServerVisualBroadcaster.circlePacket(oldTeleport, 2.0F,
                        0.15F, 0.0F, 0.2F, 60, 22, "pentagram"),
                ServerVisualBroadcaster.circlePacket(newTeleport, 2.4F,
                        0.25F, 0.03F, 0.35F, 80, 24, "hexagram"),
                ServerVisualBroadcaster.shieldRipplePacket(newTeleport.add(0, 0.9, 0), 2.2F,
                        0.25F, 0.03F, 0.35F, 18),
                ServerVisualBroadcaster.shockwavePacket(newTeleport.add(0, 0.1, 0), 2.8F,
                        0.25F, 0.03F, 0.35F, 16),
                ServerVisualBroadcaster.circlePacket(boss.add(0, 0.1, 0), 8.0F,
                        0.8F, 0.4F, 0.1F, 40, 32, "hexagram"),
                ServerVisualBroadcaster.shockwavePacket(boss.add(0, 0.2, 0), 9.2F,
                        0.8F, 0.4F, 0.1F, 20),
                ServerVisualBroadcaster.shockwavePacket(pillar, 2.8F,
                        0.2F, 0.6F, 1.0F, 18),
                ServerVisualBroadcaster.circlePacket(pillar, 2.1F,
                        0.2F, 0.6F, 1.0F, 14, 24, "hexagram"),
                ServerVisualBroadcaster.beamPacket(pillar.add(0, 2.6, 0), pillar,
                        0.2F, 0.6F, 1.0F, 12, "beam")
        );

        String snapshot = sequence(packets);
        assertTrue(snapshot.startsWith("[CIRCLE@(4.00,70.50,-3.00)->2.00#18/30/hexagram"));
        assertTrue(snapshot.contains("CIRCLE@(4.00,70.10,-3.00)->2.00#60/22/pentagram, "
                + "CIRCLE@(9.00,71.10,2.00)->2.40#80/24/hexagram, "
                + "SHIELD_RIPPLE@(9.00,72.00,2.00)->2.20#18/0/, "
                + "SHOCKWAVE@(9.00,71.20,2.00)->2.80#16/0/"));
        assertTrue(snapshot.endsWith("SHOCKWAVE@(-6.00,65.50,8.00)->2.80#18/0/, "
                + "CIRCLE@(-6.00,65.50,8.00)->2.10#14/24/hexagram, "
                + "BEAM@(-6.00,68.10,8.00)->(-6.00,65.50,8.00)#12/0/beam]"));
        System.out.println("TASK12_PACKET_SEQUENCE=" + snapshot);
    }

    private static S2CShaderEffectPack decode(S2CShaderEffectPack packet) {
        ByteBuf backing = Unpooled.buffer();
        try {
            FriendlyByteBuf buffer = new FriendlyByteBuf(backing);
            packet.write(buffer);
            return new S2CShaderEffectPack(buffer);
        } finally {
            backing.release();
        }
    }

    private static void assertPacket(S2CShaderEffectPack packet, String type, Vec3 center, Vec3 toOrSize,
                                     float r, float g, float b, int lifetime, int segments, String pattern)
            throws ReflectiveOperationException {
        assertEquals(type, field(packet, "type").toString());
        assertEquals(center, field(packet, "center"));
        assertEquals(toOrSize, field(packet, "toOrSize"));
        assertEquals(r, (float) field(packet, "r"), EPSILON);
        assertEquals(g, (float) field(packet, "g"), EPSILON);
        assertEquals(b, (float) field(packet, "b"), EPSILON);
        assertEquals(lifetime, field(packet, "lifetime"));
        assertEquals(segments, field(packet, "segments"));
        assertEquals(pattern, field(packet, "pattern"));
    }

    private static Object field(S2CShaderEffectPack packet, String name) throws ReflectiveOperationException {
        Field field = S2CShaderEffectPack.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(packet);
    }

    private static String sequence(List<S2CShaderEffectPack> packets) {
        return packets.stream().map(ServerVisualBroadcasterTest::packetSummary).toList().toString();
    }

    private static String packetSummary(S2CShaderEffectPack packet) {
        S2CShaderEffectPack decoded = decode(packet);
        try {
            String type = field(decoded, "type").toString();
            Vec3 center = (Vec3) field(decoded, "center");
            Vec3 targetOrSize = (Vec3) field(decoded, "toOrSize");
            String destination = type.equals("BEAM")
                    ? String.format(Locale.ROOT, "(%.2f,%.2f,%.2f)", targetOrSize.x, targetOrSize.y, targetOrSize.z)
                    : String.format(Locale.ROOT, "%.2f", targetOrSize.x);
            return String.format(Locale.ROOT, "%s@(%.2f,%.2f,%.2f)->%s#%d/%d/%s",
                    type, center.x, center.y, center.z, destination,
                    (int) field(decoded, "lifetime"), (int) field(decoded, "segments"), field(decoded, "pattern"));
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

}
