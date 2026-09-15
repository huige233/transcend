package com.huige233.transcend.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.junit.jupiter.api.Test;

import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

/** 验证相位护盾状态配色及三维椭球表面的顶点数量、坐标范围和透明度。 */
class PhaseShieldRenderHandlerTest {
    @Test
    void chargeBandsUseCyanRedAndOrangeEndpoints() {
        assertArrayEquals(new int[]{120, 225, 255}, PhaseShieldRenderHandler.colorFor(1.0F,
                com.huige233.transcend.tech.shield.PhaseShieldStatus.ACTIVE));
        int[] nearEmpty = PhaseShieldRenderHandler.colorFor(0.20F,
                com.huige233.transcend.tech.shield.PhaseShieldStatus.ACTIVE);
        assertTrue(nearEmpty[0] > nearEmpty[2], "near-empty shield should be red-dominant");
        int[] low = PhaseShieldRenderHandler.colorFor(0.04F,
                com.huige233.transcend.tech.shield.PhaseShieldStatus.ACTIVE);
        assertTrue(low[0] > low[1] && low[1] > low[2], "low shield should be orange-dominant");
        assertArrayEquals(new int[]{255, 145, 20}, PhaseShieldRenderHandler.colorFor(0.5F,
                com.huige233.transcend.tech.shield.PhaseShieldStatus.EMPTY));
    }
    @Test
    void shieldIsACompleteThreeDimensionalSurfaceWithValidVertices() {
        BufferBuilder builder = new BufferBuilder(131072);
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        assertDoesNotThrow(() -> PhaseShieldRenderHandler.drawShell(builder, new PoseStack().last(),
                0.75F, 1.35F, 0, 225, 255, 0.0F));
        BufferBuilder.RenderedBuffer rendered = assertDoesNotThrow(builder::end);
        try {
            var data = rendered.vertexBuffer().order(ByteOrder.nativeOrder());
            int stride = DefaultVertexFormat.POSITION_COLOR.getVertexSize();
            int count = PhaseShieldRenderHandler.LATITUDES * PhaseShieldRenderHandler.LONGITUDES * 4;
            assertEquals(count * stride, data.remaining());
            float[] min = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY};
            float[] max = {Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
            for (int vertex = 0; vertex < count; vertex++) {
                int offset = vertex * stride;
                for (int axis = 0; axis < 3; axis++) {
                    float coordinate = data.getFloat(offset + axis * 4);
                    assertTrue(Float.isFinite(coordinate));
                    min[axis] = Math.min(min[axis], coordinate);
                    max[axis] = Math.max(max[axis], coordinate);
                }
                float x = data.getFloat(offset) / 0.75F;
                float y = data.getFloat(offset + 4) / 1.35F;
                float z = data.getFloat(offset + 8) / 0.75F;
                assertEquals(1.0F, x * x + y * y + z * z, 0.0001F);
                int alpha = Byte.toUnsignedInt(data.get(offset + 15));
                assertTrue(alpha >= 35 && alpha <= 100);
            }
            assertArrayEquals(new float[]{-0.75F, -1.35F, -0.75F}, min, 0.0001F);
            assertArrayEquals(new float[]{0.75F, 1.35F, 0.75F}, max, 0.0001F);
        } finally {
            rendered.release();
        }
    }
}
