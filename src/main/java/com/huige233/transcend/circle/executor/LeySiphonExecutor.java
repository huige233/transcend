package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleManaMath;
import com.huige233.transcend.circle.CircleTier;
import com.huige233.transcend.world.mana.ChunkManaSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public class LeySiphonExecutor implements CircleFunctionExecutor {

    private static final double TICKS_PER_MINUTE = 60.0;

    private static final double[] CM_PER_MIN_BY_TIER = {
            0.0,
            4.0,
            12.0,
            35.0,
            90.0,
            180.0
    };

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {

        return ctx.getTier().getLevel() >= CircleTier.INITIATE.getLevel();
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

        int tierLevel = ctx.getTier().getLevel();
        if (tierLevel < 1 || tierLevel >= CM_PER_MIN_BY_TIER.length) {
            return;
        }
        double cmPerMin = CM_PER_MIN_BY_TIER[tierLevel];
        double cmPerTickCall = cmPerMin / TICKS_PER_MINUTE;
        if (cmPerTickCall <= 0.0) {
            return;
        }

        int remainingSpace = ctx.getMaxMana() - ctx.getStoredMana();
        if (remainingSpace <= 0) {
            return;
        }

        ChunkManaSavedData chunkManaData = ChunkManaSavedData.get(level);
        ChunkPos chunkPos = ctx.getChunkPos();
        float currentChunkMana = chunkManaData.getMana(chunkPos);
        if (currentChunkMana < CircleManaMath.CHUNK_MANA_FLOOR) {
            return;
        }

        int desiredCm = (int) Math.floor(cmPerTickCall);

        if (desiredCm <= 0) {
            double frac = cmPerTickCall - Math.floor(cmPerTickCall);
            if (level.getRandom().nextDouble() < frac) {
                desiredCm = 1;
            } else {
                return;
            }
        } else {

            double frac = cmPerTickCall - desiredCm;
            if (frac > 0.0 && level.getRandom().nextDouble() < frac) {
                desiredCm += 1;
            }
        }

        desiredCm = Math.min(desiredCm, remainingSpace);
        if (desiredCm <= 0) {
            return;
        }

        float chunkManaNeeded = desiredCm * CircleManaMath.CHUNK_MANA_PER_CM;
        float chunkManaAvailable = currentChunkMana - CircleManaMath.CHUNK_MANA_FLOOR;
        float chunkManaToConsume = Math.min(chunkManaNeeded, chunkManaAvailable);
        if (chunkManaToConsume <= 0.0f) {
            return;
        }

        float consumed = chunkManaData.consumeMana(chunkPos, chunkManaToConsume);
        if (consumed <= 0.0f) {
            return;
        }

        int gainedCm = (int) Math.floor(consumed / CircleManaMath.CHUNK_MANA_PER_CM);
        if (gainedCm <= 0) {
            return;
        }
        gainedCm = Math.min(gainedCm, remainingSpace);

        ctx.insertMana(gainedCm);
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {

    }
}
