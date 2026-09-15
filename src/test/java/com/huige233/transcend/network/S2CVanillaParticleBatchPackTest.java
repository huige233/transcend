package com.huige233.transcend.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证原版灰烬粒子批量包的坐标与速度往返，并检查粒子标识解析和灰烬类型映射。 */
class S2CVanillaParticleBatchPackTest {
    @Test
    void ashPayloadRoundTripsEveryEntryField() throws ReflectiveOperationException {
        List<S2CVanillaParticleBatchPack.VanillaParticleEntry> entries = List.of(
                new S2CVanillaParticleBatchPack.VanillaParticleEntry(1.25, 2.5, -3.75, 0.1, 0.2, 0.3),
                new S2CVanillaParticleBatchPack.VanillaParticleEntry(-4.5, 5.75, 6.0, -0.4, 0.5, -0.6));

        S2CVanillaParticleBatchPack decoded = decode(new S2CVanillaParticleBatchPack(entries, "ash"));
        assertEquals("ash", field(decoded, "particleId"));
        @SuppressWarnings("unchecked")
        List<S2CVanillaParticleBatchPack.VanillaParticleEntry> decodedEntries =
                (List<S2CVanillaParticleBatchPack.VanillaParticleEntry>) field(decoded, "entries");
        assertEquals(2, decodedEntries.size());
        assertEntry(entries.get(0), decodedEntries.get(0));
        assertEntry(entries.get(1), decodedEntries.get(1));
    }

    @Test
    void avatarMeteorAshIdIsSupportedAndMapsToAshParticle() throws Exception {
        assertEquals("ash", S2CVanillaParticleBatchPack.resolveParticleId("ash"));
        assertNull(S2CVanillaParticleBatchPack.resolveParticleId("unknown"));
        assertNull(S2CVanillaParticleBatchPack.resolveParticleId(null));
        String source = Files.readString(Path.of(
                "src/main/java/com/huige233/transcend/network/S2CVanillaParticleBatchPack.java"));
        assertTrue(source.contains("String supportedId = resolveParticleId(id);"));
        assertTrue(source.contains("case \"ash\" -> ParticleTypes.ASH;"));
    }

    private static S2CVanillaParticleBatchPack decode(S2CVanillaParticleBatchPack packet) {
        ByteBuf backing = Unpooled.buffer();
        try {
            FriendlyByteBuf buffer = new FriendlyByteBuf(backing);
            packet.write(buffer);
            return new S2CVanillaParticleBatchPack(buffer);
        } finally {
            backing.release();
        }
    }

    private static Object field(S2CVanillaParticleBatchPack packet, String name)
            throws ReflectiveOperationException {
        Field field = S2CVanillaParticleBatchPack.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(packet);
    }

    private static void assertEntry(S2CVanillaParticleBatchPack.VanillaParticleEntry expected,
                                    S2CVanillaParticleBatchPack.VanillaParticleEntry actual) {
        assertEquals(expected.x, actual.x);
        assertEquals(expected.y, actual.y);
        assertEquals(expected.z, actual.z);
        assertEquals(expected.xd, actual.xd);
        assertEquals(expected.yd, actual.yd);
        assertEquals(expected.zd, actual.zd);
    }
}
