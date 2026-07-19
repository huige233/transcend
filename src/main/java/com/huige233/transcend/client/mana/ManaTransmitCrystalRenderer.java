package com.huige233.transcend.client.mana;

import com.huige233.transcend.block.mana.ManaTransmitCrystalBlockEntity;
import com.huige233.transcend.client.renderer.TranscendRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class ManaTransmitCrystalRenderer implements BlockEntityRenderer<ManaTransmitCrystalBlockEntity> {

    public ManaTransmitCrystalRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(ManaTransmitCrystalBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        java.util.List<BlockPos> partners = be.getPartners();
        if (partners.isEmpty()) return;

        Level level = be.getLevel();
        if (level == null) return;

        long gameTime = level.getGameTime();
        float t = gameTime + partialTick;
        float pulse = (float) (Math.sin(t * 0.12) * 0.5 + 0.5);
        boolean active = be.isTransferActive();

        float r, g, b, baseAlpha, halfWidth;
        if (active) {
            r = 0.40F;
            g = 0.85F + 0.15F * pulse;
            b = 1.00F;
            baseAlpha = 0.70F + 0.30F * pulse;
            halfWidth = 0.05F + 0.025F * pulse;
        } else {
            r = 0.50F;
            g = 0.60F;
            b = 0.80F;
            baseAlpha = 0.25F + 0.15F * pulse;
            halfWidth = 0.04F;
        }

        VertexConsumer consumer = buffer.getBuffer(TranscendRenderTypes.manaBeam());
        Matrix4f pose = poseStack.last().pose();

        BlockPos selfPos = be.getBlockPos();

        for (BlockPos partner : partners) {

            Vec3 start = new Vec3(0.5, 0.5, 0.5);
            Vec3 end = new Vec3(
                    partner.getX() - selfPos.getX() + 0.5,
                    partner.getY() - selfPos.getY() + 0.5,
                    partner.getZ() - selfPos.getZ() + 0.5);

            Vec3 dir = end.subtract(start);
            double length = dir.length();
            if (length < 0.5) continue;
            Vec3 dirN = dir.normalize();

            Vec3 worldUp = new Vec3(0, 1, 0);
            Vec3 perp1;
            if (Math.abs(dirN.dot(worldUp)) > 0.99) {
                perp1 = dirN.cross(new Vec3(1, 0, 0)).normalize();
            } else {
                perp1 = dirN.cross(worldUp).normalize();
            }
            Vec3 perp2 = dirN.cross(perp1).normalize();

            drawBeamQuad(consumer, pose, start, end, perp1, halfWidth * 1.6F, r, g, b, baseAlpha * 0.5F);
            drawBeamQuad(consumer, pose, start, end, perp2, halfWidth * 1.6F, r, g, b, baseAlpha * 0.5F);
            drawBeamQuad(consumer, pose, start, end, perp1, halfWidth, r, g, b, baseAlpha);
            drawBeamQuad(consumer, pose, start, end, perp2, halfWidth, r, g, b, baseAlpha);
        }
    }

    private static void drawBeamQuad(VertexConsumer consumer, Matrix4f pose,
                                      Vec3 start, Vec3 end, Vec3 perp, float halfWidth,
                                      float r, float g, float b, float a) {
        if (a <= 0.001F) return;

        Vec3 offset = perp.scale(halfWidth);
        Vec3 v1 = start.subtract(offset);
        Vec3 v2 = end.subtract(offset);
        Vec3 v3 = end.add(offset);
        Vec3 v4 = start.add(offset);

        consumer.vertex(pose, (float) v1.x, (float) v1.y, (float) v1.z).color(r, g, b, a).endVertex();
        consumer.vertex(pose, (float) v2.x, (float) v2.y, (float) v2.z).color(r, g, b, a).endVertex();
        consumer.vertex(pose, (float) v3.x, (float) v3.y, (float) v3.z).color(r, g, b, a).endVertex();
        consumer.vertex(pose, (float) v4.x, (float) v4.y, (float) v4.z).color(r, g, b, a).endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(ManaTransmitCrystalBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
