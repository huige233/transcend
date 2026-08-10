package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import com.huige233.transcend.world.mana.ChunkManaSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/** 泉源焕新法阵功能执行器。 */
public class WellspringRenewalExecutor implements CircleFunctionExecutor {

    private static final float[] BONUS_BY_TIER = {
            0.0F,
            0.0F,
            0.5F,
            1.0F,
            1.75F,
            2.5F
    };

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.ADEPT.getLevel();
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
        if (tierLevel < 0 || tierLevel >= BONUS_BY_TIER.length) {
            return;
        }
        float bonus = BONUS_BY_TIER[tierLevel];
        if (bonus <= 0.0F) {
            return;
        }

        ChunkManaSavedData data = ChunkManaSavedData.get(level);
        ChunkPos chunkPos = ctx.getChunkPos();
        data.regenMana(chunkPos, bonus);
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {

    }
}
