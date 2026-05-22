package com.huige233.transcend.client.renderer;

import com.huige233.transcend.client.model.TranscendBossModel;
import com.huige233.transcend.entity.boss.AbstractTranscendBoss;
import com.huige233.transcend.entity.boss.BossPhase;
import com.huige233.transcend.entity.boss.TranscendenceAvatar;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class TranscendBossRenderer<T extends AbstractTranscendBoss> extends MobRenderer<T, TranscendBossModel<T>> {

    private final ResourceLocation texture;
    private final ResourceLocation glowTexture;

    public TranscendBossRenderer(EntityRendererProvider.Context ctx, TranscendBossModel<T> model,
                                  ResourceLocation texture, ResourceLocation glowTexture) {
        super(ctx, model, 0.6F);
        this.texture = texture;
        this.glowTexture = glowTexture;
        this.addLayer(new EmissiveGlowLayer(this));
        this.addLayer(new EnergySwirlLayer(this));
        this.addLayer(new GroundCircleLayer(this));
        this.addLayer(new SkillEffectLayer(this));
        this.addLayer(new EnergyWingLayer(this));
    }

    @Override
    public void render(@NotNull T entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        float ageInTicks = entity.tickCount + partialTick;
        float hover = Mth.sin(ageInTicks * 0.04F) * 0.1F;
        poseStack.translate(0, hover, 0);

        float scale = getPhaseScale(entity);
        poseStack.scale(scale, scale, scale);

        int transitionTick = entity.getPhaseTransitionTick();
        if (transitionTick > 0) {
            float flash = transitionTick / 30.0F;
            float flashScale = 1.0F + Mth.sin(flash * (float) Math.PI) * 0.12F;
            poseStack.scale(flashScale, flashScale, flashScale);
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        // 护盾球：在实体坐标系下独立绘制，与模型缩放无关
        if (entity instanceof TranscendenceAvatar avatar && avatar.getShieldHealth() > 0) {
            renderShieldSphere(avatar, ageInTicks, scale, poseStack, bufferSource);
        }

        poseStack.popPose();
    }

    /** 绘制独立护盾球，两遍（背面+正面），每像素仅1层叠加，颜色不饱和。 */
    private void renderShieldSphere(TranscendenceAvatar avatar, float age, float phaseScale,
                                    PoseStack ps, MultiBufferSource buf) {
        float shieldRatio = avatar.getShieldHealth() / Math.max(1, avatar.getMaxShield());
        float pulse = 0.90F + 0.10F * Mth.sin(age * 0.07F);
        // 始终可见的浅蓝色半透明护盾，有盾时更亮
        float alpha = (0.15F + shieldRatio * 0.10F) * pulse;

        // 浅蓝色
        float cr = 0.4F, cg = 0.7F, cb = 1.0F;

        // 球心：实体脚底往上 1 格（模型中心约在 Y=1）
        ps.pushPose();
        // 抵消 render() 里施加的 phaseScale，让球半径不随阶段缩放
        float invScale = 1f / phaseScale;
        ps.scale(invScale, invScale, invScale);
        ps.translate(0, 1.0, 0);

        float radius = 1.65F; // 比模型稍大即可
        int stacks = 16, slices = 24;

        // 预计算
        float[] sy = new float[stacks + 1], sr = new float[stacks + 1];
        for (int i = 0; i <= stacks; i++) {
            double t = Math.PI * i / stacks;
            sy[i] = (float)(Math.cos(t) * radius);
            sr[i] = (float)(Math.sin(t) * radius);
        }
        float[] sc = new float[slices + 1], ss = new float[slices + 1];
        for (int j = 0; j <= slices; j++) {
            double p = 2.0 * Math.PI * j / slices;
            sc[j] = (float)Math.cos(p);
            ss[j] = (float)Math.sin(p);
        }

        // 第一遍：背面（CULL_FRONT）
        VertexConsumer back = buf.getBuffer(TranscendRenderTypes.shieldBack());
        drawSphereQuads(back, ps.last().pose(), sy, sr, sc, ss, stacks, slices, cr, cg, cb, alpha);

        // 第二遍：正面（CULL_BACK，默认）
        VertexConsumer front = buf.getBuffer(TranscendRenderTypes.shieldFront());
        drawSphereQuads(front, ps.last().pose(), sy, sr, sc, ss, stacks, slices, cr, cg, cb, alpha);

        ps.popPose();
    }

    private void drawSphereQuads(VertexConsumer vc, Matrix4f mat,
                                  float[] sy, float[] sr, float[] sc, float[] ss,
                                  int stacks, int slices,
                                  float r, float g, float b, float a) {
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                vc.vertex(mat, sr[i]  *sc[j],   sy[i],   sr[i]  *ss[j]  ).color(r,g,b,a).endVertex();
                vc.vertex(mat, sr[i+1]*sc[j],   sy[i+1], sr[i+1]*ss[j]  ).color(r,g,b,a).endVertex();
                vc.vertex(mat, sr[i+1]*sc[j+1], sy[i+1], sr[i+1]*ss[j+1]).color(r,g,b,a).endVertex();
                vc.vertex(mat, sr[i]  *sc[j+1], sy[i],   sr[i]  *ss[j+1]).color(r,g,b,a).endVertex();
            }
        }
    }

    @Override
    protected float getFlipDegrees(@NotNull T entity) { return 0; }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull T entity) { return texture; }

    private float getPhaseScale(T entity) {
        return switch (entity.getCurrentPhase()) {
            case PHASE_1 -> 1.0F;
            case PHASE_2 -> 1.05F;
            case PHASE_3 -> 1.12F;
            case PHASE_4 -> 1.25F;
        };
    }

    // --- Layer 1: Emissive glow overlay ---
    private class EmissiveGlowLayer extends RenderLayer<T, TranscendBossModel<T>> {
        public EmissiveGlowLayer(TranscendBossRenderer<T> renderer) { super(renderer); }

        @Override
        public void render(@NotNull PoseStack ps, @NotNull MultiBufferSource buf, int light,
                           @NotNull T entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float age, float headYaw, float headPitch) {

            float r, g, b;
            if (entity.getCurrentElement() != null) {
                r = entity.getCurrentElement().particleR;
                g = entity.getCurrentElement().particleG;
                b = entity.getCurrentElement().particleB;
            } else { r = g = b = 1.0F; }

            // Phase1: very subtle eye glow only; Phase4: full body glow
            float alpha = switch (entity.getCurrentPhase()) {
                case PHASE_1 -> 0.08F;
                case PHASE_2 -> 0.2F;
                case PHASE_3 -> 0.4F;
                case PHASE_4 -> 0.65F;
            };

            float pulse = 0.7F + 0.3F * Mth.sin(age * 0.06F);
            alpha *= pulse;

            int transitionTick = entity.getPhaseTransitionTick();
            if (transitionTick > 0) {
                float flash = transitionTick / 30.0F;
                alpha = Math.min(0.95F, alpha + flash * 0.6F);
                r = Math.min(1, r + flash * 0.4F);
                g = Math.min(1, g + flash * 0.4F);
                b = Math.min(1, b + flash * 0.4F);
            }

            if (alpha < 0.02F) return;

            VertexConsumer vc = buf.getBuffer(TranscendRenderTypes.magicGlow(glowTexture));
            this.getParentModel().renderToBuffer(ps, vc, 0xF000F0, OverlayTexture.NO_OVERLAY,
                    Math.min(1, r * pulse), Math.min(1, g * pulse), Math.min(1, b * pulse), alpha);
        }
    }

    // --- Layer 2: Energy swirl (Phase 2+) ---
    private class EnergySwirlLayer extends RenderLayer<T, TranscendBossModel<T>> {
        public EnergySwirlLayer(TranscendBossRenderer<T> renderer) { super(renderer); }

        @Override
        public void render(@NotNull PoseStack ps, @NotNull MultiBufferSource buf, int light,
                           @NotNull T entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float age, float headYaw, float headPitch) {

            if (entity.getCurrentPhase() == BossPhase.PHASE_1) return;

            float speed = switch (entity.getCurrentPhase()) {
                case PHASE_1 -> 0;
                case PHASE_2 -> 0.008F;
                case PHASE_3 -> 0.015F;
                case PHASE_4 -> 0.03F;
            };

            float alpha = switch (entity.getCurrentPhase()) {
                case PHASE_1 -> 0;
                case PHASE_2 -> 0.08F;
                case PHASE_3 -> 0.18F;
                case PHASE_4 -> 0.35F;
            };

            float u = (entity.tickCount + partialTick) * speed;

            ps.pushPose();
            float swirlScale = 1.03F + Mth.sin(age * 0.04F) * 0.02F;
            ps.scale(swirlScale, swirlScale, swirlScale);

            float r, g, b;
            if (entity.getCurrentElement() != null) {
                r = entity.getCurrentElement().particleR;
                g = entity.getCurrentElement().particleG;
                b = entity.getCurrentElement().particleB;
            } else { r = g = b = 1.0F; }

            float pulse = 0.6F + 0.4F * Mth.sin(age * 0.12F);

            VertexConsumer vc = buf.getBuffer(TranscendRenderTypes.magicGlow(glowTexture));
            this.getParentModel().renderToBuffer(ps, vc, 0xF000F0, OverlayTexture.NO_OVERLAY,
                    Math.min(1, r * pulse * 1.2F), Math.min(1, g * pulse * 1.2F),
                    Math.min(1, b * pulse * 1.2F), alpha);
            ps.popPose();
        }
    }

    // --- Layer 3: Ground magic circle (shader-drawn) ---
    private class GroundCircleLayer extends RenderLayer<T, TranscendBossModel<T>> {
        public GroundCircleLayer(TranscendBossRenderer<T> renderer) { super(renderer); }

        @Override
        public void render(@NotNull PoseStack ps, @NotNull MultiBufferSource buf, int light,
                           @NotNull T entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float age, float headYaw, float headPitch) {

            float circleAlpha = switch (entity.getCurrentPhase()) {
                case PHASE_1 -> 0;
                case PHASE_2 -> 0.3F;
                case PHASE_3 -> 0.5F;
                case PHASE_4 -> 0.8F;
            };
            if (circleAlpha <= 0) return;

            float r, g, b;
            if (entity.getCurrentElement() != null) {
                r = entity.getCurrentElement().particleR;
                g = entity.getCurrentElement().particleG;
                b = entity.getCurrentElement().particleB;
            } else { r = g = b = 1.0F; }

            float radius = switch (entity.getCurrentPhase()) {
                case PHASE_1 -> 0;
                case PHASE_2 -> 2.0F;
                case PHASE_3 -> 3.0F;
                case PHASE_4 -> 4.0F;
            };

            float rotation = (entity.tickCount + partialTick) * 1.5F;

            ps.pushPose();
            ps.translate(0, 0.02, 0);
            ps.mulPose(Axis.YP.rotationDegrees(rotation));

            com.mojang.blaze3d.vertex.VertexConsumer vc = buf.getBuffer(
                    RenderType.entityTranslucentEmissive(glowTexture));

            Matrix4f mat = ps.last().pose();
            int segments = 32;
            float innerR = radius * 0.7F;

            // Outer ring
            for (int i = 0; i < segments; i++) {
                float a1 = (float) (Math.PI * 2 * i / segments);
                float a2 = (float) (Math.PI * 2 * (i + 1) / segments);

                float x1o = Mth.cos(a1) * radius, z1o = Mth.sin(a1) * radius;
                float x2o = Mth.cos(a2) * radius, z2o = Mth.sin(a2) * radius;
                float x1i = Mth.cos(a1) * innerR, z1i = Mth.sin(a1) * innerR;
                float x2i = Mth.cos(a2) * innerR, z2i = Mth.sin(a2) * innerR;

                float segAlpha = circleAlpha * (0.6F + 0.4F * Mth.sin(a1 * 3 + age * 0.05F));

                vc.vertex(mat, x1o, 0, z1o).color(r, g, b, segAlpha)
                        .uv(0.5F + Mth.cos(a1) * 0.5F, 0.5F + Mth.sin(a1) * 0.5F)
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                        .normal(0, 1, 0).endVertex();
                vc.vertex(mat, x1i, 0, z1i).color(r, g, b, segAlpha * 0.5F)
                        .uv(0.5F + Mth.cos(a1) * 0.35F, 0.5F + Mth.sin(a1) * 0.35F)
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                        .normal(0, 1, 0).endVertex();
                vc.vertex(mat, x2i, 0, z2i).color(r, g, b, segAlpha * 0.5F)
                        .uv(0.5F + Mth.cos(a2) * 0.35F, 0.5F + Mth.sin(a2) * 0.35F)
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                        .normal(0, 1, 0).endVertex();
                vc.vertex(mat, x2o, 0, z2o).color(r, g, b, segAlpha)
                        .uv(0.5F + Mth.cos(a2) * 0.5F, 0.5F + Mth.sin(a2) * 0.5F)
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                        .normal(0, 1, 0).endVertex();
            }

            // Inner sigil lines (hexagram)
            float sigilR = innerR * 0.8F;
            int points = 6;
            for (int i = 0; i < points; i++) {
                float a1 = (float) (Math.PI * 2 * i / points);
                float a2 = (float) (Math.PI * 2 * ((i + 2) % points) / points);

                float x1 = Mth.cos(a1) * sigilR, z1 = Mth.sin(a1) * sigilR;
                float x2 = Mth.cos(a2) * sigilR, z2 = Mth.sin(a2) * sigilR;
                float lineW = 0.06F;

                float dx = x2 - x1, dz = z2 - z1;
                float len = Mth.sqrt(dx * dx + dz * dz);
                float nx = -dz / len * lineW, nz = dx / len * lineW;

                float lineAlpha = circleAlpha * 0.8F;
                vc.vertex(mat, x1 + nx, 0.01F, z1 + nz).color(r, g, b, lineAlpha)
                        .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                        .normal(0, 1, 0).endVertex();
                vc.vertex(mat, x1 - nx, 0.01F, z1 - nz).color(r, g, b, lineAlpha)
                        .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                        .normal(0, 1, 0).endVertex();
                vc.vertex(mat, x2 - nx, 0.01F, z2 - nz).color(r, g, b, lineAlpha)
                        .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                        .normal(0, 1, 0).endVertex();
                vc.vertex(mat, x2 + nx, 0.01F, z2 + nz).color(r, g, b, lineAlpha)
                        .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                        .normal(0, 1, 0).endVertex();
            }

            ps.popPose();
        }
    }

    // --- Layer 4: Skill effect rendering (beam/slam visual feedback) ---
    private class SkillEffectLayer extends RenderLayer<T, TranscendBossModel<T>> {
        public SkillEffectLayer(TranscendBossRenderer<T> renderer) { super(renderer); }

        @Override
        public void render(@NotNull PoseStack ps, @NotNull MultiBufferSource buf, int light,
                           @NotNull T entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float age, float headYaw, float headPitch) {
            int animTick = entity.getAttackAnimTick();
            int animType = entity.getAttackAnimType();
            if (animTick <= 0 || animType == 0) return;

            float r, g, b;
            if (entity.getCurrentElement() != null) {
                r = entity.getCurrentElement().particleR;
                g = entity.getCurrentElement().particleG;
                b = entity.getCurrentElement().particleB;
            } else { r = g = b = 1.0F; }

            float progress = animTick / (animType == 1 ? 15.0F : 12.0F);
            VertexConsumer vc = buf.getBuffer(TranscendRenderTypes.magicGlow(glowTexture));
            Matrix4f mat = ps.last().pose();

            if (animType == 1) {
                // Beam: horizontal expanding light plane from chest
                ps.pushPose();
                float beamLen = 12.0F * (1.0F - progress);
                float beamW = 0.3F + (1.0F - progress) * 0.2F;
                float beamAlpha = progress * 0.7F;
                float yaw = (float) Math.toRadians(-entity.getYRot());

                ps.mulPose(Axis.YP.rotation(yaw));
                mat = ps.last().pose();

                vc.vertex(mat, 0, 1.2F, -beamW).color(r, g, b, beamAlpha)
                        .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                        .normal(0, 1, 0).endVertex();
                vc.vertex(mat, 0, 1.2F, beamW).color(r, g, b, beamAlpha)
                        .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                        .normal(0, 1, 0).endVertex();
                vc.vertex(mat, beamLen, 1.2F, beamW * 0.5F).color(r, g, b, beamAlpha * 0.3F)
                        .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                        .normal(0, 1, 0).endVertex();
                vc.vertex(mat, beamLen, 1.2F, -beamW * 0.5F).color(r, g, b, beamAlpha * 0.3F)
                        .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                        .normal(0, 1, 0).endVertex();

                ps.popPose();
            } else if (animType == 2) {
                // Slam: expanding ground ring
                float slamRadius = 6.0F * (1.0F - progress);
                float slamAlpha = progress * 0.6F;
                float innerR = slamRadius * 0.8F;
                int segs = 24;

                ps.pushPose();
                ps.translate(0, 0.05, 0);
                mat = ps.last().pose();

                for (int i = 0; i < segs; i++) {
                    float a1 = (float)(Math.PI * 2 * i / segs);
                    float a2 = (float)(Math.PI * 2 * (i + 1) / segs);

                    vc.vertex(mat, Mth.cos(a1) * slamRadius, 0, Mth.sin(a1) * slamRadius)
                            .color(r, g, b, slamAlpha)
                            .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                            .normal(0, 1, 0).endVertex();
                    vc.vertex(mat, Mth.cos(a1) * innerR, 0, Mth.sin(a1) * innerR)
                            .color(r, g, b, slamAlpha * 0.3F)
                            .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                            .normal(0, 1, 0).endVertex();
                    vc.vertex(mat, Mth.cos(a2) * innerR, 0, Mth.sin(a2) * innerR)
                            .color(r, g, b, slamAlpha * 0.3F)
                            .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                            .normal(0, 1, 0).endVertex();
                    vc.vertex(mat, Mth.cos(a2) * slamRadius, 0, Mth.sin(a2) * slamRadius)
                            .color(r, g, b, slamAlpha)
                            .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                            .normal(0, 1, 0).endVertex();
                }
                ps.popPose();
            }
        }
    }

    // --- Layer 5: Energy wing shader panels ---
    // Draws wing quads directly in model space, matching the body offset/rotation that
    // MobRenderer sets before calling layers. Panel coordinates are in model units (same
    // scale as the body cubes: ±4 = body half-width). Each wing is two overlapping sheets
    // with slight angle offset for a feathered energy look.
    private class EnergyWingLayer extends RenderLayer<T, TranscendBossModel<T>> {
        public EnergyWingLayer(TranscendBossRenderer<T> renderer) { super(renderer); }

        @Override
        public void render(@NotNull PoseStack ps, @NotNull MultiBufferSource buf, int light,
                           @NotNull T entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float age, float headYaw, float headPitch) {
            if (!(entity instanceof com.huige233.transcend.entity.boss.TranscendenceAvatar)) return;

            float r = 1f, g = 1f, b = 1f;
            if (entity.getCurrentElement() != null) {
                r = entity.getCurrentElement().particleR;
                g = entity.getCurrentElement().particleG;
                b = entity.getCurrentElement().particleB;
            }
            float phaseAlpha = switch (entity.getCurrentPhase()) {
                case PHASE_1 -> 0.50F; case PHASE_2 -> 0.65F;
                case PHASE_3 -> 0.80F; case PHASE_4 -> 1.00F;
            };
            float pulse = 0.80F + 0.20F * Mth.sin(age * 0.09F);
            float flapSpeed = entity.getCurrentPhase() == com.huige233.transcend.entity.boss.BossPhase.PHASE_4 ? 0.15F : 0.08F;
            float flapAmp   = entity.getCurrentPhase() == com.huige233.transcend.entity.boss.BossPhase.PHASE_4 ? 0.50F : 0.30F;
            float flap = Mth.sin(age * flapSpeed) * flapAmp;

            // 两层翅膀片：{ 左yRot, 右yRot, 宽, 高, alpha倍率 }
            // pivot 在模型侧面 x=±4，翅膀向外展开。
            // 模型身高约 26 单位（头8+身12+腿14），翅膀高度对齐躯干（约12单位）。
            // 宽度 4 单位 = 贴着模型侧面往外伸约 4 格模型单位，视觉上紧贴。
            float[][] sheets = {
                { 0.06F, -0.06F, 2.0F, 4.2F, 1.00F },  // 主翼
                { 0.10F, -0.10F, 1.5F, 3.3F, 0.50F },  // 内层
            };

            VertexConsumer vc = buf.getBuffer(TranscendRenderTypes.magicGlow(glowTexture));

            for (float[] sh : sheets) {
                float yL = sh[0] + flap, yR = sh[1] - flap;
                float w = sh[2], h = sh[3];
                float a = phaseAlpha * pulse * sh[4];

                // pivot Y=2 = 어깨 높이, x0=-1로 모델 내부에서 시작
                drawSheet(ps, vc, 2f, 2f, 0f,  yL,  0.02f, -2.0f, w, h, r, g, b, a);
                drawSheet(ps, vc,-2f, 2f, 0f,  yR, -0.02f, -w+2.0f, w, h, r, g, b, a);
            }
        }

        /** Draw a single rectangular wing sheet in model space. */
        private void drawSheet(PoseStack ps, VertexConsumer vc,
                               float pivotX, float pivotY, float pivotZ,
                               float yRot, float zRot,
                               float x0, float w, float h,
                               float r, float g, float b, float a) {
            ps.pushPose();
            ps.translate(pivotX, pivotY, pivotZ);
            ps.mulPose(com.mojang.math.Axis.YP.rotation(yRot));
            ps.mulPose(com.mojang.math.Axis.ZP.rotation(zRot));

            Matrix4f m = ps.last().pose();
            float x1 = x0 + w;
            float y0 = -h * 0.75f, y1 = h * 0.25f; // 위쪽 75%, 아래쪽 25%

            // Quad top-alpha to bottom-fade for feathered look
            float aTop = a, aBot = a * 0.15f;

            vc.vertex(m, x0, y0, 0).color(r,g,b,aTop).uv(0,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0,0,1).endVertex();
            vc.vertex(m, x0, y1, 0).color(r,g,b,aBot).uv(0,1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0,0,1).endVertex();
            vc.vertex(m, x1, y1, 0).color(r,g,b,aBot).uv(1,1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0,0,1).endVertex();
            vc.vertex(m, x1, y0, 0).color(r,g,b,aTop).uv(1,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0,0,1).endVertex();
            // back face
            vc.vertex(m, x1, y0, 0).color(r,g,b,aTop).uv(1,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0,0,-1).endVertex();
            vc.vertex(m, x1, y1, 0).color(r,g,b,aBot).uv(1,1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0,0,-1).endVertex();
            vc.vertex(m, x0, y1, 0).color(r,g,b,aBot).uv(0,1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0,0,-1).endVertex();
            vc.vertex(m, x0, y0, 0).color(r,g,b,aTop).uv(0,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0,0,-1).endVertex();

            ps.popPose();
        }
    }

}
