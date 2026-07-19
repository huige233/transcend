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

public class DeepSightVeilExecutor implements CircleFunctionExecutor {

    private static final int BUFF_DURATION_TICKS = 80;

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

        boolean grantConduit = ctx.getTier().getLevel() >= CircleTier.MASTER.getLevel();
        for (Player player : players) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION,
                    BUFF_DURATION_TICKS,
                    0,
                    true,
                    false,
                    true
            ));
            player.addEffect(new MobEffectInstance(
                    MobEffects.WATER_BREATHING,
                    BUFF_DURATION_TICKS,
                    0,
                    true,
                    false,
                    true
            ));
            if (grantConduit) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.CONDUIT_POWER,
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
