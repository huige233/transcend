package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.util.HashMap;
import java.util.Map;

/**
 * 次元锚（Dimensional Anchor）功能执行器。
 * <p>
 * 通过 {@link ForgeChunkManager#forceChunk} 强制加载法环核心所在区块，使其在
 * 没有玩家时也能继续 tick。
 */
public class DimensionalAnchorExecutor implements CircleFunctionExecutor {

    private static final String MODID = "transcend";

    /** 单例执行器跨多个法环核心共享 — 用 Map 跟踪每个核心当前强加载的区块。 */
    private final Map<BlockPos, ChunkPos> loadedChunks = new HashMap<>();

    /** 每个核心一个 tick 计数器（约 40 CM/min ≈ 0.67/tick → 每 2 tick 消耗 1）。 */
    private final Map<BlockPos, Integer> tickCounters = new HashMap<>();

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.ARCHON.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos corePos = ctx.getCorePos();
        ChunkPos cp = new ChunkPos(corePos);
        if (ForgeChunkManager.forceChunk(level, MODID, corePos, cp.x, cp.z, true, true)) {
            loadedChunks.put(corePos.immutable(), cp);
        }
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        BlockPos key = ctx.getCorePos();
        int counter = tickCounters.getOrDefault(key, 0) + 1;
        // 每 2 次 tick 消耗 1 点魔力 ≈ 0.5/tick × 60 tick/min × 20 BE ticks = 40 CM/min
        if (counter >= 2) {
            ctx.consumeMana(1);
            counter = 0;
        }
        tickCounters.put(key.immutable(), counter);
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos corePos = ctx.getCorePos();
        ChunkPos cp = loadedChunks.remove(corePos);
        if (cp == null) {
            cp = new ChunkPos(corePos);
        }
        ForgeChunkManager.forceChunk(level, MODID, corePos, cp.x, cp.z, false, true);
        tickCounters.remove(corePos);
    }
}
