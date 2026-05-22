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
 * 极光剧院（Aurora Theatre）功能执行器。
 * <p>
 * 每 600 tick（约 30 秒）对范围内的玩家施加"被启迪"状态：
 * <ul>
 *     <li>幸运 I 持续 400 tick。</li>
 *     <li>恢复 1 点饱食度（{@link Player#getFoodData()}）。</li>
 * </ul>
 * 维护开销极低，作为基础精神类增益的占位实现。
 */
public class AuroraTheatreExecutor implements CircleFunctionExecutor {

    /** 触发周期（tick）。 */
    private static final int TRIGGER_INTERVAL_TICKS = 600;
    /** 幸运效果持续时间（tick）。 */
    private static final int LUCK_DURATION_TICKS = 400;

    private int timer = 0;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.ADEPT.getLevel();
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
        if (timer < TRIGGER_INTERVAL_TICKS) {
            return;
        }
        timer = 0;

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
            // 恢复 1 点饱食度（saturation）
            player.getFoodData().eat(0, 1.0f);
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        timer = 0;
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
