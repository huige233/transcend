package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleManaMath;
import com.huige233.transcend.circle.CircleTier;
import com.huige233.transcend.world.mana.ChunkManaSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public class LeylineConvergenceExecutor implements CircleFunctionExecutor {

    private static final float PULL_PER_CHUNK = 0.5f;

    private static final float CONVERGENCE_FLOOR = 2000.0f;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.ARCHON.getLevel();
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

    }
}
