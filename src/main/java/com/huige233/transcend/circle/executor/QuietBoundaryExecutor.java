package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 静境结界（Quiet Boundary）功能执行器。
 * <p>
 * 在范围内对敌对生物施加"温和的排斥力"：每次 tick 调用都将范围内的敌对怪物
 * 沿"远离核心方块"的方向施加一个小幅速度，从而使它们倾向于远离结界中心。
 *
 * <p>v1 不直接拦截怪物生成（涉及 Forge 事件钩子），后续可通过监听
 * {@code MobSpawnEvent} 并查询活跃法环来实现完整的"抑制生成"功能。
 */
public class QuietBoundaryExecutor implements CircleFunctionExecutor {

    /** 推力大小（每次施加的水平速度增量）。 */
    private static final double PUSH_STRENGTH = 0.18;
    /** 额外向上分量，避免被原地卡住。 */
    private static final double PUSH_VERTICAL = 0.05;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.ADEPT.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        // 无需特殊处理
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
            // 仅推开敌对生物：保留村民、动物等不受影响
            if (!(mob instanceof Enemy)) {
                continue;
            }

            double dx = mob.getX() - cx;
            double dz = mob.getZ() - cz;
            double distSqr = dx * dx + dz * dz;
            if (distSqr < 1.0E-4) {
                // 与核心几乎重合时随机给一个方向，避免 NaN
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
            mob.hurtMarked = true; // 强制同步速度到客户端
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        // 无需特殊处理
    }
}
