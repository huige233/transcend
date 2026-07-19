package com.huige233.transcend.ascension.tree;

import com.huige233.transcend.ascension.AscensionStatBlock;
import com.huige233.transcend.ascension.MageClass;
import io.netty.buffer.Unpooled;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TreeRegistryTest {
    private final TreeRegistry registry = TreeRegistry.getInstance();

    @AfterEach
    void clearRegistry() {
        registry.clear();
    }

    @Test
    void jsonNodeStatsHaveNoHiddenMultiplier() {
        assertEquals(1.0F, TreeRegistry.NODE_STAT_GLOBAL_MULT);
    }

    @Test
    void omniscientAveragesEveryAvailableCanonicalScalingMap() {
        Map<String, Map<StatType, Float>> scaling = new LinkedHashMap<>();
        scaling.put("fire", Map.of(StatType.SPELL_POWER_BONUS, 0.2F));
        scaling.put("water", Map.of(StatType.SPELL_POWER_BONUS, 0.4F, StatType.BONUS_MAX_HEALTH, 10.0F));
        registry.replaceTalentTrees(List.of(tree(MageClass.OMNISCIENT,
                Map.of("scaled", node("scaled", List.of(), scaling)))));

        AscensionStatBlock stats = registry.computeNodeStats(Set.of("scaled"), MageClass.OMNISCIENT);

        assertEquals(0.3F, stats.spellPowerBonus, 0.0001F);
        assertEquals(5.0F, stats.bonusMaxHealth, 0.0001F);
    }

    @Test
    void activePassivesFollowNodeIdOrder() {
        PassiveEffect fromA = new PassiveEffect.DamageReduction(0.1F);
        PassiveEffect fromB = new PassiveEffect.DamageReduction(0.2F);
        Map<String, NodeDefinition> nodes = new LinkedHashMap<>();
        nodes.put("b_node", node("b_node", List.of(fromB), Map.of()));
        nodes.put("a_node", node("a_node", List.of(fromA), Map.of()));
        registry.replaceTalentTrees(List.of(tree(nodes)));

        assertEquals(List.of(fromA, fromB), registry.getActivePassives(
                Set.of("b_node", "a_node"), MageClass.PYROMANCER));
    }

    @Test
    void movedForeignNodesStopApplyingWithoutDeletingRecordedIds() {
        NodeDefinition foreign = new NodeDefinition("moved", 1, 1, ChatFormatting.WHITE,
                "test.name", "test.desc", List.of(), 0, 0,
                Map.of(StatType.SPELL_POWER_BONUS, 0.4F),
                List.of(new PassiveEffect.Dodge(0.1F)), Map.of());
        registry.replaceTalentTrees(List.of(tree(MageClass.PYROMANCER, Map.of("moved", foreign))));

        assertEquals(0.0F, registry.computeNodeStats(Set.of("moved"), MageClass.CRYOMANCER)
                .spellPowerBonus);
        assertEquals(List.of(), registry.getActivePassives(Set.of("moved"), MageClass.CRYOMANCER));
        assertEquals(0.4F, registry.computeNodeStats(Set.of("moved"), MageClass.PYROMANCER)
                .spellPowerBonus);
    }

    @Test
    void rejectsInvalidCostsTiersStatsChancesAndParentGraphs() {
        assertThrows(IllegalArgumentException.class, () -> registry.replaceTalentTrees(List.of(
                tree(Map.of("bad", rawNode("bad", 1, -1, List.of(), Float.NaN,
                        List.of()))))));
        assertThrows(IllegalArgumentException.class, () -> registry.replaceTalentTrees(List.of(
                tree(Map.of("bad", rawNode("bad", 0, 1, List.of(), 0.0F,
                        List.of()))))));
        assertThrows(IllegalArgumentException.class, () -> registry.replaceTalentTrees(List.of(
                tree(Map.of("bad", rawNode("bad", 1, 1, List.of(), 0.0F,
                        List.of(new PassiveEffect.Dodge(1.01F))))))));

        Map<String, NodeDefinition> badParents = new LinkedHashMap<>();
        badParents.put("parent", rawNode("parent", 2, 1, List.of(), 0.0F, List.of()));
        badParents.put("child", rawNode("child", 1, 1, List.of("parent"), 0.0F, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> registry.replaceTalentTrees(List.of(tree(badParents))));

        Map<String, NodeDefinition> cycle = new LinkedHashMap<>();
        cycle.put("a", rawNode("a", 1, 1, List.of("b"), 0.0F, List.of()));
        cycle.put("b", rawNode("b", 2, 1, List.of("a"), 0.0F, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> registry.replaceTalentTrees(List.of(tree(cycle))));
    }

    @Test
    void networkRoundTripPreservesPassiveEffects() {
        List<PassiveEffect> passives = List.of(
                new PassiveEffect.DamageReduction(0.12F),
                new PassiveEffect.ManaFreeCast(0.08F),
                new PassiveEffect.Undying(0.5F, 1200));
        NodeDefinition original = node("networked", passives,
                Map.of("chaos", Map.of(StatType.REACTION_BONUS, 0.15F)));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        original.write(buffer);
        NodeDefinition decoded = NodeDefinition.read(buffer);

        assertEquals(passives, decoded.getPassiveEffects());
        assertEquals(original.getAllElementScaling(), decoded.getAllElementScaling());
    }

    private static TreeDefinition tree(Map<String, NodeDefinition> nodes) {
        return tree(MageClass.PYROMANCER, nodes);
    }

    private static TreeDefinition tree(MageClass mageClass, Map<String, NodeDefinition> nodes) {
        return new TreeDefinition(new ResourceLocation("transcend", "test"), TreeType.TALENT,
                mageClass, "test.name", "test.desc", 0xFFFFFF, nodes);
    }

    private static NodeDefinition node(String id, List<PassiveEffect> passives,
                                       Map<String, Map<StatType, Float>> scaling) {
        return new NodeDefinition(id, 1, 1, ChatFormatting.WHITE, "test.name", "test.desc",
                List.of(), 0, 0, Map.of(), passives, scaling);
    }

    private static NodeDefinition rawNode(String id, int tier, int cost, List<String> parents,
                                          float spellPower, List<PassiveEffect> passives) {
        return new NodeDefinition(id, tier, cost, ChatFormatting.WHITE, "test.name", "test.desc",
                parents, 0, 0, Map.of(StatType.SPELL_POWER_BONUS, spellPower), passives, Map.of());
    }
}
