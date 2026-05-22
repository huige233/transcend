package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 陷阱织网（Trapweaver Relay）功能执行器（v1）。
 * <p>
 * 每 20 tick 给半径内所有敌对生物附加缓慢 II 与虚弱 I 各 40t，模拟"陷阱减益"。
 */
public class TrapweaverRelayExecutor implements CircleFunctionExecutor {

    private static final int APPLY_INTERVAL_TICKS = 20;
    private static final int DEBUFF_DURATION = 40;

    /** 每个核心位置上次施加减益的世界时间。 */
    private final Map<BlockPos, Long> lastApply = new HashMap<>();

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.ADEPT.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        lastApply.remove(ctx.getCorePos().immutable());
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        BlockPos selfPos = ctx.getCorePos().immutable();
        long gameTime = ctx.getLevel().getGameTime();
        Long last = lastApply.get(selfPos);
        if (last != null && gameTime - last < APPLY_INTERVAL_TICKS) return;

        double radius = ctx.getBaseRadius();
        List<Monster> hostiles = ctx.getMobsInRadius(Monster.class, radius);
        if (hostiles.isEmpty()) {
            lastApply.put(selfPos, gameTime);
            return;
        }

        for (Monster mob : hostiles) {
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DEBUFF_DURATION, 1, false, true));
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DEBUFF_DURATION, 0, false, true));
        }

        lastApply.put(selfPos, gameTime);
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        lastApply.remove(ctx.getCorePos().immutable());
    }
}
