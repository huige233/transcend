package com.huige233.transcend.client;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.client.renderer.TranscendRenderTypes;
import com.huige233.transcend.network.PhaseShieldClientState;
import com.huige233.transcend.tech.shield.PhaseShieldStatus;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


/** 根据同步的护盾状态与电量，在玩家周围绘制带扫描光带和电量变色的半透明护盾外壳。 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PhaseShieldRenderHandler {
    static final int LATITUDES = 24;
    static final int LONGITUDES = 48;

    private PhaseShieldRenderHandler() { }

    static int[] colorFor(float charge, PhaseShieldStatus status) {
        if (status == PhaseShieldStatus.EMPTY) return new int[]{255, 145, 20};
        float value = Math.max(0.0F, Math.min(1.0F, charge));
        
        if (value <= 0.08F) return new int[]{255, 145, 20};
        if (value <= 0.25F) return new int[]{255, 45, 35};
        if (value <= 0.40F) {
            float t = (value - 0.25F) / 0.15F;
            return new int[]{255, (int) (45.0F + 100.0F * t), (int) (35.0F + 125.0F * t)};
        }
        float t = (value - 0.40F) / 0.60F;
        return new int[]{(int) (255.0F - 135.0F * t),
                (int) (145.0F + 80.0F * t), (int) (160.0F + 95.0F * t)};
    }
    @SubscribeEvent
    public static void render(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        PhaseShieldStatus status = PhaseShieldClientState.status(player.getUUID());
        if (!player.isAlive() || player.isSpectator() || player.isInvisible()
                || status != PhaseShieldStatus.ACTIVE) return;
        float charge = PhaseShieldClientState.charge(player.getUUID());
        if (charge <= 0.001F) return;

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        try {
            pose.translate(0.0D, player.getBbHeight() * 0.5D, 0.0D);
            float radius = Math.max(0.65F, player.getBbWidth() * 1.18F + 0.12F);
            float height = player.getBbHeight() * 0.62F + 0.28F;
            VertexConsumer vertices = event.getMultiBufferSource().getBuffer(TranscendRenderTypes.phaseShield());
            int[] color = colorFor(charge, PhaseShieldClientState.status(player.getUUID()));
            drawShell(vertices, pose.last(), radius, height,
                    color[0], color[1], color[2], player.tickCount + event.getPartialTick());
        } finally {
            pose.popPose();
        }
    }

    static void drawShell(VertexConsumer vertices, PoseStack.Pose pose,
                          float radius, float height, int red, int green, int blue, float time) {
        for (int latitude = 0; latitude < LATITUDES; latitude++) {
            double lower = -Math.PI * 0.5 + Math.PI * latitude / LATITUDES;
            double upper = -Math.PI * 0.5 + Math.PI * (latitude + 1) / LATITUDES;
            for (int longitude = 0; longitude < LONGITUDES; longitude++) {
                double left = Math.PI * 2.0 * longitude / LONGITUDES;
                double right = Math.PI * 2.0 * (longitude + 1) / LONGITUDES;
                shellVertex(vertices, pose, radius, height, lower, left, red, green, blue, time);
                shellVertex(vertices, pose, radius, height, upper, left, red, green, blue, time);
                shellVertex(vertices, pose, radius, height, upper, right, red, green, blue, time);
                shellVertex(vertices, pose, radius, height, lower, right, red, green, blue, time);
            }
        }
    }

    private static void shellVertex(VertexConsumer vertices, PoseStack.Pose pose,
                                    float radius, float height, double latitude, double longitude,
                                    int red, int green, int blue, float time) {
        float y = (float) Math.sin(latitude);
        float ring = (float) Math.cos(latitude);
        float scanHeight = (float) Math.sin(time * 0.035F);
        float band = Math.max(0.0F, 1.0F - Math.abs(y - scanHeight) / 0.16F);
        int alpha = 35 + (int) (band * 65.0F);
        
        vertices.vertex(pose.pose(), (float) Math.cos(longitude) * radius * ring,
                        y * height, (float) Math.sin(longitude) * radius * ring)
                .color(red, green, blue, alpha).endVertex();
    }
}
