package com.huige233.transcend.ascension;

public enum CultivationStage {
    EARLY("early", 0),
    MIDDLE("middle", 1),
    LATE("late", 2);

    private final String id;
    private final int index;

    CultivationStage(String id, int index) {
        this.id = id;
        this.index = index;
    }

    public String getId() {
        return id;
    }

    public int getIndex() {
        return index;
    }

    public String getDisplayKey() {
        return "cultivation.stage.transcend." + id;
    }

    public CultivationStage next() {
        return switch (this) {
            case EARLY -> MIDDLE;
            case MIDDLE, LATE -> LATE;
        };
    }

    public static CultivationStage byId(String id) {
        if (id != null) {
            for (CultivationStage stage : values()) {
                if (stage.id.equals(id)) return stage;
            }
        }
        return EARLY;
    }

    public static CultivationStage fromLegacyLevel(int level) {
        if (level >= 7) return LATE;
        if (level >= 4) return MIDDLE;
        return EARLY;
    }
}
