package com.huige233.transcend.tech.research;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;

/** 验证研究树的分层前置条件、乱序存档拓扑恢复、进度清洗和大整数研究费用。 */
class ResearchCoreTest {
    @Test void registryHasThreeBranchesAndBranchPrerequisites() {
        for (ResearchTier tier : ResearchTier.values()) {
            assertEquals(5, ResearchRegistry.all().stream().filter(n -> n.core() && n.tier() == tier).count());
        }
        assertEquals("cosmic_forge", ResearchRegistry.get("spacetime_engineering").prerequisites().get(0));
        assertEquals("cosmic_forge_materials", ResearchRegistry.get("spacetime_engineering_materials").prerequisites().get(0));
    }
    @Test void scrambledNbtReconstructsTopology() {
        CompoundTag tag = new CompoundTag(); ListTag list = new ListTag();
        list.add(StringTag.valueOf("energy_network")); list.add(StringTag.valueOf("energy_basics")); list.add(StringTag.valueOf("materials_basics")); list.add(StringTag.valueOf("automation_basics")); list.add(StringTag.valueOf("combat_basics")); list.add(StringTag.valueOf("knowledge_basics"));
        tag.put("Completed", list); ResearchProgress p = new ResearchProgress(); p.load(tag);
        assertTrue(p.completed().contains("energy_basics")); assertTrue(p.completed().contains("energy_network"));
    }
    @Test void completedActiveIsRejectedAndValuesClamped() {
        CompoundTag tag = new CompoundTag(); ListTag list = new ListTag(); list.add(StringTag.valueOf("energy_basics")); tag.put("Completed", list);
        tag.putString("Active", "energy_basics"); tag.putString("Paid", "9999999999999999999999999999999999999999999999999999999999999999"); tag.putLong("Ticks", Long.MAX_VALUE);
        ResearchProgress p = new ResearchProgress(); p.load(tag); assertNull(p.active()); assertEquals(BigInteger.ZERO, p.paid());
    }
    @Test void overpayReturnsOnlyAcceptedPoints() {
        ResearchProgress p = new ResearchProgress(); assertTrue(p.start("energy_basics"));
        assertEquals(BigInteger.valueOf(1000), p.addPoints(BigInteger.valueOf(5000))); assertEquals(BigInteger.ZERO, p.addPoints(BigInteger.ONE));
    }
    @Test void allPriorTierNodesAreRequired() {
        assertFalse(ResearchService.isUnlocked(SetUtil.of("energy_basics","materials_basics","automation_basics","combat_basics","knowledge_basics","automation","energy_network"), "energy_storage"));
        assertTrue(ResearchService.isUnlocked(SetUtil.of("energy_basics","materials_basics","automation_basics","combat_basics","knowledge_basics","automation","energy_network","material_processing","advanced_combat","advanced_knowledge"), "energy_storage"));
    }
    @Test void laterCostsHavePlayableStepsAndOptionalTopicsDoNotBlockEras() {
        assertEquals(BigInteger.TEN.pow(8), ResearchRegistry.get("cosmic_forge").cost());
        assertEquals(BigInteger.TEN.pow(12).multiply(BigInteger.valueOf(5)), ResearchRegistry.get("ultimate_universe_knowledge").cost());
        var coreBeforeT2 = ResearchRegistry.all().stream().filter(n -> n.core() && n.tier().index() < 2)
                .map(ResearchNode::id).collect(java.util.stream.Collectors.toSet());
        assertTrue(ResearchService.isUnlocked(coreBeforeT2, "energy_storage"));
        assertFalse(coreBeforeT2.contains("shield_combat_magic"));
    }
    @Test void entireCatalogCanBeReachedWithoutCircularEraRequirements() {
        var completed = new java.util.HashSet<String>();
        boolean changed;
        do {
            changed = false;
            for (var node : ResearchRegistry.all()) if (ResearchService.isUnlocked(completed, node.id())) changed |= completed.add(node.id());
        } while (changed);
        assertEquals(68, completed.size());
    }
    /** 将可变数量的研究节点标识构造为不可变集合，供解锁前置条件测试使用。 */
    private static final class SetUtil { static java.util.Set<String> of(String... values) { return java.util.Set.of(values); } }
}
