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

/** 维度锚法阵功能执行器。 */
public class DimensionalAnchorExecutor implements CircleFunctionExecutor {

    private static final String MODID = "transcend";

    private final Map<BlockPos, ChunkPos> loadedChunks = new HashMap<>();

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
