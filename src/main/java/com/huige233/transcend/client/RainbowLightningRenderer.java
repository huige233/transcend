package com.huige233.transcend.client;

import com.huige233.transcend.entity.RainbowLightning;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.Random;

public class RainbowLightningRenderer extends EntityRenderer<RainbowLightning> {

    public RainbowLightningRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RainbowLightning entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        org.joml.Matrix4f mat = poseStack.last().pose();

        double[] offsetX = new double[8];
        double[] offsetZ = new double[8];
        double dx = 0.0;
        double dz = 0.0;

        Random random = new Random(entity.getBoltSeed());
        for (int i = 7; i >= 0; i--) {
            offsetX[i] = dx;
            offsetZ[i] = dz;
            dx += random.nextInt(11) - 5;
            dz += random.nextInt(11) - 5;
        }

        for (int pass = 0; pass < 4; pass++) {
            Random rng = new Random(entity.getBoltSeed());

            for (int branch = 0; branch < 3; branch++) {
                int top = 7;
                int bottom = 0;
                if (branch > 0) top = 7 - branch;
                if (branch > 0) bottom = top - 2;

                double curX = offsetX[top] - dx;
                double curZ = offsetZ[top] - dz;

                for (int seg = top; seg >= bottom; seg--) {
                    double prevX = curX;
                    double prevZ = curZ;

                    if (branch == 0) {
                        curX += rng.nextInt(11) - 5;
                        curZ += rng.nextInt(11) - 5;
                    } else {
                        curX += rng.nextInt(31) - 15;
                        curZ += rng.nextInt(31) - 15;
                    }

                    float r = rng.nextFloat();
                    float g = rng.nextFloat();
                    float b = rng.nextFloat();
                    float a = 0.3F + rng.nextFloat() * 0.5F;

                    double width = 0.1 + pass * 0.2;
                    if (branch == 0) width *= seg * 0.1 + 1.0;

                    double widthPrev = 0.1 + pass * 0.2;
                    if (branch == 0) widthPrev *= (seg - 1) * 0.1 + 1.0;

                    float y0 = seg * 16;
                    float y1 = (seg + 1) * 16;

                    for (int vert = 0; vert < 5; vert++) {
                        float x0 = (float)(0.5 - width    + (vert == 1 || vert == 2 ? 2 * width    : 0));
                        float z0 = (float)(0.5 - width    + (vert == 2 || vert == 3 ? 2 * width    : 0));
                        float x1 = (float)(0.5 - widthPrev + (vert == 1 || vert == 2 ? 2 * widthPrev : 0));
                        float z1 = (float)(0.5 - widthPrev + (vert == 2 || vert == 3 ? 2 * widthPrev : 0));

                        consumer.vertex(mat, (float)(x1 + curX),  y0, (float)(z1 + curZ)).color(r, g, b, a).endVertex();
                        consumer.vertex(mat, (float)(x0 + prevX), y1, (float)(z0 + prevZ)).color(r, g, b, a).endVertex();
                    }
                }
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(RainbowLightning entity) {
        return null;
    }
}
