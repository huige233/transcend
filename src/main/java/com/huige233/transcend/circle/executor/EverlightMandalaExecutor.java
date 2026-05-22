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
 * 长明法环（Everlight Mandala）功能执行器（v1）。
 * <p>
 * 简化实现：不在世界中放置/清理光源方块，而是为半径内的玩家施加夜视效果，
 * 间接达到"驱散黑暗"的目标，避免方块层面的清理与同步开销。
 * <p>
 * T3 起作用半径在基础值上额外延伸（×1.25），T4 起再延伸（×1.5）。
 */
public class EverlightMandalaExecutor implements CircleFunctionExecutor {

    /** 夜视持续时间（tick）：覆盖 tick 间隔并避免画面闪烁。 */
    private static final int NIGHT_VISION_DURATION_TICKS = 60;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.INITIATE.getLevel();
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
        if (players.isEmpty()) {
            return;
        }

        for (Player player : players) {
            // ambient = true，使夜视的画面闪烁更柔和
            MobEffectInstance effect = new MobEffectInstance(
                    MobEffects.NIGHT_VISION,
                    NIGHT_VISION_DURATION_TICKS,
                    0,
                    true,
                    false,
                    true
            );
            player.addEffect(effect);
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        // 夜视效果将自行衰减；如需"立即变暗"可在此主动移除
    }

    /**
     * 在以核心方块为中心的范围内查找所有玩家。
     * 半径会根据等级做出额外加成：T3 ×1.25，T4 及以上 ×1.5。
     */
    private List<Player> getPlayersInRadius(CircleFunctionContext ctx) {
        double base = ctx.getBaseRadius();
        int tier = ctx.getTier().getLevel();
        double multiplier;
        if (tier >= CircleTier.ARCHON.getLevel()) {
            multiplier = 1.5;
        } else if (tier >= CircleTier.MASTER.getLevel()) {
            multiplier = 1.25;
        } else {
            multiplier = 1.0;
        }
        double r = base * multiplier;

        BlockPos pos = ctx.getCorePos();
        AABB area = new AABB(
                pos.getX() - r, pos.getY() - 2, pos.getZ() - r,
                pos.getX() + r, pos.getY() + 4, pos.getZ() + r
        );
        return ctx.getLevel().getEntitiesOfClass(Player.class, area);
    }
}
