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

/** 翠绿复苏法阵功能执行器。 */
public class VerdantRestorationExecutor implements CircleFunctionExecutor {

    private static final int REGEN_DURATION_TICKS = 60;

    private static final int CLEANSE_INTERVAL_TICKS = 600;

    private int cleanseTimer = 0;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.ADEPT.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        cleanseTimer = 0;
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        if (ctx.getLevel() == null) {
            return;
        }

        List<Player> players = getPlayersInRadius(ctx);
        if (players.isEmpty()) {

            cleanseTimer = Math.min(cleanseTimer + 20, CLEANSE_INTERVAL_TICKS);
            return;
        }

        int tier = ctx.getTier().getLevel();
        int regenAmplifier = tier >= CircleTier.ARCHON.getLevel() ? 1 : 0;
        boolean cleanseWither = tier >= CircleTier.PRIMORDIAL.getLevel();

        for (Player player : players) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.REGENERATION,
                    REGEN_DURATION_TICKS,
                    regenAmplifier,
                    true,
                    false,
                    true
            ));
        }

        cleanseTimer += 20;
        if (cleanseTimer >= CLEANSE_INTERVAL_TICKS) {
            cleanseTimer = 0;
            for (Player player : players) {
                player.removeEffect(MobEffects.POISON);
                if (cleanseWither) {
                    player.removeEffect(MobEffects.WITHER);
                }
            }
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        cleanseTimer = 0;
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
