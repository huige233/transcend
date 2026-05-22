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
 * 风行法环（Wayfarer's Haste）功能执行器。
 * <p>
 * 为半径内的玩家持续施加移动速度与跳跃提升效果：
 * <ul>
 *     <li>T1/T2：速度 I + 跳跃提升 I</li>
 *     <li>T3/T4/T5：速度 II + 跳跃提升 II</li>
 *     <li>T4+：当玩家位于核心方块上方较高处（{@code y > coreY + 3}）时，额外施加缓降。</li>
 * </ul>
 */
public class WayfarersHasteExecutor implements CircleFunctionExecutor {

    /** 通用 buff 持续时间（tick）。 */
    private static final int BUFF_DURATION_TICKS = 60;
    /** 高度阈值：玩家高度高于核心 y + 此偏移时施加缓降。 */
    private static final int SLOW_FALLING_HEIGHT_OFFSET = 3;

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

        int tier = ctx.getTier().getLevel();
        int speedJumpAmplifier = tier >= CircleTier.MASTER.getLevel() ? 1 : 0;
        boolean enableSlowFalling = tier >= CircleTier.ARCHON.getLevel();
        BlockPos corePos = ctx.getCorePos();
        int slowFallingTriggerY = corePos.getY() + SLOW_FALLING_HEIGHT_OFFSET;

        for (Player player : players) {
            // 速度
            player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED,
                    BUFF_DURATION_TICKS,
                    speedJumpAmplifier,
                    true,
                    false,
                    true
            ));
            // 跳跃提升
            player.addEffect(new MobEffectInstance(
                    MobEffects.JUMP,
                    BUFF_DURATION_TICKS,
                    speedJumpAmplifier,
                    true,
                    false,
                    true
            ));

            // T4+ 高空缓降
            if (enableSlowFalling && player.getY() > slowFallingTriggerY) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.SLOW_FALLING,
                        BUFF_DURATION_TICKS,
                        0,
                        true,
                        false,
                        true
                ));
            }
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        // 效果自然消散
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
