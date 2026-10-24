package com.huige233.transcend.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

class MiniUniverseEyeEffectTest {
    @ParameterizedTest
    @ValueSource(doubles = {0, .5, .9, .95, 1})
    void allEvolutionStagesProduceBoundedQuadsWithFaintHalos(double usage) {
        MeshStats stats = draw(usage, false, new Vec3(1, .3, .2).normalize());
        assertTrue(stats.vertices > 0);
        assertEquals(0, stats.vertices % 4);
        assertTrue(stats.faint > 0, "halos below 0.1 alpha must survive the vertex pipeline");
        assertTrue(stats.opaque > 0, "the central body must retain opaque surfaces");
        assertTrue(stats.minY < -.48 && stats.maxY > .48, "the sphere must remain complete");
    }

    @Test
    void distantViewReducesGeometryWhileKeepingCoreAndShell() {
        MeshStats near = draw(.5, false, new Vec3(0, 0, 1));
        MeshStats distant = draw(.5, true, new Vec3(0, 0, 1));
        assertTrue(distant.vertices < near.vertices / 2);
        assertTrue(distant.opaque > 0);
        assertEquals(near.minY, distant.minY, .0001);
        assertEquals(near.maxY, distant.maxY, .0001);
    }

    @Test
    void collapsedUniverseKeepsDarkCoreAndRemovesOrbitingWorlds() {
        MeshStats initial = draw(0, false, new Vec3(0, 0, 1));
        MeshStats collapsed = draw(1, false, new Vec3(0, 0, 1));
        assertTrue(initial.warmCore > 0);
        assertEquals(0, initial.darkCore);
        assertTrue(collapsed.darkCore > 0, "black-hole surfaces must retain their near-black color");
        assertEquals(0, collapsed.warmCore);
        assertTrue(collapsed.vertices < initial.vertices);
    }

    @Test
    void poleViewsKeepBillboardGeometryFinite() {
        assertEquals(draw(0, false, new Vec3(0, 1, 0)).vertices,
                draw(0, false, new Vec3(0, -1, 0)).vertices);
    }

    private static MeshStats draw(double usage, boolean distant, Vec3 camera) {
        BufferBuilder builder = new BufferBuilder(524288);
        // RenderType initialization requires a running Minecraft registry; exercise the emitted mesh here.
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        MiniUniverseEyeEffect.drawUniverse(builder, new PoseStack().last(), camera, 123.5, usage, distant);
        BufferBuilder.RenderedBuffer rendered = builder.end();
        try {
            var data = rendered.vertexBuffer().order(ByteOrder.nativeOrder());
            int stride = DefaultVertexFormat.POSITION_COLOR.getVertexSize();
            assertEquals(0, data.remaining() % stride);
            MeshStats stats = new MeshStats();
            stats.vertices = data.remaining() / stride;
            for (int offset = 0; offset < data.remaining(); offset += stride) {
                float x = data.getFloat(offset);
                float y = data.getFloat(offset + 4);
                float z = data.getFloat(offset + 8);
                assertTrue(Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z));
                double radiusSquared = x * x + y * y + z * z;
                assertTrue(radiusSquared <= .508 * .508, "vertices must stay inside the render bounds");
                stats.minY = Math.min(stats.minY, y);
                stats.maxY = Math.max(stats.maxY, y);
                int red = Byte.toUnsignedInt(data.get(offset + 12));
                int green = Byte.toUnsignedInt(data.get(offset + 13));
                int blue = Byte.toUnsignedInt(data.get(offset + 14));
                int alpha = Byte.toUnsignedInt(data.get(offset + 15));
                assertTrue(alpha > 0);
                if (alpha <= 7) stats.faint++;
                if (alpha == 255) {
                    stats.opaque++;
                    if (radiusSquared < .1 * .1) {
                        if (red <= 3 && green <= 3 && blue <= 4) stats.darkCore++;
                        if (red > 50 && green > 20 && blue < 20) stats.warmCore++;
                    }
                }
            }
            return stats;
        } finally {
            rendered.release();
        }
    }

    private static class MeshStats {
        int vertices;
        int faint;
        int opaque;
        int darkCore;
        int warmCore;
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
    }
}
