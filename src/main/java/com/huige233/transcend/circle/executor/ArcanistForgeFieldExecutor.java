package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 奥术锻场（Arcanist's Forge Field）功能执行器。
 * <p>
 * v1 实现：对半径范围内玩家持续施加幸运 I 60 tick，作为附魔加成的占位实现。
 *
 * <p>TODO（后续版本）：
 * <ul>
 *     <li>真正的附魔台/铁砧附魔几率与代价修正。</li>
 *     <li>支持稀有附魔（如灵魂疾行）的解锁。</li>
 * </ul>
 */
public class ArcanistForgeFieldExecutor implements CircleFunctionExecutor {

    /** 幸运效果持续时间（tick）。 */
    private static final int LUCK_DURATION_TICKS = 60;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.MASTER.getLevel();
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
        List<Player> players = getPlayersInRadius(ctx);
        for (Player player : players) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.LUCK,
                    LUCK_DURATION_TICKS,
                    0,
                    true,
                    false,
                    true
            ));
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        // 效果自然消散
    }

    /** 在以核心方块为中心范围内查找所有玩家。 */
    private List<Player> getPlayersInRadius(CircleFunctionContext ctx) {
        double r = ctx.getBaseRadius();
        BlockPos pos = ctx.getCorePos();
        AABB area = new AABB(
                pos.getX() - r, pos.getY() - 2, pos.getZ() - r,
                pos.getX() + r, pos.getY() + 4, pos.getZ() + r
        );
        return ctx.getLevel().getEntitiesOfClass(Player.class, area);
    }
}
