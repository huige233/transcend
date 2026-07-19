package com.huige233.transcend.spell;

import com.huige233.transcend.ascension.ElementMastery;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MasteryResistanceTest {
    @Test
    void masteryUsesBoundedSameOtherAndOmniBonuses() {
        assertEquals(0.20F, SpellDamageService.masteryResistance(ElementMastery.FIRE, SpellElement.FIRE));
        assertEquals(0.05F, SpellDamageService.masteryResistance(ElementMastery.FIRE, SpellElement.WATER));
        assertEquals(0.10F, SpellDamageService.masteryResistance(ElementMastery.OMNI, SpellElement.EARTH));
        assertEquals(0.0F, SpellDamageService.masteryResistance(ElementMastery.NONE, SpellElement.FIRE));
    }
}
