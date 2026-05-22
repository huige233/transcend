package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 时序织机（Chrono Loom）功能执行器。
 * <p>
 * 加速世界的"局部时间流逝"：每次 tick 调用，在作用半径内随机挑选若干位置，
 * 对其中的农作物方块执行额外的随机 tick。v1 简化实现，不处理 BlockEntity 与
 * 火药 / 酿造台等容器的额外 tick。
 *
 * <p>每次 tick 调用扫描 32 个随机位置，单次最多对 32 个农作物执行随机 tick。
 */
public class ChronoLoomExecutor implements CircleFunctionExecutor {

    /** 单次 tick 调用尝试扫描的随机位置数量。 */
    private static final int SAMPLE_COUNT = 32;
    /** 单次 tick 调用最多触发的随机 tick 次数，避免过载。 */
    private static final int MAX_TICKED_PER_PULSE = 32;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.MASTER.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        // 无需特殊处理
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
            int dy = random.nextInt(7) - 3; // 在核心高度 ±3 范围内取样
            cursor.set(core.getX() + dx, core.getY() + dy, core.getZ() + dz);

            BlockState state = level.getBlockState(cursor);
            if (state.getBlock() instanceof CropBlock) {
                // 对作物执行一次随机 tick：等同于在自然 random tick 中触发一次生长
                state.randomTick(level, cursor.immutable(), random);
                ticked++;
            }
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        // 无需特殊处理
    }
}
