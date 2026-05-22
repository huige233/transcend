package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 葳蕤收获法环（Verdant Reaping）功能执行器。
 * <p>
 * 每 160 tick（约 8 秒）扫描半径范围内的成熟农作物，
 * 收割并重新播种最多 32 株。收割后的物品掉落由 {@link ServerLevel#destroyBlock} 处理，
 * 并在原位置写入农作物方块的默认状态以完成"重播种"。
 */
public class VerdantReapingExecutor implements CircleFunctionExecutor {

    /** 扫描周期（tick）。 */
    private static final int SCAN_INTERVAL_TICKS = 160;
    /** 单次最多收割的农作物数量。 */
    private static final int MAX_HARVEST_PER_PASS = 32;

    /** 内部计时器。 */
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

        // tick 每 20 game tick 触发一次
        timer += 20;
        if (timer < SCAN_INTERVAL_TICKS) {
            return;
        }
        timer = 0;

        int radius = (int) ctx.getBaseRadius();
        BlockPos core = ctx.getCorePos();
        int harvested = 0;

        // 上下各 2 格搜索，覆盖一般农田高度
        for (int dy = -2; dy <= 2 && harvested < MAX_HARVEST_PER_PASS; dy++) {
            for (int dx = -radius; dx <= radius && harvested < MAX_HARVEST_PER_PASS; dx++) {
                for (int dz = -radius; dz <= radius && harvested < MAX_HARVEST_PER_PASS; dz++) {
                    BlockPos pos = core.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                        // 销毁并掉落物品（true = drop drops）
                        level.destroyBlock(pos, true);
                        // 重新播种为默认（幼苗）状态
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
