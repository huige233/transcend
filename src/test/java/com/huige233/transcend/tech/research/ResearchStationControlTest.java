package com.huige233.transcend.tech.research;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;

/** 验证研究站批量能量兑换的原子性、手动启动扣点和付费进度在存档后的恢复与完成。 */
class ResearchStationControlTest {
    private static final long BATCH_RF = ResearchEnergyAccount.RF_PER_POINT * 10;

    @Test void failedBatchDoesNotConsumeEnergyOrChangePoints() {
        ResearchEnergyAccount account = new ResearchEnergyAccount();
        account.receive(BATCH_RF - 1, false);
        assertFalse(account.convertExact(BATCH_RF));
        assertEquals(BATCH_RF - 1, account.stored());
        assertEquals(BigInteger.ZERO, account.points());
        assertEquals(0, account.remainder());
    }

    @Test void insufficientPointCapacityRejectsEntireBatch() {
        ResearchEnergyAccount account = new ResearchEnergyAccount();
        CompoundTag tag = new CompoundTag();
        tag.putInt("AccountingVersion", 1);
        tag.putLong("Energy", BATCH_RF);
        BigInteger balance = BigInteger.TEN.pow(64).subtract(BigInteger.valueOf(6));
        tag.putString("Points", balance.toString());
        tag.putLong("Remainder", 123);
        account.load(tag);
        assertFalse(account.convertExact(BATCH_RF));
        assertEquals(BATCH_RF, account.stored());
        assertEquals(balance, account.points());
        assertEquals(123, account.remainder());
    }

    @Test void oneBatchCreatesTenPointsAndPreservesRemainder() {
        ResearchEnergyAccount account = new ResearchEnergyAccount();
        account.receive(BATCH_RF + 321, false);
        account.convert(321);
        assertTrue(account.convertExact(BATCH_RF));
        assertEquals(BigInteger.TEN, account.points());
        assertEquals(321, account.remainder());
        assertEquals(0, account.stored());
        assertFalse(account.convertExact(BATCH_RF));
    }

    @Test void preparedPointsAreNotSpentBeforeManualStart() {
        ResearchEnergyAccount account = new ResearchEnergyAccount();
        account.receive(BATCH_RF, false);
        assertTrue(account.convertExact(BATCH_RF));
        ResearchProgress progress = new ResearchProgress();
        assertFalse(progress.advance(account));
        assertEquals(BigInteger.TEN, account.points());
        assertTrue(progress.canStartOrResume("energy_basics", account.points()));
        assertTrue(progress.start("energy_basics"));
        assertTrue(progress.advance(account));
        assertEquals(BigInteger.valueOf(5), progress.paid());
        assertEquals(BigInteger.valueOf(5), account.points());
    }

    @Test void rfAloneDoesNotProduceResearchOrAdvanceTime() {
        ResearchEnergyAccount account = new ResearchEnergyAccount();
        account.receive(Long.MAX_VALUE, false);
        ResearchProgress progress = new ResearchProgress();
        assertFalse(progress.canStartOrResume("energy_basics", account.points()));
        assertTrue(progress.start("energy_basics"));
        assertFalse(progress.advance(account));
        assertEquals(0, progress.ticks());
        assertEquals(BigInteger.ZERO, progress.paid());
        assertEquals(Long.MAX_VALUE, account.stored());
    }

    @Test void resumeAfterReloadKeepsPaidPointsAndTicksWithoutNewBalance() {
        ResearchProgress progress = new ResearchProgress();
        assertTrue(progress.start("energy_basics"));
        progress.addPoints(BigInteger.TEN);
        progress.tick();
        CompoundTag tag = new CompoundTag();
        progress.save(tag);
        ResearchProgress restored = new ResearchProgress();
        restored.load(tag);
        assertTrue(restored.canStartOrResume("energy_basics", BigInteger.ZERO));
        assertFalse(restored.canStartOrResume("materials_basics", BigInteger.TEN));
        assertEquals(BigInteger.TEN, restored.paid());
        assertEquals(1, restored.ticks());
    }

    @Test void fullyFundedResearchCompletesWithEmptyStationBalance() {
        ResearchProgress progress = new ResearchProgress();
        assertTrue(progress.start("energy_basics"));
        progress.addPoints(ResearchRegistry.get("energy_basics").cost());
        ResearchEnergyAccount account = new ResearchEnergyAccount();
        for (int i = 0; i < ResearchRegistry.get("energy_basics").durationTicks(); i++)
            assertTrue(progress.advance(account));
        assertNull(progress.active());
        assertTrue(progress.completed().contains("energy_basics"));
        assertEquals(BigInteger.ZERO, account.points());
    }
}
