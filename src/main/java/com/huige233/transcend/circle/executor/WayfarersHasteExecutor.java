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

public class WayfarersHasteExecutor implements CircleFunctionExecutor {

    private static final int BUFF_DURATION_TICKS = 60;

    private static final int SLOW_FALLING_HEIGHT_OFFSET = 3;

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

        int tier = ctx.getTier().getLevel();
        int speedJumpAmplifier = tier >= CircleTier.MASTER.getLevel() ? 1 : 0;
        boolean enableSlowFalling = tier >= CircleTier.ARCHON.getLevel();
        BlockPos corePos = ctx.getCorePos();
        int slowFallingTriggerY = corePos.getY() + SLOW_FALLING_HEIGHT_OFFSET;

        for (Player player : players) {

            player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED,
                    BUFF_DURATION_TICKS,
                    speedJumpAmplifier,
                    true,
                    false,
                    true
            ));

            player.addEffect(new MobEffectInstance(
                    MobEffects.JUMP,
                    BUFF_DURATION_TICKS,
                    speedJumpAmplifier,
                    true,
                    false,
                    true
            ));

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

    }

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
