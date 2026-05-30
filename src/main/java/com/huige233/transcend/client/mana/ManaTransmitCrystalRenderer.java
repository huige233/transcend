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

/**
 * R62 → R65 重写：魔力传输水晶 Beam 渲染器（向量法，无旋转矩阵依赖）。
 *
 * <p>响应玩家 R62 反馈"激光不对啊 不应该是链接水晶之间的吗"：
 * 旧实现用 yaw/pitch 旋转 PoseStack 让 +X 对准 partner，pitch 符号搞反 → 垂直方向 beam
 * 朝错误方向射出（partner 在上 → beam 朝下，partner 在下 → beam 朝上）。两个水晶不在
 * 同一水平面时就完全错位。
 *
 * <p>R65 重写直接用世界坐标向量构造 beam quad：
 * <ol>
 *   <li>计算 self → partner 单位方向向量 D</li>
 *   <li>取与 D 垂直的两个轴 perp1, perp2（cross-product 推导）</li>
 *   <li>quad 1 顶点：start ± perp1*halfWidth, end ± perp1*halfWidth</li>
 *   <li>quad 2 顶点：start ± perp2*halfWidth, end ± perp2*halfWidth</li>
 * </ol>
 *
 * <p>所有顶点直接相对 BE block local 坐标（PoseStack 默认状态下），无旋转矩阵参与，
 * 不会因坐标系约定（Minecraft 左/右手 / Y 上 / Z 南）出现符号错误。
 */
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
            // self 中心相对 BE 局部坐标 = (0.5, 0.5, 0.5)
            // partner 中心相对 BE 局部坐标 = (partner - self) + (0.5, 0.5, 0.5)
            Vec3 start = new Vec3(0.5, 0.5, 0.5);
            Vec3 end = new Vec3(
                    partner.getX() - selfPos.getX() + 0.5,
                    partner.getY() - selfPos.getY() + 0.5,
                    partner.getZ() - selfPos.getZ() + 0.5);

            Vec3 dir = end.subtract(start);
            double length = dir.length();
            if (length < 0.5) continue;
            Vec3 dirN = dir.normalize();

            // 取与 dir 垂直的两个轴。world up 与 dir 不平行时用 dir × up；
            // 否则（dir 接近垂直）改用 +X 作参考避免 cross 退化为零向量。
            Vec3 worldUp = new Vec3(0, 1, 0);
            Vec3 perp1;
            if (Math.abs(dirN.dot(worldUp)) > 0.99) {
                perp1 = dirN.cross(new Vec3(1, 0, 0)).normalize();
            } else {
                perp1 = dirN.cross(worldUp).normalize();
            }
            Vec3 perp2 = dirN.cross(perp1).normalize();

            // 双层十字交叉给 beam 体积感：外层包络 + 内层核心
            drawBeamQuad(consumer, pose, start, end, perp1, halfWidth * 1.6F, r, g, b, baseAlpha * 0.5F);
            drawBeamQuad(consumer, pose, start, end, perp2, halfWidth * 1.6F, r, g, b, baseAlpha * 0.5F);
            drawBeamQuad(consumer, pose, start, end, perp1, halfWidth, r, g, b, baseAlpha);
            drawBeamQuad(consumer, pose, start, end, perp2, halfWidth, r, g, b, baseAlpha);
        }
    }

    /**
     * 在 start → end 之间绘制一个矩形 quad，宽度沿 perp 方向 ±halfWidth。
     * 4 顶点逆时针：start-perp, end-perp, end+perp, start+perp.
     */
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

    /**
     * Beam 可能延伸到 64 格外，超出 BE 默认渲染剔除距离时会被剔除。
     * 启用 offscreen render 让 beam 即使 BE 主体在视锥外也能保持显示。
     */
    @Override
    public boolean shouldRenderOffScreen(ManaTransmitCrystalBlockEntity be) {
        return true;
    }

    /**
     * BE 视野距离：默认 64 格。本 beam 起点 BE 距对端最大 64 格 ⇒ 玩家在中点观察时
     * 任意一端 BE 都可能距玩家 ~32 格，仍在默认范围内。但保险起见提到 96。
     */
    @Override
    public int getViewDistance() {
        return 96;
    }
}
