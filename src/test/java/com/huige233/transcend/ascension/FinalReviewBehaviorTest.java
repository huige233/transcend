package com.huige233.transcend.ascension;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinalReviewBehaviorTest {
    @Test
    void greedDebtIsAnExplicitFinalCooldownMultiplier() {
        PlayerAscensionData data = new PlayerAscensionData();
        data.setVowForStage(3, "vow_of_greed");

        assertEquals(1.40F, data.getVowCooldownMult(), 0.0001F);
        assertEquals(0.0F, data.buildTotalStats().cooldownReduction, 0.0001F);

        data.liberateVow("vow_of_greed");
        assertEquals(1.0F, data.getVowCooldownMult(), 0.0001F);
    }

    @Test
    void immortalityAndResonanceUseCanonicalValues() {
        assertEquals(75.0F, AscensionHandler.applyImmortalityDamageReduction(100.0F), 0.0001F);
        assertEquals(6000L, AscensionHandler.IMMORTALITY_SAVE_COOLDOWN_TICKS);
        assertFalse(AscensionHandler.hasBothResonanceReactionCategories(
                Set.of("five_elements_generation")));
        assertTrue(AscensionHandler.hasBothResonanceReactionCategories(Set.of(
                "five_elements_generation", "five_elements_overcoming")));
    }

    @Test
    void environmentalManaConsumesOnlyAcceptableFraction() {
        assertEquals(0.0F, AscensionHandler.environmentalManaAcceptance(0, 0.4, 0.1, 0.4), 0.0F);
        assertEquals(0.1F, AscensionHandler.environmentalManaAcceptance(1, 0.8, 0.1, 0.4), 0.0001F);
        assertEquals(0.4F, AscensionHandler.environmentalManaAcceptance(2, 0.25, 0.05, 0.4), 0.0001F);
    }
}
