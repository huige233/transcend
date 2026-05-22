package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * 矿脉汇聚法环（Mineral Convergence）功能执行器。
 * <p>
 * 周期性（每 120 次 tick 调用 ≈ 2 分钟）在核心下方 10 格的 5x5x5 范围内将一个
 * 随机的石头 / 深板岩 / 凝灰岩 / 下界岩转化为一个随机矿石。
 */
public class MineralConvergenceExecutor implements CircleFunctionExecutor {

    private static final int CONVERSION_INTERVAL = 120;
    private static final int MANA_COST = 3;

    private final Map<BlockPos, Integer> tickCounters = new HashMap<>();

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.MASTER.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        // 无需初始化
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        BlockPos key = ctx.getCorePos();
        int counter = tickCounters.getOrDefault(key, 0) + 1;
        if (counter < CONVERSION_INTERVAL) {
            tickCounters.put(key.immutable(), counter);
            return;
        }
        tickCounters.put(key.immutable(), 0);

        ServerLevel level = ctx.getLevel();
        RandomSource random = level.getRandom();

        // 在核心下方 10 格为中心的 5x5x5 立方体中随机挑一个位置
        BlockPos center = ctx.getCorePos().below(10);
        int dx = random.nextInt(5) - 2;
        int dy = random.nextInt(5) - 2;
        int dz = random.nextInt(5) - 2;
        BlockPos target = center.offset(dx, dy, dz);

        BlockState current = level.getBlockState(target);
        Block currentBlock = current.getBlock();
        if (currentBlock != Blocks.STONE && currentBlock != Blocks.DEEPSLATE
                && currentBlock != Blocks.TUFF && currentBlock != Blocks.NETHERRACK) {
            return;
        }

        if (!ctx.consumeMana(MANA_COST)) {
            return;
        }

        Block ore = pickOre(random);
        level.setBlock(target, ore.defaultBlockState(), 3);
    }

    private Block pickOre(RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 65) {
            // 常见：铁 / 铜
            return random.nextBoolean() ? Blocks.IRON_ORE : Blocks.COPPER_ORE;
        } else if (roll < 90) {
            // 中等：金 / 红石 / 青金石
            int sub = random.nextInt(3);
            return switch (sub) {
                case 0 -> Blocks.GOLD_ORE;
                case 1 -> Blocks.REDSTONE_ORE;
                default -> Blocks.LAPIS_ORE;
            };
        } else if (roll < 98) {
            return Blocks.DIAMOND_ORE;
        } else {
            return Blocks.EMERALD_ORE;
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        tickCounters.remove(ctx.getCorePos());
    }
}
