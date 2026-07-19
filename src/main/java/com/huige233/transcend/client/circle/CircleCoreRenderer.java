package com.huige233.transcend.client.circle;

import com.huige233.transcend.block.circle.MagicCircleCoreBlockEntity;
import com.huige233.transcend.circle.CircleCategory;
import com.huige233.transcend.circle.CircleFunctionType;
import com.huige233.transcend.circle.CircleTier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class CircleCoreRenderer implements BlockEntityRenderer<MagicCircleCoreBlockEntity> {

    private static final ResourceLocation CIRCLE_TEXTURE = new ResourceLocation("transcend", "textures/entity/circle_pattern.png");

    public CircleCoreRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MagicCircleCoreBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!blockEntity.isActive() || !blockEntity.isStructureValid()) {
            return;
        }

        long gameTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0;
        CircleFunctionType function = blockEntity.getActiveFunction();
        CircleTier tier = blockEntity.getDetectedTier();

        int tierLevel = tier != null ? tier.getLevel() : 1;

        float tierScale = 1.0f;

        float rotSpeed = 2.0f + (tierLevel - 1) * 0.5f;
        float rotAngle = (gameTime + partialTick) * rotSpeed;

        float r = 0.9f, g = 0.9f, b = 1.0f;
        if (function != null) {
            CircleCategory category = function.getCategory();
            if (category != null) {
                switch (category) {
                    case MANA_LOGISTICS:
                        r = 0.3f; g = 0.8f; b = 1.0f;
                        break;
                    case PLAYER_BUFF:
                        r = 0.3f; g = 1.0f; b = 0.5f;
                        break;
                    case WORLD_INTERACTION:
                        r = 1.0f; g = 0.85f; b = 0.3f;
                        break;
                    case ADVANCED:
                        r = 0.8f; g = 0.3f; b = 1.0f;
                        break;
                    case FARMING:
                        r = 0.5f; g = 1.0f; b = 0.3f;
                        break;
                    case DEFENSE:
                        r = 1.0f; g = 0.4f; b = 0.3f;
                        break;
                }
            }
        }

        float[] rgb = applyTierTint(r, g, b, tierLevel, gameTime, partialTick);
        r = rgb[0]; g = rgb[1]; b = rgb[2];

        float pulse = (float) Math.sin((gameTime + partialTick) * 0.1f);
        float baseAlpha = 0.55f + (tierLevel - 1) * 0.06f;
        float alpha = baseAlpha + 0.2f * pulse;

        poseStack.pushPose();
        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotAngle));
        poseStack.mulPose(Axis.XP.rotationDegrees(90));

        float radius = 1.5f * tierScale;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(CIRCLE_TEXTURE));
        Matrix4f pose = poseStack.last().pose();

        consumer.vertex(pose, -radius, radius, 0).color(r, g, b, alpha).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        consumer.vertex(pose, radius, radius, 0).color(r, g, b, alpha).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        consumer.vertex(pose, radius, -radius, 0).color(r, g, b, alpha).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        consumer.vertex(pose, -radius, -radius, 0).color(r, g, b, alpha).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();

        consumer.vertex(pose, -radius, -radius, 0).color(r, g, b, alpha).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, -1).endVertex();
        consumer.vertex(pose, radius, -radius, 0).color(r, g, b, alpha).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, -1).endVertex();
        consumer.vertex(pose, radius, radius, 0).color(r, g, b, alpha).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, -1).endVertex();
        consumer.vertex(pose, -radius, radius, 0).color(r, g, b, alpha).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, -1).endVertex();

        poseStack.popPose();

        if (tierLevel >= 5) {
            renderPrimordialOuterRing(poseStack, bufferSource, packedLight, partialTick, gameTime, radius);
        }
    }

    private static float[] applyTierTint(float r, float g, float b, int tier, long gameTime, float partialTick) {

        switch (tier) {
            case 2: {

                float tR = 0.4f, tG = 0.95f, tB = 1.0f;
                return new float[]{ lerp(r, tR, 0.45f), lerp(g, tG, 0.45f), lerp(b, tB, 0.45f) };
            }
            case 3: {

                float tR = 1.0f, tG = 0.80f, tB = 0.25f;
                return new float[]{
                    Math.min(1.0f, lerp(r, tR, 0.60f) * 1.15f),
                    Math.min(1.0f, lerp(g, tG, 0.60f) * 1.15f),
                    Math.min(1.0f, lerp(b, tB, 0.60f) * 1.15f)
                };
            }
            case 4: {

                float tR = 0.85f, tG = 0.20f, tB = 1.0f;
                return new float[]{
                    Math.min(1.0f, lerp(r, tR, 0.75f) * 1.25f),
                    Math.min(1.0f, lerp(g, tG, 0.75f) * 1.25f),
                    Math.min(1.0f, lerp(b, tB, 0.75f) * 1.25f)
                };
            }
            case 5: {

                float hue = ((gameTime + partialTick) % 100f) / 100f;
                return hsvToRgb(hue, 0.85f, 1.0f);
            }
            case 1:
            default:
                return new float[]{ r, g, b };
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float[] hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1 - Math.abs(((h * 6f) % 2f) - 1));
        float m = v - c;
        float r, g, b;
        int sector = (int)(h * 6f) % 6;
        switch (sector) {
            case 0: r = c; g = x; b = 0; break;
            case 1: r = x; g = c; b = 0; break;
            case 2: r = 0; g = c; b = x; break;
            case 3: r = 0; g = x; b = c; break;
            case 4: r = x; g = 0; b = c; break;
            case 5: r = c; g = 0; b = x; break;
            default: r = 0; g = 0; b = 0;
        }
        return new float[]{ r + m, g + m, b + m };
    }

    private static void renderPrimordialOuterRing(PoseStack poseStack, MultiBufferSource bufferSource,
                                                   int packedLight, float partialTick, long gameTime, float innerRadius) {
        float hue = ((gameTime + partialTick + 50) % 100f) / 100f;
        float[] rgb = hsvToRgb(hue, 0.7f, 1.0f);
        float pulse = (float) Math.sin((gameTime + partialTick) * 0.15f);
        float alpha = 0.50f + 0.20f * pulse;

        poseStack.pushPose();
        poseStack.translate(0.5, 1.55, 0.5);

        poseStack.mulPose(Axis.YP.rotationDegrees(-(gameTime + partialTick) * 1.5f));
        poseStack.mulPose(Axis.XP.rotationDegrees(90));

        float outerRadius = innerRadius * 1.5f;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(CIRCLE_TEXTURE));
        Matrix4f pose = poseStack.last().pose();

        consumer.vertex(pose, -outerRadius, outerRadius, 0).color(rgb[0], rgb[1], rgb[2], alpha).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        consumer.vertex(pose, outerRadius, outerRadius, 0).color(rgb[0], rgb[1], rgb[2], alpha).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        consumer.vertex(pose, outerRadius, -outerRadius, 0).color(rgb[0], rgb[1], rgb[2], alpha).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();
        consumer.vertex(pose, -outerRadius, -outerRadius, 0).color(rgb[0], rgb[1], rgb[2], alpha).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, 1).endVertex();

        consumer.vertex(pose, -outerRadius, -outerRadius, 0).color(rgb[0], rgb[1], rgb[2], alpha).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, -1).endVertex();
        consumer.vertex(pose, outerRadius, -outerRadius, 0).color(rgb[0], rgb[1], rgb[2], alpha).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, -1).endVertex();
        consumer.vertex(pose, outerRadius, outerRadius, 0).color(rgb[0], rgb[1], rgb[2], alpha).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, -1).endVertex();
        consumer.vertex(pose, -outerRadius, outerRadius, 0).color(rgb[0], rgb[1], rgb[2], alpha).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 0, -1).endVertex();

        poseStack.popPose();
    }
}

