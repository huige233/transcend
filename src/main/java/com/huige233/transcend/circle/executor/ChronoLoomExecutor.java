package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ChronoLoomExecutor implements CircleFunctionExecutor {

    private static final int SAMPLE_COUNT = 32;

    private static final int MAX_TICKED_PER_PULSE = 32;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.MASTER.getLevel();
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

        int radius = (int) ctx.getBaseRadius();
        if (radius <= 0) {
            return;
        }

        BlockPos core = ctx.getCorePos();
        RandomSource random = level.getRandom();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        int ticked = 0;
        for (int i = 0; i < SAMPLE_COUNT && ticked < MAX_TICKED_PER_PULSE; i++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            int dy = random.nextInt(7) - 3;
            cursor.set(core.getX() + dx, core.getY() + dy, core.getZ() + dz);

            BlockState state = level.getBlockState(cursor);
            if (state.getBlock() instanceof CropBlock) {

                state.randomTick(level, cursor.immutable(), random);
                ticked++;
            }
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {

    }
}
