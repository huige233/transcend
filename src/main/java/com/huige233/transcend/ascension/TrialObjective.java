package com.huige233.transcend.ascension;

/** 试炼目标：定义类型/目标数值与完成判定。 */
public class TrialObjective {
    public enum ObjectiveType {
        KILL_COUNT,
        CAST_COUNT,
        BOSS_KILL,
        DISCOVER_COMPONENTS,
        STABILIZE_CIRCLE,
        MULTI_CIRCLE,
        ARENA_WAVE,
        CRAFT_SCROLL,
        RESTORE_TABLET,
        NEXUS_ACTION,
        MANA_THROUGHPUT,
        ITEM_REQUIREMENT
    }

    private final String id;
    private final ObjectiveType type;
    private final int targetValue;
    private final String targetId;

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
            case ITEM_REQUIREMENT -> false;
        };
    }
}
