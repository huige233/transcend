package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleManaMath;
import com.huige233.transcend.circle.CircleTier;
import com.huige233.transcend.world.mana.ChunkManaSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * 地脉汇聚（Leyline Convergence）功能执行器 — 跨区块抽取（T4+）。
 *
 * <p>每 tick 从核心所在区块周围 3x3 区块（共 8 个相邻区块，跳过自身）各抽取 0.5
 * 区块魔力（== 0.05 CM），仅当该区块魔力高于 2000 floor 时。
 * 累计后按 {@link CircleManaMath#CHUNK_MANA_PER_CM} 折算为 CM 注入核心。
 */
public class LeylineConvergenceExecutor implements CircleFunctionExecutor {

    /** 每个相邻区块每次 tick 抽取的区块魔力。 */
    private static final float PULL_PER_CHUNK = 0.5f;
    /** 区块魔力抽取下限：低于此值的区块不会被抽取。 */
    private static final float CONVERGENCE_FLOOR = 2000.0f;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.ARCHON.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        // 无需特殊初始化
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        ServerLevel level = ctx.getLevel();
        if (level == null) {
            return;
        }

        int remainingSpace = ctx.getMaxMana() - ctx.getStoredMana();
        if (remainingSpace <= 0) {
            return;
        }

        ChunkManaSavedData data = ChunkManaSavedData.get(level);
        ChunkPos coreChunk = ctx.getChunkPos();

        float totalConsumed = 0f;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                ChunkPos neighbor = new ChunkPos(coreChunk.x + dx, coreChunk.z + dz);
                float current = data.getMana(neighbor);
                if (current <= CONVERGENCE_FLOOR) continue;
                float available = current - CONVERGENCE_FLOOR;
                float toPull = Math.min(PULL_PER_CHUNK, available);
                if (toPull <= 0f) continue;
                float consumed = data.consumeMana(neighbor, toPull);
                totalConsumed += consumed;
            }
        }

        if (totalConsumed <= 0f) {
            return;
        }

        // 折算为 CM：每 CircleManaMath.CHUNK_MANA_PER_CM 区块魔力 = 1 CM。
        // 单 tick 收益通常 < 1（8 个区块 * 0.5 / 10 = 0.4 CM/tick），所以用概率方式累积长期速率。
        double cmGained = totalConsumed / CircleManaMath.CHUNK_MANA_PER_CM;
        int wholeCm = (int) Math.floor(cmGained);
        double frac = cmGained - wholeCm;
        if (frac > 0.0 && level.getRandom().nextDouble() < frac) {
            wholeCm += 1;
        }
        if (wholeCm <= 0) {
            return;
        }
        wholeCm = Math.min(wholeCm, remainingSpace);
        ctx.insertMana(wholeCm);
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        // 无需特殊清理
    }
}
