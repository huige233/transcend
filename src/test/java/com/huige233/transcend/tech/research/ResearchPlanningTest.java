package com.huige233.transcend.tech.research;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ResearchPlanningTest {
    @Test void effectiveRequirementsPreserveEraAndExplicitGatesForEveryTopic() {
        for (ResearchNode target : ResearchRegistry.all()) {
            Set<String> required = new HashSet<>(target.prerequisites());
            for (ResearchNode earlier : ResearchRegistry.all()) {
                if (earlier.core() && earlier.tier().index() < target.tier().index()) required.add(earlier.id());
            }
            assertEquals(required, new HashSet<>(ResearchPlanning.prerequisites(target.id()).stream().map(ResearchNode::id).toList()), target.id());
            assertTrue(ResearchService.isUnlocked(required, target.id()), target.id());
            for (String id : required) {
                Set<String> incomplete = new HashSet<>(required);
                incomplete.remove(id);
                assertFalse(ResearchService.isUnlocked(incomplete, target.id()), target.id() + " must require " + id);
            }
        }
    }

    @Test void navigationReachesEveryTopicThroughActuallyUnlockedDependencies() {
        for (ResearchNode target : ResearchRegistry.all()) {
            Set<String> completed = new HashSet<>();
            for (int step = 0; !ResearchService.isUnlocked(completed, target.id()); step++) {
                assertTrue(step < ResearchRegistry.all().size(), "Navigation loop: " + target.id());
                ResearchNode next = ResearchPlanning.nextPrerequisite(completed, target.id(), "");
                assertNotNull(next, "No next step for " + target.id());
                assertFalse(completed.contains(next.id()));
                assertTrue(ResearchService.canStart(completed, next.id(), null), next.id());
                completed.add(next.id());
            }
            assertNull(ResearchPlanning.nextPrerequisite(completed, target.id(), ""));
        }
    }

    @Test void implicitCrossBranchRequirementsAndOptionalDependenciesAreVisible() {
        var missing = ResearchPlanning.missingPrerequisites(Set.of("energy_basics"), "energy_network");
        assertEquals(Set.of("materials_basics", "automation_basics", "combat_basics", "knowledge_basics"),
                new HashSet<>(missing.stream().map(ResearchNode::id).toList()));
        assertTrue(ResearchPlanning.prerequisites("energy_storage_quantum").stream().anyMatch(n -> n.id().equals("energy_storage_advanced")));
        assertFalse(ResearchPlanning.prerequisites("energy_storage_quantum").stream().anyMatch(n -> n.id().equals("shield_combat_melee")));
    }

    @Test void existingTopicTakesPriorityButCompletedAndUnknownTargetsDoNotNavigate() {
        assertEquals("materials_basics", ResearchPlanning.nextPrerequisite(Set.of(), "energy_storage", "materials_basics").id());
        assertNull(ResearchPlanning.nextPrerequisite(Set.of("energy_storage"), "energy_storage", "materials_basics"));
        assertNull(ResearchPlanning.nextPrerequisite(Set.of(), "unknown", "materials_basics"));
    }

    @Test void fundingSubtractsPaidPointsBalanceAndStoredRfAndRoundsManualBatches() {
        var funding = ResearchPlanning.funding(BigInteger.valueOf(1000), BigInteger.valueOf(900), BigInteger.valueOf(89), 50_000, 10, true);
        assertEquals(BigInteger.valueOf(11), funding.missingPoints());
        assertEquals(BigInteger.valueOf(2), funding.inputPairs());
        assertEquals(BigInteger.valueOf(1_050_000), funding.automaticExternalRf());
        assertEquals(BigInteger.valueOf(1_950_000), funding.manualExternalRf());
        var covered = ResearchPlanning.funding(BigInteger.TEN, BigInteger.ONE, BigInteger.TEN, 0, 10, true);
        assertEquals(BigInteger.ZERO, covered.missingPoints());
        assertEquals(BigInteger.ZERO, covered.inputPairs());
        assertEquals(BigInteger.ZERO, covered.automaticExternalRf());
        assertEquals(BigInteger.ZERO, covered.manualExternalRf());
    }

    @Test void fundingHandlesLargeCostsAndDoesNotTreatSavedFractionsAsSpendableRf() {
        BigInteger cost = BigInteger.TEN.pow(40);
        var large = ResearchPlanning.funding(cost, BigInteger.ZERO, BigInteger.ZERO, 0, 10_000_000, false);
        assertEquals(cost.multiply(BigInteger.valueOf(100_000)), large.automaticExternalRf());
        assertEquals(large.automaticExternalRf(), large.manualExternalRf());
        var almost = ResearchPlanning.funding(BigInteger.ONE, BigInteger.ZERO, BigInteger.ZERO, 99_999, 10, true);
        assertEquals(BigInteger.ONE, almost.automaticExternalRf());
        assertEquals(BigInteger.valueOf(900_001), almost.manualExternalRf());
        var invalid = ResearchPlanning.funding(BigInteger.ONE, BigInteger.valueOf(-1), BigInteger.valueOf(-2), -1, 0, false);
        assertEquals(BigInteger.valueOf(100_000), invalid.automaticExternalRf());
        for (String text : new String[] {null, "", "invalid", "-3"}) assertEquals(BigInteger.ZERO, ResearchPlanning.nonNegative(text));
        assertEquals(cost, ResearchPlanning.nonNegative(cost.toString()));
    }

    @Test void newTopicWithNoPointsBudgetsAFullStartupBatchBeforeAutomaticPreparation() {
        var fresh = ResearchPlanning.funding(BigInteger.valueOf(1000), BigInteger.ZERO, BigInteger.ZERO, 100_000_000, 10_000_000, false);
        assertEquals(BigInteger.valueOf(999_900_000_000L), fresh.automaticExternalRf());
        assertEquals(fresh.manualExternalRf(), fresh.automaticExternalRf());
        var running = ResearchPlanning.funding(BigInteger.valueOf(1000), BigInteger.ZERO, BigInteger.ZERO, 100_000_000, 10_000_000, true);
        assertEquals(BigInteger.ZERO, running.automaticExternalRf());
        var ready = ResearchPlanning.funding(BigInteger.valueOf(1000), BigInteger.ZERO, BigInteger.ONE, 0, 10_000_000, false);
        assertEquals(BigInteger.valueOf(99_900_000), ready.automaticExternalRf());
    }
}
