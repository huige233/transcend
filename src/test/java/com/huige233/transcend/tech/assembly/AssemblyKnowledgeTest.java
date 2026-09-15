package com.huige233.transcend.tech.assembly;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 验证机械知识必须逐级学习、制造同时受知识和熟练度限制，且前一级加工能够达到后一级门槛。 */
class AssemblyKnowledgeTest {
    @Test
    void requiresSequentialPermanentLearning() {
        for (int tier = 1; tier <= 7; tier++) {
            assertTrue(AssemblyKnowledge.canLearn(tier - 1, tier));
            assertFalse(AssemblyKnowledge.canLearn(tier, tier));
            assertFalse(AssemblyKnowledge.canLearn(tier, tier - 1));
        }
        assertFalse(AssemblyKnowledge.canLearn(0, 2));
        assertFalse(AssemblyKnowledge.canLearn(7, 8));
        assertFalse(AssemblyKnowledge.canLearn(0, 0));
    }

    @Test
    void knowledgeDoesNotReplaceProcessProficiency() {
        for (int tier = 1; tier <= 7; tier++) {
            int minimum = (tier - 1) * 15;
            assertEquals(minimum, AssemblyKnowledge.requiredProficiency(tier));
            assertTrue(AssemblyKnowledge.canManufacture(tier, minimum, tier));
            assertFalse(AssemblyKnowledge.canManufacture(tier - 1, 100, tier));
            assertFalse(AssemblyKnowledge.canManufacture(7, minimum - 1, tier));
        }
        assertFalse(AssemblyKnowledge.canManufacture(7, 100, 0));
        assertFalse(AssemblyKnowledge.canManufacture(7, 100, 8));
    }

    @Test
    void oldAndMalformedKnowledgeLevelsAreBounded() {
        assertEquals(0, AssemblyKnowledge.normalize(Integer.MIN_VALUE));
        assertEquals(0, AssemblyKnowledge.normalize(0));
        assertEquals(7, AssemblyKnowledge.normalize(Integer.MAX_VALUE));
        assertTrue(AssemblyKnowledge.canLearn(0, 1));
    }

    @Test
    void precedingTierCanReachTheNextProficiencyGate() {
        for (int tier = 2; tier <= 7; tier++) {
            int xp = AssemblyRules.xpForLevel(AssemblyKnowledge.requiredProficiency(tier));
            assertTrue(AssemblyRules.gain(xp - 1, tier - 1, Integer.MAX_VALUE) > 0);
            assertEquals(0, AssemblyRules.gain(xp, tier - 1, Integer.MAX_VALUE));
        }
    }
}
