package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** 炉心稳定法阵功能执行器。 */
public class HearthStabilityExecutor implements CircleFunctionExecutor {

    private static final int RESISTANCE_DURATION_TICKS = 40;

    private static final int FIRE_SCAN_VERTICAL = 4;

    private static final int MAX_EXTINGUISH_PER_PULSE = 64;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.ADEPT.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {

    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        ServerLevel level = ctx.getLevel();
        if (level == null) {
            return;
        }

        List<Player> players = getPlayersInRadius(ctx);
        for (Player player : players) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE,
                    RESISTANCE_DURATION_TICKS,
                    0,
                    true,
                    false,
                    true
            ));
        }

        extinguishFires(ctx, level);
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {

    }

    private void extinguishFires(CircleFunctionContext ctx, ServerLevel level) {
        int radius = (int) ctx.getBaseRadius();
        if (radius <= 0) {
            return;
        }

        BlockPos core = ctx.getCorePos();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int extinguished = 0;

        for (int dy = -FIRE_SCAN_VERTICAL; dy <= FIRE_SCAN_VERTICAL && extinguished < MAX_EXTINGUISH_PER_PULSE; dy++) {
            for (int dx = -radius; dx <= radius && extinguished < MAX_EXTINGUISH_PER_PULSE; dx++) {
                for (int dz = -radius; dz <= radius && extinguished < MAX_EXTINGUISH_PER_PULSE; dz++) {
                    cursor.set(core.getX() + dx, core.getY() + dy, core.getZ() + dz);
                    if (level.getBlockState(cursor).is(Blocks.FIRE)) {
                        level.removeBlock(cursor, false);
                        extinguished++;
                    }
                }
            }
        }
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
