package com.huige233.transcend.ascension;

/**
 * 试炼目标 — 飞升仪式中的单个目标条件。
 * 每个仪式包含多个目标，玩家需完成全部目标以推进阶段。
 */
public class TrialObjective {
    public enum ObjectiveType {
        KILL_COUNT,          // 击杀计数
        CAST_COUNT,          // 施法次数
        BOSS_KILL,           // Boss击杀
        DISCOVER_COMPONENTS, // 发现法术组件
        STABILIZE_CIRCLE,    // 稳定法阵持续时间
        MULTI_CIRCLE,        // 同时维持多法阵
        ARENA_WAVE,          // 竞技场波数
        CRAFT_SCROLL,        // 制作卷轴
        RESTORE_TABLET,      // 修复石板
        NEXUS_ACTION,        // 枢纽维度行动
        MANA_THROUGHPUT,     // 魔力吞吐量
        ITEM_REQUIREMENT     // 物品需求（消耗）
    }

    private final String id;
    private final ObjectiveType type;
    private final int targetValue;    // 目标数值
    private final String targetId;    // 目标ID（如Boss类型、物品ID等）

    public TrialObjective(String id, ObjectiveType type, int targetValue, String targetId) {
        this.id = id;
        this.type = type;
        this.targetValue = targetValue;
        this.targetId = targetId;
    }

    public String getId() { return id; }
    public ObjectiveType getType() { return type; }
    public int getTargetValue() { return targetValue; }
    public String getTargetId() { return targetId; }

    public String getTranslationKey() { return "trial.transcend." + id; }
    public String getDescriptionKey() { return "trial.transcend." + id + ".desc"; }

    /** 检查玩家数据是否满足此目标 */
    public boolean isMet(PlayerAscensionData data) {
        return switch (type) {
            case KILL_COUNT -> data.getTotalKills() >= targetValue;
            case CAST_COUNT -> data.getTotalCasts() >= targetValue;
            case BOSS_KILL -> data.getBossKills() >= targetValue;
            case DISCOVER_COMPONENTS -> data.getDiscoveredComponents() >= targetValue;
            case STABILIZE_CIRCLE -> data.getCircleStabilizeSeconds() >= targetValue;
            case MULTI_CIRCLE -> data.getMaxConcurrentCircles() >= targetValue;
            case ARENA_WAVE -> data.getHighestArenaWave() >= targetValue;
            case CRAFT_SCROLL -> data.getScrollsCrafted() >= targetValue;
            case RESTORE_TABLET -> data.getTabletsRestored() >= targetValue;
            case NEXUS_ACTION -> data.getNexusActions() >= targetValue;
            case MANA_THROUGHPUT -> data.getPeakManaThroughput() >= targetValue;
            case ITEM_REQUIREMENT -> false; // 仪式时由外部检查物品栏
        };
    }
}
