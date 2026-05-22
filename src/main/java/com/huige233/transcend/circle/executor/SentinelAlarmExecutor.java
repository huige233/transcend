package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;

import java.util.List;

/**
 * 哨戒警报（Sentinel Alarm）功能执行器。
 * <p>
 * 每 40 tick 扫描半径范围内的敌对生物（{@link Monster}）：
 * <ul>
 *     <li>若发现敌对生物，在核心位置播放音符方块钟声进行警报。</li>
 *     <li>T3+ 时额外为被发现的敌对生物施加发光 60 tick，方便玩家定位。</li>
 * </ul>
 */
public class SentinelAlarmExecutor implements CircleFunctionExecutor {

    /** 扫描周期（tick）。 */
    private static final int SCAN_INTERVAL_TICKS = 40;
    /** 发光效果持续时间（tick）。 */
    private static final int GLOWING_DURATION_TICKS = 60;

    private int timer = 0;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.INITIATE.getLevel();
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

        double radius = ctx.getBaseRadius();
        List<Monster> hostiles = ctx.getMobsInRadius(Monster.class, radius);
        if (hostiles.isEmpty()) {
            return;
        }

        BlockPos core = ctx.getCorePos();
        // 警报钟声
        ctx.getLevel().playSound(
                null,
                core,
                SoundEvents.NOTE_BLOCK_BELL.value(),
                SoundSource.BLOCKS,
                1.0f,
                1.0f
        );

        // T3+ 标记发光
        if (ctx.getTier().getLevel() >= CircleTier.MASTER.getLevel()) {
            for (Monster m : hostiles) {
                m.addEffect(new MobEffectInstance(
                        MobEffects.GLOWING,
                        GLOWING_DURATION_TICKS,
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
        timer = 0;
    }
}
