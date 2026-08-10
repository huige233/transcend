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

/** 永恒光曼陀罗法阵功能执行器。 */
public class EverlightMandalaExecutor implements CircleFunctionExecutor {

    private static final int NIGHT_VISION_DURATION_TICKS = 60;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.INITIATE.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {

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

    }

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
