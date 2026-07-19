package com.huige233.transcend.circle;

public enum CircleTier {

    INITIATE(1, "initiate", 3, 1, 64, 6),

    ADEPT(2, "adept", 5, 1, 256, 18),

    MASTER(3, "master", 9, 4, 1024, 60),

    ARCHON(4, "archon", 13, 6, 4096, 180),

    PRIMORDIAL(5, "primordial", 17, 9, 16384, 480);

    private final int level;

    private final String id;

    private final int footprint;

    private final int height;

    private final int manaCapacity;

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

    public String getTranslationKey() {
        return "circle.transcend.tier." + id;
    }

    public static CircleTier fromLevel(int level) {
        for (CircleTier tier : values()) {
            if (tier.level == level) {
                return tier;
            }
        }
        return INITIATE;
    }
}
