package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/** 翠绿收割法阵功能执行器。 */
public class VerdantReapingExecutor implements CircleFunctionExecutor {

    private static final int SCAN_INTERVAL_TICKS = 160;

    private static final int MAX_HARVEST_PER_PASS = 32;

    private int timer = 0;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.ADEPT.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        timer = 0;
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        ServerLevel level = ctx.getLevel();
        if (level == null) {
            return;
        }

        timer += 20;
        if (timer < SCAN_INTERVAL_TICKS) {
            return;
        }
        timer = 0;

        int radius = (int) ctx.getBaseRadius();
        BlockPos core = ctx.getCorePos();
        int harvested = 0;

        for (int dy = -2; dy <= 2 && harvested < MAX_HARVEST_PER_PASS; dy++) {
            for (int dx = -radius; dx <= radius && harvested < MAX_HARVEST_PER_PASS; dx++) {
                for (int dz = -radius; dz <= radius && harvested < MAX_HARVEST_PER_PASS; dz++) {
                    BlockPos pos = core.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {

                        level.destroyBlock(pos, true);

                        level.setBlockAndUpdate(pos, crop.defaultBlockState());
                        harvested++;
                    }
                }
            }
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        timer = 0;
    }
}
