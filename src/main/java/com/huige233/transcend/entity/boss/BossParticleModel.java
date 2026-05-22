package com.huige233.transcend.entity.boss;

import com.huige233.transcend.client.renderer.ShaderSpellRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Boss身体粒子模型 — 仅保留最小化的身体光效，
 * 不画法阵(addCircle)、不画持续shader环。
 */
public class BossParticleModel {

    public static void renderWardenBody(Level level, double x, double y, double z,
                                        float r, float g, float b, int tick) {
        if (!level.isClientSide) return;
        // Warden: 无持续特效，靠实际模型/贴图表现
    }

    public static void renderWeaverBody(Level level, double x, double y, double z, int tick) {
        if (!level.isClientSide) return;
        // Weaver: 无持续特效
    }

    public static void renderAvatarBody(Level level, double x, double y, double z,
                                        float r, float g, float b, float r2, float g2, float b2,
                                        int tick, boolean finalPhase) {
        if (!level.isClientSide) return;
        // Avatar: 仅在Phase4时保留陨石光柱（标志性特效）
        if (finalPhase && tick % 8 == 0) {
            Vec3 center = new Vec3(x, y, z);
            double a = tick * 0.10;
            Vec3 meteorFrom = center.add(Math.cos(a) * 3.5, 8.0, Math.sin(a) * 3.5);
            Vec3 meteorTo = center.add(0.0, 0.25, 0.0);
            ShaderSpellRenderer.addSpellEffect(meteorFrom, meteorTo,
                    Math.min(1.0F, r + 0.35F), Math.min(1.0F, g + 0.18F), Math.min(1.0F, b + 0.18F),
                    22, "meteor");
        }
    }
}
