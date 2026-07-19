package com.huige233.transcend.ascension;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CultivationRealmTest {
    @Test
    void realmRanksAndEffectSlotsReachConfiguredMaximum() {
        assertEquals(1, CultivationRealm.SPIRIT_SENSING.getRank());
        assertEquals(1, CultivationRealm.SPIRIT_SENSING.getMaxEffectSlots());
        assertEquals(12, CultivationRealm.CHAOS.getMaxSpellTier());
        assertEquals(7, CultivationRealm.CHAOS.getMaxEffectSlots());
    }

    @Test
    void legacyProgressMapsWithoutDiscardingAdvancedPlayers() {
        assertEquals(CultivationRealm.SPIRIT_SENSING, CultivationRealm.fromLegacyStage(0));
        assertEquals(CultivationRealm.SPIRIT_TRANSFORMATION, CultivationRealm.fromLegacyStage(4));
        assertEquals(CultivationStage.EARLY, CultivationStage.fromLegacyLevel(3));
        assertEquals(CultivationStage.MIDDLE, CultivationStage.fromLegacyLevel(4));
        assertEquals(CultivationStage.LATE, CultivationStage.fromLegacyLevel(7));
    }
}
