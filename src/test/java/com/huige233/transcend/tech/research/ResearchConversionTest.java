package com.huige233.transcend.tech.research;

import com.huige233.transcend.items.tech.ShieldModule;
import com.huige233.transcend.tech.rank.TechRank;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ResearchConversionTest {
    @Test void pluginBatchingPreservesEnergyCostAndRejectsImpossibleBatches() {
        for (long power : new long[]{1, 10, 1000, 1_000_000, Long.MAX_VALUE}) {
            assertEquals(ResearchConversion.points(power) * ResearchEnergyAccount.RF_PER_POINT, ResearchConversion.energy(power));
            assertTrue(ResearchConversion.energy(power) > 0);
            assertEquals(0, ResearchConversion.batches(16, 64, ResearchConversion.energy(power) - 1, power, true));
            assertEquals(1, ResearchConversion.batches(16, 64, ResearchConversion.energy(power), power, true));
        }
        assertEquals(16, ResearchConversion.batches(16, 64, Long.MAX_VALUE, 1, true));
        assertEquals(1, ResearchConversion.batches(16, 64, Long.MAX_VALUE, 1, false));
        assertEquals(0, ResearchConversion.batches(-1, 64, Long.MAX_VALUE, 1, true));
    }

    @Test void everyEquipmentAndModuleUnlockExistsAndKnowledgeCannotExceedSeven() {
        for (TechRank rank : TechRank.values()) assertNotNull(ResearchRegistry.get(ResearchUnlocks.equipment(rank)));
        for (ShieldModule module : ShieldModule.values()) assertNotNull(ResearchRegistry.get(ResearchUnlocks.module(module)));
        assertEquals(0, ResearchUnlocks.knowledgeTier(Set.of("combat_basics")));
        assertEquals(1, ResearchUnlocks.knowledgeTier(Set.of("knowledge_basics")));
        assertEquals(7, ResearchUnlocks.knowledgeTier(ResearchRegistry.all().stream().map(ResearchNode::id).collect(java.util.stream.Collectors.toSet())));
    }
}
