package com.huige233.transcend.circle;

public final class CircleManaMath {

    public static final float CHUNK_MANA_PER_CM = 10.0f;

    public static final float CHUNK_MANA_FLOOR = 1000.0f;

    public static final float CHUNK_MANA_WEAK_THRESHOLD = 2500.0f;

    public static final float CHUNK_MANA_RICH_THRESHOLD = 7500.0f;

    private CircleManaMath() {

    }

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

    public static int computeBufferMax(int durationLevel) {
        return 64 + 32 * durationLevel;
    }

    public static int computeGraceTicks(int durationLevel) {
        return 60 + 40 * durationLevel;
    }

    public static float chunkManaToItemMana(float chunkMana) {
        return chunkMana / CHUNK_MANA_PER_CM;
    }

    public static float itemManaToChunkMana(float itemMana) {
        return itemMana * CHUNK_MANA_PER_CM;
    }
}
