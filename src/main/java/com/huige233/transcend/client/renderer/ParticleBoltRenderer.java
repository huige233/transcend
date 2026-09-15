package com.huige233.transcend.client.renderer;

import com.huige233.transcend.entity.projectile.ParticleBolt;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.jetbrains.annotations.NotNull;


/** 按照同步的颜色与大小，将粒子弹渲染为周期性脉动的明亮能量菱形。 */
public class ParticleBoltRenderer extends EntityRenderer<ParticleBolt> {
    public ParticleBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ParticleBolt entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        int color = entity.getColor();
        float red = ((color >>> 16) & 0xFF) / 255.0F;
        float green = ((color >>> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float size = (0.25F + 0.05F * (float) Math.sin(entity.tickCount * 0.8F)) * entity.getVisualSize();

        quad(consumer, matrix, normal, size, 0, 0, 0, size, 0, red, green, blue);
        quad(consumer, matrix, normal, 0, size, 0, 0, 0, size, red, green, blue);
        quad(consumer, matrix, normal, size, 0, 0, 0, 0, size, red, green, blue);
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float red, float green, float blue) {
        consumer.vertex(matrix, ax, ay, az).color(red, green, blue, 1.0F).endVertex();
        consumer.vertex(matrix, bx, by, bz).color(red, green, blue, 1.0F).endVertex();
        consumer.vertex(matrix, -ax, -ay, -az).color(red, green, blue, 1.0F).endVertex();
        consumer.vertex(matrix, -bx, -by, -bz).color(red, green, blue, 1.0F).endVertex();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ParticleBolt entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/forcefield.png");
    }

}
