package com.huige233.transcend.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MagicCrystalHelperArithmeticTest {
    @Test
    void malformedStoredManaCannotReduceAvailableTotal() {
        assertEquals(23L, MagicCrystalHelper.saturatingManaAdd(23L, -7L));
        assertEquals(23L, MagicCrystalHelper.saturatingManaAdd(23L, 0L));
    }

    @Test
    void availableManaAndCrystalCeilingDoNotOverflow() {
        assertEquals(Long.MAX_VALUE,
                MagicCrystalHelper.saturatingManaAdd(Long.MAX_VALUE - 2L, 3L));
        assertEquals(Integer.MAX_VALUE,
                MagicCrystalHelper.ceilDiv(Integer.MAX_VALUE, 1));
        assertEquals(2, MagicCrystalHelper.ceilDiv(Integer.MAX_VALUE, Integer.MAX_VALUE - 1));
    }
}
