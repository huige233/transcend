package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * 虚空钻探（Void Bore）功能执行器。
 * <p>
 * 每 90 次 tick 调用（约 45 秒），从核心正下方向下扫描最多 64 格，消耗最多 64 个
 * 非空气方块；每消耗一个方块掷一次掉落表，将产物以掉落物形式投放到核心上方。
 */
public class VoidBoreExecutor implements CircleFunctionExecutor {

    private static final int EXTRACTION_INTERVAL = 90;
    private static final int MAX_BLOCKS_PER_CYCLE = 64;
    private static final int MAX_SCAN_DEPTH = 64;
    private static final int MANA_COST = 8;

    private final Map<BlockPos, Integer> tickCounters = new HashMap<>();

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.PRIMORDIAL.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        // 无需初始化
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        BlockPos key = ctx.getCorePos();
        int counter = tickCounters.getOrDefault(key, 0) + 1;
        if (counter < EXTRACTION_INTERVAL) {
            tickCounters.put(key.immutable(), counter);
            return;
        }
        tickCounters.put(key.immutable(), 0);

        if (!ctx.consumeMana(MANA_COST)) {
            return;
        }

        ServerLevel level = ctx.getLevel();
        RandomSource random = level.getRandom();
        BlockPos core = ctx.getCorePos();

        int extracted = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = 1; dy <= MAX_SCAN_DEPTH && extracted < MAX_BLOCKS_PER_CYCLE; dy++) {
            cursor.set(core.getX(), core.getY() - dy, core.getZ());
            if (cursor.getY() < level.getMinBuildHeight()) break;

            BlockState state = level.getBlockState(cursor);
            if (state.isAir() || state.getDestroySpeed(level, cursor) < 0) {
                // 跳过空气和不可破坏方块（如基岩）
                continue;
            }

            if (level.removeBlock(cursor, false)) {
                Item drop = pickDrop(random);
                ItemEntity entity = new ItemEntity(level,
                        core.getX() + 0.5, core.getY() + 1.0, core.getZ() + 0.5,
                        new ItemStack(drop));
                entity.setDefaultPickUpDelay();
                level.addFreshEntity(entity);
                extracted++;
            }
        }
    }

    private Item pickDrop(RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 55) {
            return Items.COBBLESTONE;
        } else if (roll < 80) {
            int sub = random.nextInt(4);
            return switch (sub) {
                case 0 -> Items.RAW_IRON;
                case 1 -> Items.RAW_COPPER;
                case 2 -> Items.RAW_GOLD;
                default -> Items.COAL;
            };
        } else if (roll < 92) {
            return Items.OBSIDIAN;
        } else if (roll < 98) {
            return Items.ENDER_PEARL;
        } else {
            return Items.DIAMOND;
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        tickCounters.remove(ctx.getCorePos());
    }
}
