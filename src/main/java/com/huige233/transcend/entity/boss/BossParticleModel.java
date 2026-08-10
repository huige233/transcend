package com.huige233.transcend.entity.boss;

import com.huige233.transcend.visual.ServerVisualBroadcaster;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** 首领粒子效果配置类。 */
public class BossParticleModel {

    public static void renderWardenBody(ServerLevel level, double x, double y, double z,
                                        float r, float g, float b, int tick) {

    }

    public static void renderWeaverBody(ServerLevel level, double x, double y, double z, int tick) {

    }

    public static void renderAvatarBody(ServerLevel level, double x, double y, double z,
                                        float r, float g, float b, float r2, float g2, float b2,
                                        int tick, boolean finalPhase) {

        if (finalPhase && tick % 8 == 0) {
            Vec3 center = new Vec3(x, y, z);
            double a = tick * 0.10;
            Vec3 meteorFrom = center.add(Math.cos(a) * 3.5, 8.0, Math.sin(a) * 3.5);
            Vec3 meteorTo = center.add(0.0, 0.25, 0.0);
            ServerVisualBroadcaster.beam(level, meteorFrom, meteorTo,
                    Math.min(1.0F, r + 0.35F), Math.min(1.0F, g + 0.18F), Math.min(1.0F, b + 0.18F),
                    22, "meteor");
        }
    }
}
