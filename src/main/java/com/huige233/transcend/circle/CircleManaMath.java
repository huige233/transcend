package com.huige233.transcend.circle;

/**
 * 魔法阵魔力计算工具类。
 * <p>
 * 集中存放与魔力上限、消耗、缓冲、宽限期等相关的数学公式与常量，
 * 供运行时控制器与 UI 共同调用，避免散落在多处导致数值不一致。
 * <p>
 * 区块魔力（Chunk Mana）以浮点数形式按区块存储，物品魔力（Item Mana，CM）则以整数为主、
 * 按 {@link #CHUNK_MANA_PER_CM} 的比率与区块魔力相互转换。
 */
public final class CircleManaMath {

    /** 1 单位物品魔力（CM）等价的区块魔力数量。 */
    public static final float CHUNK_MANA_PER_CM = 10.0f;
    /** 区块魔力的最低基底：低于此值时多数功能将停止运作。 */
    public static final float CHUNK_MANA_FLOOR = 1000.0f;
    /** 区块魔力的“贫瘠”阈值：低于此值时升级费用上升。 */
    public static final float CHUNK_MANA_WEAK_THRESHOLD = 2500.0f;
    /** 区块魔力的“富饶”阈值：高于此值时升级费用下降。 */
    public static final float CHUNK_MANA_RICH_THRESHOLD = 7500.0f;

    private CircleManaMath() {
        // 工具类禁止实例化
    }

    /**
     * 计算魔法阵在当前等级、符文配置、干扰数量与环境魔力下的最终每分钟消耗。
     * <p>
     * 公式如下：
     * <pre>
     * final = base
     *       * (1 + 0.18 * power + 0.08 * duration + 0.12 * special)
     *       * (1 - 0.08 * efficiency)
     *       * interference
     *       * chunkQuality
     * </pre>
     * 其中：
     * <ul>
     *     <li>{@code interference = 1.0 + 0.25 * max(0, interferenceCount - 1)}，即首个干扰免费，之后每个 +25%。</li>
     *     <li>{@code chunkQuality}：区块魔力 &lt; 2500 时为 1.25，2500-7500 之间为 1.0，&gt; 7500 时为 0.9。</li>
     * </ul>
     *
     * @param baseUpkeep         基础每分钟消耗
     * @param powerLevel         强度符文等级
     * @param durationLevel      持续符文等级
     * @param efficiencyLevel    效率符文等级
     * @param specialLevel       特殊符文等级
     * @param interferenceCount  当前生效的其它干扰魔法阵数量
     * @param chunkMana          当前所在区块的魔力数值
     * @return 最终的每分钟魔力消耗（CM/min）
     */
    public static float computeFinalUpkeep(float baseUpkeep,
                                           int powerLevel,
                                           int durationLevel,
                                           int efficiencyLevel,
                                           int specialLevel,
                                           int interferenceCount,
                                           float chunkMana) {
        float runeFactor = 1.0f + 0.18f * powerLevel + 0.08f * durationLevel + 0.12f * specialLevel;
        float efficiencyFactor = 1.0f - 0.08f * efficiencyLevel;
        if (efficiencyFactor < 0.0f) {
            efficiencyFactor = 0.0f;
        }
        int extraInterference = Math.max(0, interferenceCount - 1);
        float interferenceFactor = 1.0f + 0.25f * extraInterference;
        float chunkQuality;
        if (chunkMana < CHUNK_MANA_WEAK_THRESHOLD) {
            chunkQuality = 1.25f;
        } else if (chunkMana > CHUNK_MANA_RICH_THRESHOLD) {
            chunkQuality = 0.9f;
        } else {
            chunkQuality = 1.0f;
        }
        return baseUpkeep * runeFactor * efficiencyFactor * interferenceFactor * chunkQuality;
    }

    /**
     * 计算内部魔力缓冲池上限：{@code 64 + 32 * durationLevel}。
     *
     * @param durationLevel 持续符文等级
     * @return 缓冲池容量（CM）
     */
    public static int computeBufferMax(int durationLevel) {
        return 64 + 32 * durationLevel;
    }

    /**
     * 计算魔力中断后的宽限刻数：{@code 60 + 40 * durationLevel}。
     * <p>
     * 在此期间魔法阵不会立即关闭，玩家有机会补充魔力以维持效果。
     *
     * @param durationLevel 持续符文等级
     * @return 宽限期长度（游戏刻）
     */
    public static int computeGraceTicks(int durationLevel) {
        return 60 + 40 * durationLevel;
    }

    /**
     * 将区块魔力换算为等价的物品魔力（CM）。
     *
     * @param chunkMana 区块魔力数值
     * @return 等价的物品魔力（CM）
     */
    public static float chunkManaToItemMana(float chunkMana) {
        return chunkMana / CHUNK_MANA_PER_CM;
    }

    /**
     * 将物品魔力（CM）换算为等价的区块魔力。
     *
     * @param itemMana 物品魔力数值
     * @return 等价的区块魔力
     */
    public static float itemManaToChunkMana(float itemMana) {
        return itemMana * CHUNK_MANA_PER_CM;
    }
}
