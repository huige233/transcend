package com.huige233.transcend.entity.boss;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossSpellTierTest {
    private static final Path BOSS_ROOT = Path.of(
            "src/main/java/com/huige233/transcend/entity/boss");

    @Test
    void baseTierGainsPhaseOrdinalAndCapsAtTwelve() {
        assertEquals(3, BossSpellTier.calculate(3, BossPhase.PHASE_1.ordinal()));
        assertEquals(7, BossSpellTier.calculate(6, BossPhase.PHASE_2.ordinal()));
        assertEquals(11, BossSpellTier.calculate(9, BossPhase.PHASE_3.ordinal()));
        assertEquals(12, BossSpellTier.calculate(9, BossPhase.PHASE_4.ordinal()));
    }

    @Test
    void tierFormulaRemainsBasePlusPhaseOrdinalCappedAtTwelve() throws IOException {
        String source = Files.readString(BOSS_ROOT.resolve("BossSpellTier.java"));

        assertTrue(source.contains("return Math.min(12, baseTier + phaseOrdinal);"));
    }

    @Test
    void bossesDeclareTheirAssignedBaseTiers() throws IOException {
        assertBaseTier("ElementalWarden.java", 3);
        assertBaseTier("VoidWeaver.java", 6);
        assertBaseTier("TranscendenceAvatar.java", 9);
    }

    @Test
    void everyBossProjectileUsesCalculatedTier() throws IOException {
        String source = Files.readString(BOSS_ROOT.resolve("AbstractTranscendBoss.java"));
        int projectileCount = source.split("new SpellProjectile", -1).length - 1;
        int tierCount = source.split("this.getBossSpellTier\\(\\)", -1).length - 1;

        assertEquals(4, projectileCount);
        assertEquals(projectileCount, tierCount);
        assertFalse(source.contains("scaledPower, 1)"));
    }

    private static void assertBaseTier(String file, int tier) throws IOException {
        String source = Files.readString(BOSS_ROOT.resolve(file));
        assertTrue(source.contains("protected int getBaseSpellTier() { return " + tier + "; }"), file);
    }
}
