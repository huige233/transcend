package com.huige233.transcend.ascension;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MageClassTest {
    @Test
    void legacySaveIdsResolveToCurrentClasses() {
        assertEquals(MageClass.CHRONOWEAVER, MageClass.getById("arcanist"));
        assertEquals(MageClass.OMNISCIENT, MageClass.getById("omnimancer"));
    }

    @Test
    void currentClassesHaveCanonicalMasteries() {
        assertEquals(ElementMastery.FIRE, MageClass.PYROMANCER.getCanonicalMastery());
        assertEquals(ElementMastery.WATER, MageClass.CRYOMANCER.getCanonicalMastery());
        assertEquals(ElementMastery.FIRE, MageClass.STORMCALLER.getCanonicalMastery());
        assertEquals(ElementMastery.CHAOS, MageClass.ABYSSWALKER.getCanonicalMastery());
        assertEquals(ElementMastery.EARTH, MageClass.EARTHSHAPER.getCanonicalMastery());
        assertEquals(ElementMastery.CHAOS, MageClass.CHRONOWEAVER.getCanonicalMastery());
        assertEquals(ElementMastery.CHAOS, MageClass.OMNISCIENT.getCanonicalMastery());
    }

    @Test
    void classSelectionAssignsDefaultWithoutOverwritingExistingMastery() {
        PlayerAscensionData fresh = new PlayerAscensionData();
        fresh.forceSetStage(1);
        assertTrue(fresh.selectClass(MageClass.CRYOMANCER));
        assertEquals(ElementMastery.WATER, fresh.getMastery());

        PlayerAscensionData existing = new PlayerAscensionData();
        existing.forceSetStage(1);
        assertTrue(existing.selectMastery(ElementMastery.METAL, true));
        assertTrue(existing.selectClass(MageClass.PYROMANCER));
        assertEquals(ElementMastery.METAL, existing.getMastery());
    }

    @Test
    void respecRestoresClassCanonicalMastery() {
        PlayerAscensionData data = new PlayerAscensionData();
        data.forceSetStage(1);
        assertTrue(data.selectClass(MageClass.CRYOMANCER));
        assertTrue(data.selectMastery(ElementMastery.METAL, true));

        data.respec();

        assertEquals(ElementMastery.WATER, data.getMastery());
    }

    @Test
    void ordinaryClassSelectionRequiresRitualTierOne() {
        PlayerAscensionData data = new PlayerAscensionData();
        assertFalse(data.selectClass(MageClass.PYROMANCER));
        data.forceSetStage(1);
        assertTrue(data.selectClass(MageClass.PYROMANCER));
    }

    @Test
    void talentTiersUseCultivationRealmRank() {
        PlayerAscensionData data = new PlayerAscensionData();
        assertTrue(data.isTierUnlockedByStage(0));
        assertTrue(data.isTierUnlockedByStage(1));
        assertFalse(data.isTierUnlockedByStage(2));

        data.setCultivationProgress(CultivationRealm.GOLDEN_CORE, CultivationStage.EARLY, 0);
        assertTrue(data.isTierUnlockedByStage(2));
        assertFalse(data.isTierUnlockedByStage(3));

        data.setCultivationProgress(CultivationRealm.SPIRIT_TRANSFORMATION, CultivationStage.EARLY, 0);
        assertTrue(data.isTierUnlockedByStage(3));
        assertFalse(data.isTierUnlockedByStage(4));

        data.setCultivationProgress(CultivationRealm.EARTH_IMMORTAL, CultivationStage.EARLY, 0);
        assertTrue(data.isTierUnlockedByStage(4));
        assertFalse(data.isTierUnlockedByStage(5));

        data.setCultivationProgress(CultivationRealm.GREAT_LUO, CultivationStage.EARLY, 0);
        assertTrue(data.isTierUnlockedByStage(5));
        assertFalse(data.isTierUnlockedByStage(6));
    }
}
