package com.huige233.transcend.circle;

/**
 * 魔法阵等级枚举。
 * <p>
 * 每个等级定义了魔法阵的物理占地、最大高度、内部魔力缓冲容量以及单位时间内可处理的最大吞吐量。
 * 等级越高，能够承载的功能越强大，但同时也对结构、材料以及周围环境的魔力浓度要求更高。
 */
public enum CircleTier {
    /** T1：入门级，3x3 平面，单层结构，64 CM 缓冲，6 CM/分钟吞吐。 */
    INITIATE(1, "initiate", 3, 1, 64, 6),
    /** T2：精通级，5x5 平面，单层结构，256 CM 缓冲，18 CM/分钟吞吐。 */
    ADEPT(2, "adept", 5, 1, 256, 18),
    /** T3：宗师级，9x9 平面，四层结构，1024 CM 缓冲，60 CM/分钟吞吐。 */
    MASTER(3, "master", 9, 4, 1024, 60),
    /** T4：执政级，13x13 平面，六层结构，4096 CM 缓冲，180 CM/分钟吞吐。 */
    ARCHON(4, "archon", 13, 6, 4096, 180),
    /** T5：原初级，17x17 平面，九层结构，16384 CM 缓冲，480 CM/分钟吞吐。 */
    PRIMORDIAL(5, "primordial", 17, 9, 16384, 480);

    /** 数值化等级（1-5）。 */
    private final int level;
    /** 字符串 ID，用于本地化键、配方查找等。 */
    private final String id;
    /** 占地正方形边长（方块数）。 */
    private final int footprint;
    /** 允许的最大高度（方块数）。 */
    private final int height;
    /** 内部魔力（CM）缓冲上限。 */
    private final int manaCapacity;
    /** 每分钟最大吞吐量（CM/min）。 */
    private final int throughputPerMinute;

    CircleTier(int level, String id, int footprint, int height, int manaCapacity, int throughputPerMinute) {
        this.level = level;
        this.id = id;
        this.footprint = footprint;
        this.height = height;
        this.manaCapacity = manaCapacity;
        this.throughputPerMinute = throughputPerMinute;
    }

    public int getLevel() {
        return level;
    }

    public String getId() {
        return id;
    }

    public int getFootprint() {
        return footprint;
    }

    public int getHeight() {
        return height;
    }

    public int getManaCapacity() {
        return manaCapacity;
    }

    public int getThroughputPerMinute() {
        return throughputPerMinute;
    }

    /**
     * 获取魔法阵的基础作用半径（方块）。
     * <p>
     * 此半径用于功能的默认覆盖范围计算，可被部分功能或符文修正。
     *
     * @return 基础作用半径
     */
    public int getBaseRadius() {
        switch (this) {
            case INITIATE:
                return 8;
            case ADEPT:
                return 16;
            case MASTER:
                return 32;
            case ARCHON:
                return 64;
            case PRIMORDIAL:
                return 96;
            default:
                return 0;
        }
    }

    /**
     * 获取本地化键，用于在界面、提示信息中显示该等级的名称。
     *
     * @return 本地化键，例如 {@code "circle.transcend.tier.master"}
     */
    public String getTranslationKey() {
        return "circle.transcend.tier." + id;
    }

    /**
     * 通过数值化等级查找对应的枚举值。
     *
     * @param level 数值化等级（1-5）
     * @return 对应的 {@link CircleTier}，若未找到则返回 {@link #INITIATE} 作为兜底
     */
    public static CircleTier fromLevel(int level) {
        for (CircleTier tier : values()) {
            if (tier.level == level) {
                return tier;
            }
        }
        return INITIATE;
    }
}
