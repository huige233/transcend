package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleManaMath;
import com.huige233.transcend.circle.CircleTier;
import com.huige233.transcend.world.mana.ChunkManaSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * 地脉虹吸（Leyline Siphon）功能执行器。
 * <p>
 * 该法环持续从所在区块的区块魔力中抽取能量，并按 {@link CircleManaMath#CHUNK_MANA_PER_CM}
 * 的比率转换为内部物品魔力（CM）。等级越高抽取速率越快，但区块魔力低于
 * {@link CircleManaMath#CHUNK_MANA_FLOOR} 时会停止抽取，避免破坏环境基底。
 */
public class LeySiphonExecutor implements CircleFunctionExecutor {

    /** 每分钟 tick 调用次数：每 20tick 调用一次 → 60 次/分钟。 */
    private static final double TICKS_PER_MINUTE = 60.0;

    /**
     * 各层级每分钟抽取的物品魔力（CM/min）。
     * 索引与 {@link CircleTier#getLevel()} 对齐（1-based，索引 0 留空）。
     */
    private static final double[] CM_PER_MIN_BY_TIER = {
            0.0,    // 占位
            4.0,    // T1
            12.0,   // T2
            35.0,   // T3
            90.0,   // T4
            180.0   // T5
    };

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        // T1 起即可激活
        return ctx.getTier().getLevel() >= CircleTier.INITIATE.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        // 激活时无需特殊处理
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        ServerLevel level = ctx.getLevel();
        if (level == null) {
            return;
        }

        // 计算本次 tick 应抽取的物品魔力
        int tierLevel = ctx.getTier().getLevel();
        if (tierLevel < 1 || tierLevel >= CM_PER_MIN_BY_TIER.length) {
            return;
        }
        double cmPerMin = CM_PER_MIN_BY_TIER[tierLevel];
        double cmPerTickCall = cmPerMin / TICKS_PER_MINUTE;
        if (cmPerTickCall <= 0.0) {
            return;
        }

        // 缓冲池剩余空间，避免溢出
        int remainingSpace = ctx.getMaxMana() - ctx.getStoredMana();
        if (remainingSpace <= 0) {
            return;
        }

        // 区块魔力底线检查：低于 1000 不抽取
        ChunkManaSavedData chunkManaData = ChunkManaSavedData.get(level);
        ChunkPos chunkPos = ctx.getChunkPos();
        float currentChunkMana = chunkManaData.getMana(chunkPos);
        if (currentChunkMana < CircleManaMath.CHUNK_MANA_FLOOR) {
            return;
        }

        // 期望抽取的物品魔力（取整，至少为 1 时才进行抽取，否则用累积式更佳，但此处简化处理）
        int desiredCm = (int) Math.floor(cmPerTickCall);
        // 若每 tick 不足 1 CM，则按概率抽取（保证长期速率正确）
        if (desiredCm <= 0) {
            double frac = cmPerTickCall - Math.floor(cmPerTickCall);
            if (level.getRandom().nextDouble() < frac) {
                desiredCm = 1;
            } else {
                return;
            }
        } else {
            // 处理小数尾差
            double frac = cmPerTickCall - desiredCm;
            if (frac > 0.0 && level.getRandom().nextDouble() < frac) {
                desiredCm += 1;
            }
        }

        desiredCm = Math.min(desiredCm, remainingSpace);
        if (desiredCm <= 0) {
            return;
        }

        // 计算需要消耗的等价区块魔力，并保证不会击穿底线
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

        // 反向折算成实际获得的物品魔力
        int gainedCm = (int) Math.floor(consumed / CircleManaMath.CHUNK_MANA_PER_CM);
        if (gainedCm <= 0) {
            return;
        }
        gainedCm = Math.min(gainedCm, remainingSpace);

        ctx.insertMana(gainedCm);
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        // 无需特殊处理
    }
}
