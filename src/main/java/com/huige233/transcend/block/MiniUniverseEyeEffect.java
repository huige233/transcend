package com.huige233.transcend.block;

import com.huige233.transcend.particle.TranscendDustParticleOptions;
import com.huige233.transcend.particle.TranscendGlitterParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;


/** 按游戏时间在微型宇宙发电机上方生成旋转星环与中心光点粒子。 */
final class MiniUniverseEyeEffect {
    private MiniUniverseEyeEffect() {}
    static void spawn(Level level, BlockPos pos, BlockState state, long gameTime) {
        double t = gameTime * Math.PI * 2.0 / 80.0;
        double cx = pos.getX() + .5, cy = pos.getY() + 1.12, cz = pos.getZ() + .5;
        for (int ring = 0; ring < 3; ring++) {
            double radius = .22 + ring * .13;
            double tilt = .35 + ring * .28;
            double phase = t * (ring == 1 ? -1.0 : 1.0) + ring * 2.1;
            for (int star = 0; star < 8; star++) {
                double a = phase + star * Math.PI / 4.0;
                level.addParticle(new TranscendGlitterParticleOptions(
                        new Vector3f(.22f + ring * .12f, .50f + ring * .12f, 1f),
                        .55f + ring * .12f, 12, true),
                        cx + Math.cos(a) * radius, cy + Math.sin(a) * radius * tilt,
                        cz + Math.sin(a) * radius, -Math.sin(a) * .008, .002, Math.cos(a) * .008);
            }
        }
        level.addParticle(new TranscendGlitterParticleOptions(
                new Vector3f(.65f, .90f, 1f), 1.45f, 14, true), cx, cy, cz, 0, .003, 0);
        if (gameTime % 4 == 0) {
            double a = t * 1.7;
            level.addParticle(new TranscendDustParticleOptions(
                    new Vector3f(.30f, .65f, 1f), .72f, 18, true),
                    cx + Math.cos(a) * .12, cy + .36, cz + Math.sin(a) * .12, 0, .012, 0);
        }
    }
}
