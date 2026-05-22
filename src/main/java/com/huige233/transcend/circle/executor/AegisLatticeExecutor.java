package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 庇护方阵（Aegis Lattice）功能执行器。
 * <p>
 * 每 10 tick 扫描半径范围内的箭矢/抛射物：
 * 若其来源不是玩家（或没有归属），则销毁该实体。
 * 用于保护核心区域免受敌对箭塔、骷髅等远程攻击。
 */
public class AegisLatticeExecutor implements CircleFunctionExecutor {

    /** 扫描周期（tick）。 */
    private static final int SCAN_INTERVAL_TICKS = 10;

    private int timer = 0;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.MASTER.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        timer = 0;
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        if (ctx.getLevel() == null) {
            return;
        }

        timer += 20;
        if (timer < SCAN_INTERVAL_TICKS) {
            return;
        }
        timer = 0;

        double r = ctx.getBaseRadius();
        BlockPos pos = ctx.getCorePos();
        AABB aabb = new AABB(
                pos.getX() - r, pos.getY() - r, pos.getZ() - r,
                pos.getX() + r, pos.getY() + r, pos.getZ() + r
        );

        List<AbstractArrow> arrows = ctx.getLevel().getEntitiesOfClass(
                AbstractArrow.class,
                aabb,
                arrow -> arrow.getOwner() == null || !(arrow.getOwner() instanceof Player)
        );

        for (AbstractArrow arrow : arrows) {
            arrow.discard();
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        timer = 0;
    }
}
