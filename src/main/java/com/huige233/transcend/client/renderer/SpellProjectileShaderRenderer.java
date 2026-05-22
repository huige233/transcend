package com.huige233.transcend.client.renderer;

import com.huige233.transcend.spell.SpellCarrier;
import com.huige233.transcend.spell.SpellElement;
import com.huige233.transcend.spell.SpellProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class SpellProjectileShaderRenderer extends EntityRenderer<SpellProjectile> {

    private static final ResourceLocation GLOW_TEX = new ResourceLocation("transcend", "textures/entity/elemental_warden_glow.png");

    public SpellProjectileShaderRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(@NotNull SpellProjectile entity, float yaw, float partialTick,
                       @NotNull PoseStack ps, @NotNull MultiBufferSource buf, int light) {
        SpellElement element = entity.getElement();
        SpellCarrier carrier = entity.getCarrier();
        if (element == null || carrier == null) return;

        float r = element.particleR, g = element.particleG, b = element.particleB;
        float age = entity.tickCount + partialTick;

        ps.pushPose();
        ps.translate(0, 0.15, 0);

        VertexConsumer vc = buf.getBuffer(TranscendRenderTypes.magicGlow(GLOW_TEX));

        switch (carrier) {
            case ORB, VORTEX, TRAP, RAIN -> renderOrbShape(ps, vc, r, g, b, age, carrier == SpellCarrier.VORTEX);
            case ARROW -> renderArrowShape(ps, vc, r, g, b, age, entity);
            default -> renderOrbShape(ps, vc, r, g, b, age, false);
        }

        ps.popPose();
    }

    private void renderOrbShape(PoseStack ps, VertexConsumer vc, float r, float g, float b,
                                 float age, boolean spinning) {
        float pulse = 0.7F + 0.3F * Mth.sin(age * 0.3F);
        float size = 0.25F * pulse;

        if (spinning) {
            ps.mulPose(Axis.YP.rotationDegrees(age * 12));
        }

        // Core billboard quad
        renderBillboardQuad(ps, vc, r, g, b, 0.8F * pulse, size);

        // Outer glow (larger, more transparent)
        renderBillboardQuad(ps, vc,
                Math.min(1, r + 0.3F), Math.min(1, g + 0.3F), Math.min(1, b + 0.3F),
                0.3F * pulse, size * 2.0F);

        // Rotating ring
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(age * 8));
        Matrix4f mat = ps.last().pose();
        int segs = 12;
        float ringR = size * 1.8F;
        float ringW = 0.02F;
        for (int i = 0; i < segs; i++) {
            float a1 = (float)(Math.PI * 2 * i / segs);
            float a2 = (float)(Math.PI * 2 * (i + 1) / segs);
            float segAlpha = 0.4F * pulse * (0.5F + 0.5F * Mth.sin(a1 * 2 + age * 0.2F));

            vc.vertex(mat, Mth.cos(a1) * ringR, Mth.sin(a1) * ringR, -ringW)
                    .color(r, g, b, segAlpha).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(0xF000F0).normal(0, 0, 1).endVertex();
            vc.vertex(mat, Mth.cos(a1) * ringR, Mth.sin(a1) * ringR, ringW)
                    .color(r, g, b, segAlpha).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(0xF000F0).normal(0, 0, 1).endVertex();
            vc.vertex(mat, Mth.cos(a2) * ringR, Mth.sin(a2) * ringR, ringW)
                    .color(r, g, b, segAlpha).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(0xF000F0).normal(0, 0, 1).endVertex();
            vc.vertex(mat, Mth.cos(a2) * ringR, Mth.sin(a2) * ringR, -ringW)
                    .color(r, g, b, segAlpha).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(0xF000F0).normal(0, 0, 1).endVertex();
        }
        ps.popPose();
    }

    private void renderArrowShape(PoseStack ps, VertexConsumer vc, float r, float g, float b,
                                   float age, SpellProjectile entity) {
        float pulse = 0.8F + 0.2F * Mth.sin(age * 0.5F);

        // Core point
        renderBillboardQuad(ps, vc, r, g, b, 0.9F * pulse, 0.15F);

        // Trail quads behind the projectile
        net.minecraft.world.phys.Vec3 vel = entity.getDeltaMovement();
        double speed = vel.length();
        if (speed > 0.01) {
            ps.pushPose();
            float yawAngle = (float) Math.atan2(vel.x, vel.z);
            float pitchAngle = (float) -Math.atan2(vel.y, Math.sqrt(vel.x * vel.x + vel.z * vel.z));
            ps.mulPose(Axis.YP.rotation(yawAngle));
            ps.mulPose(Axis.XP.rotation(pitchAngle));

            Matrix4f mat = ps.last().pose();
            for (int i = 1; i <= 4; i++) {
                float t = i * 0.15F;
                float trailAlpha = (1.0F - t * 1.5F) * 0.5F * pulse;
                float trailW = 0.08F - i * 0.012F;
                if (trailAlpha <= 0) break;

                vc.vertex(mat, 0, -trailW, -t).color(r, g, b, trailAlpha)
                        .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(0xF000F0).normal(0, 1, 0).endVertex();
                vc.vertex(mat, 0, trailW, -t).color(r, g, b, trailAlpha)
                        .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(0xF000F0).normal(0, 1, 0).endVertex();
                vc.vertex(mat, 0, trailW * 0.5F, -t - 0.15F).color(r * 0.5F, g * 0.5F, b * 0.5F, trailAlpha * 0.3F)
                        .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(0xF000F0).normal(0, 1, 0).endVertex();
                vc.vertex(mat, 0, -trailW * 0.5F, -t - 0.15F).color(r * 0.5F, g * 0.5F, b * 0.5F, trailAlpha * 0.3F)
                        .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(0xF000F0).normal(0, 1, 0).endVertex();
            }
            ps.popPose();
        }
    }

    private void renderBillboardQuad(PoseStack ps, VertexConsumer vc,
                                      float r, float g, float b, float alpha, float size) {
        ps.pushPose();
        ps.mulPose(this.entityRenderDispatcher.cameraOrientation());
        Matrix4f mat = ps.last().pose();

        vc.vertex(mat, -size, -size, 0).color(r, g, b, alpha)
                .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0).normal(0, 0, 1).endVertex();
        vc.vertex(mat, size, -size, 0).color(r, g, b, alpha)
                .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0).normal(0, 0, 1).endVertex();
        vc.vertex(mat, size, size, 0).color(r, g, b, alpha)
                .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0).normal(0, 0, 1).endVertex();
        vc.vertex(mat, -size, size, 0).color(r, g, b, alpha)
                .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0).normal(0, 0, 1).endVertex();

        ps.popPose();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SpellProjectile entity) {
        return GLOW_TEX;
    }
}
