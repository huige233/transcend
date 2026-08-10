package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 静寂边界法阵功能执行器。 */
public class QuietBoundaryExecutor implements CircleFunctionExecutor {

    private static final double PUSH_STRENGTH = 0.18;

    private static final double PUSH_VERTICAL = 0.05;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.ADEPT.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {

    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        if (ctx.getLevel() == null) {
            return;
        }

        double radius = ctx.getBaseRadius();
        List<Mob> mobs = ctx.getMobsInRadius(Mob.class, radius);
        if (mobs.isEmpty()) {
            return;
        }

        BlockPos core = ctx.getCorePos();
        double cx = core.getX() + 0.5;
        double cy = core.getY() + 0.5;
        double cz = core.getZ() + 0.5;

        for (Mob mob : mobs) {

            if (!(mob instanceof Enemy)) {
                continue;
            }

            double dx = mob.getX() - cx;
            double dz = mob.getZ() - cz;
            double distSqr = dx * dx + dz * dz;
            if (distSqr < 1.0E-4) {

                double angle = ctx.getLevel().getRandom().nextDouble() * Math.PI * 2.0;
                dx = Math.cos(angle);
                dz = Math.sin(angle);
            } else {
                double dist = Math.sqrt(distSqr);
                dx /= dist;
                dz /= dist;
            }

            Vec3 v = mob.getDeltaMovement();
            mob.setDeltaMovement(
                    v.x + dx * PUSH_STRENGTH,
                    v.y + PUSH_VERTICAL,
                    v.z + dz * PUSH_STRENGTH
            );
            mob.hurtMarked = true;
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {

    }
}
