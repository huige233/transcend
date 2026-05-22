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
 * 护界法环（Warding Aegis）功能执行器。
 * <p>
 * 持续为半径内的玩家施加抗性提升效果。等级越高，等级越高的抗性持续时间不变（每 20tick 刷新），
 * T3 起额外叠加抗性 II。该法环不主动攻击，仅强化区域内友方生存能力。
 */
public class WardingAegisExecutor implements CircleFunctionExecutor {

    /** 抗性效果持续时间（tick）：略大于 tick 间隔以保证不会断档。 */
    private static final int EFFECT_DURATION_TICKS = 40;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.INITIATE.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        // 无需播放粒子或音效（保留给客户端层）
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        if (ctx.getLevel() == null) {
            return;
        }

        List<Player> players = getPlayersInRadius(ctx);
        if (players.isEmpty()) {
            return;
        }

        int tier = ctx.getTier().getLevel();
        // 默认抗性 I（20% 减伤）；T3+ 升级为抗性 II（40% 减伤）
        int amplifier = tier >= CircleTier.MASTER.getLevel() ? 1 : 0;

        for (Player player : players) {
            // ambient = false, visible = true, showIcon = true
            MobEffectInstance effect = new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE,
                    EFFECT_DURATION_TICKS,
                    amplifier,
                    true,
                    false,
                    true
            );
            player.addEffect(effect);
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        // 抗性效果会随其自身持续时间自然消退，无需主动清理
    }

    /**
     * 在以核心方块为中心、{@link CircleFunctionContext#getBaseRadius()} 为水平半径的
     * 立方体范围内查找所有玩家。
     */
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
