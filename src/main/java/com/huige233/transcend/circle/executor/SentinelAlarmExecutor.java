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

public class SentinelAlarmExecutor implements CircleFunctionExecutor {

    private static final int SCAN_INTERVAL_TICKS = 40;

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

        ctx.getLevel().playSound(
                null,
                core,
                SoundEvents.NOTE_BLOCK_BELL.value(),
                SoundSource.BLOCKS,
                1.0f,
                1.0f
        );

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
