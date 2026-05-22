package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import com.huige233.transcend.world.mana.ChunkManaSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * 涌泉复苏（Wellspring Renewal）功能执行器。
 * <p>
 * 持续向所在区块的区块魔力中注入"额外恢复量"，相当于在自然恢复速率之上的加速回血。
 * 注入量按等级缩放，调用 {@link ChunkManaSavedData#regenMana(ChunkPos, float)}
 * 进行回复（注意：该方法只会回复到 {@link ChunkManaSavedData#DEFAULT_MANA} 上限）。
 *
 * <p>每等级（每次 tick 调用，即 20 game tick）的附加恢复量：
 * <ul>
 *     <li>T2：0.5</li>
 *     <li>T3：1.0</li>
 *     <li>T4：1.75</li>
 *     <li>T5：2.5</li>
 * </ul>
 */
public class WellspringRenewalExecutor implements CircleFunctionExecutor {

    /** 各层级每次 tick 调用注入的额外恢复量，索引与等级对齐（1-based）。 */
    private static final float[] BONUS_BY_TIER = {
            0.0F,    // 占位
            0.0F,    // T1（未开放）
            0.5F,    // T2
            1.0F,    // T3
            1.75F,   // T4
            2.5F     // T5
    };

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.ADEPT.getLevel();
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
        // 已注入的魔力会保留在区块中，无需回收
    }
}
