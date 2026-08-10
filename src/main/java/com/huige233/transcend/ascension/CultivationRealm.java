package com.huige233.transcend.ascension;

/** 修炼境界枚举。 */
public enum CultivationRealm {
    SPIRIT_SENSING("spirit_sensing", 1, 1),
    FOUNDATION("foundation", 2, 1),
    GOLDEN_CORE("golden_core", 3, 2),
    NASCENT_SOUL("nascent_soul", 4, 2),
    SPIRIT_TRANSFORMATION("spirit_transformation", 5, 3),
    BODY_INTEGRATION("body_integration", 6, 3),
    MAHAYANA("mahayana", 7, 4),
    EARTH_IMMORTAL("earth_immortal", 8, 4),
    HEAVEN_IMMORTAL("heaven_immortal", 9, 5),
    GOLDEN_IMMORTAL("golden_immortal", 10, 5),
    GREAT_LUO("great_luo", 11, 6),
    CHAOS("chaos", 12, 7);

    private final String id;
    private final int rank;
    private final int maxEffectSlots;

    CultivationRealm(String id, int rank, int maxEffectSlots) {
        this.id = id;
        this.rank = rank;
        this.maxEffectSlots = maxEffectSlots;
    }

    public String getId() {
        return id;
    }

    public int getRank() {
        return rank;
    }

    public int getMaxEffectSlots() {
        return maxEffectSlots;
    }

    public int getMaxSpellTier() {
        return rank;
    }

    public String getDisplayKey() {
        return "cultivation.realm.transcend." + id;
    }

    public CultivationRealm next() {
        int nextIndex = ordinal() + 1;
        return nextIndex < values().length ? values()[nextIndex] : this;
    }

    public static CultivationRealm byId(String id) {
        if (id != null) {
            for (CultivationRealm realm : values()) {
                if (realm.id.equals(id)) return realm;
            }
        }
        return SPIRIT_SENSING;
    }

    public static CultivationRealm byRank(int rank) {
        int index = Math.max(1, Math.min(values().length, rank)) - 1;
        return values()[index];
    }

    public static CultivationRealm fromLegacyStage(int stage) {
        return switch (Math.max(0, Math.min(4, stage))) {
            case 0 -> SPIRIT_SENSING;
            case 1 -> FOUNDATION;
            case 2 -> GOLDEN_CORE;
            case 3 -> NASCENT_SOUL;
            default -> SPIRIT_TRANSFORMATION;
        };
    }
}
