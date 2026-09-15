package com.huige233.transcend.tech.energy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 验证长整型储能在最大值附近不会溢出、模拟传输不改余额且负数请求无效。 */
class LongEnergyStorageTest {
    @Test
    void saturatesAtLongMaximumWithoutOverflow() {
        LongEnergyStorage storage = new LongEnergyStorage(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, storage.receive(Long.MAX_VALUE));
        assertEquals(0L, storage.receive(1L));
        assertEquals(Long.MAX_VALUE, storage.stored());
        assertEquals(Long.MAX_VALUE, storage.extract(Long.MAX_VALUE));
        assertEquals(0L, storage.stored());
    }

    @Test
    void simulationDoesNotChangeBuffer() {
        LongEnergyStorage storage = new LongEnergyStorage(100L);
        assertEquals(100L, storage.receive(100L, true));
        assertEquals(0L, storage.stored());
        assertEquals(0L, storage.extract(10L, true));
        assertEquals(0L, storage.stored());
    }

    @Test
    void rejectsNegativeAmounts() {
        LongEnergyStorage storage = new LongEnergyStorage(100L);
        assertEquals(0L, storage.receive(-1L));
        assertEquals(0L, storage.extract(-1L));
        assertEquals(0L, storage.stored());
    }
}
