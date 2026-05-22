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
        // Round 44: 用户要求 — 渲染半径固定为初始大小（T1），不再随 tier 缩放
        float tierScale = 1.0f;
        // Round 09: 按 tier 旋转加速 (1.0 → 3.5 倍速) — 保留
        float rotSpeed = 2.0f + (tierLevel - 1) * 0.5f;
        float rotAngle = (gameTime + partialTick) * rotSpeed;

        // Determine base color by category (existing logic)
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

        // Round 09: tier 着色 — 每阶混入特征色让玩家一眼看出升阶
        float[] rgb = applyTierTint(r, g, b, tierLevel, gameTime, partialTick);
        r = rgb[0]; g = rgb[1]; b = rgb[2];

        // Alpha pulsing — 高 tier 更亮
        float pulse = (float) Math.sin((gameTime + partialTick) * 0.1f);
        float baseAlpha = 0.55f + (tierLevel - 1) * 0.06f; // T1=0.55 → T5=0.79
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

        // Round 09: T5 (PRIMORDIAL) 额外绘制第二层旋转环（彩虹反向旋转）增强终局感
        if (tierLevel >= 5) {
            renderPrimordialOuterRing(poseStack, bufferSource, packedLight, partialTick, gameTime, radius);
        }
    }

    /**
     * Round 09: tier 颜色调制。让玩家一眼分辨阶级。
     *
     * <ul>
     *   <li>T1 INITIATE: 基础白调，无变化</li>
     *   <li>T2 ADEPT:    +冷青调（cyan 混入 0.25）</li>
     *   <li>T3 MASTER:   +金调（gold 混入 0.30）+ 亮度提升</li>
     *   <li>T4 ARCHON:   +深紫调（violet 混入 0.35）+ 高亮度</li>
     *   <li>T5 PRIMORDIAL: HSV 彩虹循环（5 秒一周期），完全覆盖基础色</li>
     * </ul>
     */
    private static float[] applyTierTint(float r, float g, float b, int tier, long gameTime, float partialTick) {
        // Round 44: 用户要求升阶变色更明显 — blend 因子从 0.25/0.30/0.35 提到 0.45/0.60/0.75
        switch (tier) {
            case 2: {
                // T2 ADEPT: 鲜亮青蓝（adept 通透感）
                float tR = 0.4f, tG = 0.95f, tB = 1.0f;
                return new float[]{ lerp(r, tR, 0.45f), lerp(g, tG, 0.45f), lerp(b, tB, 0.45f) };
            }
            case 3: {
                // T3 MASTER: 暖金（master 权威感）
                float tR = 1.0f, tG = 0.80f, tB = 0.25f;
                return new float[]{
                    Math.min(1.0f, lerp(r, tR, 0.60f) * 1.15f),
                    Math.min(1.0f, lerp(g, tG, 0.60f) * 1.15f),
                    Math.min(1.0f, lerp(b, tB, 0.60f) * 1.15f)
                };
            }
            case 4: {
                // T4 ARCHON: 紫电（archon 神性感）
                float tR = 0.85f, tG = 0.20f, tB = 1.0f;
                return new float[]{
                    Math.min(1.0f, lerp(r, tR, 0.75f) * 1.25f),
                    Math.min(1.0f, lerp(g, tG, 0.75f) * 1.25f),
                    Math.min(1.0f, lerp(b, tB, 0.75f) * 1.25f)
                };
            }
            case 5: {
                // T5 PRIMORDIAL: HSV 彩虹循环（5 秒一周期）— 完全覆盖基础色
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

    /** 极简 HSV→RGB（s/v ∈ [0,1], h ∈ [0,1)）。 */
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

    /** Round 09: T5 终局环 — 在主法阵外多绘一圈反向旋转的彩虹环。 */
    private static void renderPrimordialOuterRing(PoseStack poseStack, MultiBufferSource bufferSource,
                                                   int packedLight, float partialTick, long gameTime, float innerRadius) {
        float hue = ((gameTime + partialTick + 50) % 100f) / 100f; // 相位偏移
        float[] rgb = hsvToRgb(hue, 0.7f, 1.0f);
        float pulse = (float) Math.sin((gameTime + partialTick) * 0.15f);
        float alpha = 0.50f + 0.20f * pulse;

        poseStack.pushPose();
        poseStack.translate(0.5, 1.55, 0.5);
        // 反向旋转
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
// 注册方式：在客户端事件 EntityRenderersEvent.RegisterRenderers 中调用:
// event.registerBlockEntityRenderer(ModBlockEntities.CIRCLE_CORE_BE.get(), CircleCoreRenderer::new);