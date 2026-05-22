package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 制图者之眼（Cartographer's Eye）功能执行器。
 * <p>
 * 周期性向半径内的玩家施加夜视，模拟"提升测绘视野"。
 * 在 MASTER 及以上层级时额外施加海豚的恩惠，加速探索。
 */
public class CartographersEyeExecutor implements CircleFunctionExecutor {

    private static final int APPLY_INTERVAL_TICKS = 200;
    private static final int NIGHT_VISION_DURATION = 400;
    private static final int DOLPHINS_GRACE_DURATION = 200;

    /** 每个核心位置上次施加 buff 的世界时间。 */
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
        List<Player> players = ctx.getMobsInRadius(Player.class, radius);
        if (players.isEmpty()) {
            lastApply.put(selfPos, gameTime);
            return;
        }

        boolean t3Plus = ctx.getTier().getLevel() >= CircleTier.MASTER.getLevel();
        for (Player player : players) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION, 0, false, true));
            if (t3Plus) {
                player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, DOLPHINS_GRACE_DURATION, 0, false, true));
            }
        }

        lastApply.put(selfPos, gameTime);
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        lastApply.remove(ctx.getCorePos().immutable());
    }
}
