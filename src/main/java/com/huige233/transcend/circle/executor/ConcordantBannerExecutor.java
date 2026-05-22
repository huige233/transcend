package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;

/**
 * 共鸣旌旗（Concordant Banner）功能执行器。
 * <p>
 * 周期性向半径范围内的玩家广播速度 I + 生命恢复 I 60t。
 * 此外，给法环主人附加 60t 发光效果用作领地视觉标记。
 */
public class ConcordantBannerExecutor implements CircleFunctionExecutor {

    private static final int BUFF_DURATION = 60;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.MASTER.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        // 无状态
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        double radius = ctx.getBaseRadius();
        List<Player> players = ctx.getMobsInRadius(Player.class, radius);
        if (players.isEmpty()) return;

        UUID owner = ctx.getOwner();
        for (Player player : players) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, BUFF_DURATION, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, BUFF_DURATION, 0, false, true));

            if (owner != null && owner.equals(player.getUUID())) {
                player.addEffect(new MobEffectInstance(MobEffects.GLOWING, BUFF_DURATION, 0, false, true));
            }
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        // 无状态
    }
}
