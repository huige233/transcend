package com.huige233.transcend.tech.research;

import java.math.BigInteger;
import java.util.*;


/** 集中定义并查询各时代的能源、材料、自动化、战斗和知识研究节点及其费用与前置条件。 */
public final class ResearchRegistry {
    private static final Map<String, ResearchNode> NODES = new LinkedHashMap<>();
    static {
        add("energy_basics", ResearchTier.T0, List.of(), BigInteger.valueOf(1_000), 200, "energy_basic");
        add("materials_basics", ResearchTier.T0, List.of(), BigInteger.valueOf(1_200), 220, "materials_basic");
        add("automation_basics", ResearchTier.T0, List.of(), BigInteger.valueOf(1_500), 240, "automation_basic");
        add("automation", ResearchTier.T1, List.of("automation_basics"), BigInteger.valueOf(100_000), 600, "automation");
        add("energy_network", ResearchTier.T1, List.of("energy_basics"), BigInteger.valueOf(120_000), 650, "energy_network");
        add("material_processing", ResearchTier.T1, List.of("materials_basics"), BigInteger.valueOf(140_000), 700, "material_processing");
        add("energy_storage", ResearchTier.T2, List.of("energy_network"), BigInteger.TEN.pow(7), 1200, "energy_storage");
        add("advanced_materials", ResearchTier.T2, List.of("material_processing"), BigInteger.TEN.pow(7).multiply(BigInteger.valueOf(2)), 1300, "advanced_materials");
        add("precision_automation", ResearchTier.T2, List.of("automation"), BigInteger.TEN.pow(7).multiply(BigInteger.valueOf(3)), 1400, "precision_automation");
        add("singularity_engineering", ResearchTier.T3, List.of("energy_storage"), BigInteger.TEN.pow(9), 2400, "singularity");
        add("exotic_materials", ResearchTier.T3, List.of("advanced_materials"), BigInteger.TEN.pow(9).multiply(BigInteger.valueOf(2)), 2500, "exotic_materials");
        add("autonomous_factories", ResearchTier.T3, List.of("precision_automation"), BigInteger.TEN.pow(9).multiply(BigInteger.valueOf(3)), 2600, "autonomous_factories");
        add("combat_basics", ResearchTier.T0, List.of(), BigInteger.valueOf(1_800), 260, "combat_basics");
        add("knowledge_basics", ResearchTier.T0, List.of(), BigInteger.valueOf(2_000), 280, "knowledge_basics");
        add("advanced_combat", ResearchTier.T1, List.of("combat_basics"), BigInteger.valueOf(220_000), 820, "advanced_combat");
        add("advanced_knowledge", ResearchTier.T1, List.of("knowledge_basics"), BigInteger.valueOf(240_000), 840, "advanced_knowledge");
        add("quantum_combat", ResearchTier.T2, List.of("advanced_combat", "advanced_materials"), BigInteger.TEN.pow(7).multiply(BigInteger.valueOf(4)), 1500, "quantum_combat");
        add("quantum_knowledge", ResearchTier.T2, List.of("advanced_knowledge", "precision_automation"), BigInteger.TEN.pow(7).multiply(BigInteger.valueOf(5)), 1600, "quantum_knowledge");
        add("void_combat", ResearchTier.T3, List.of("quantum_combat", "singularity_engineering"), BigInteger.TEN.pow(9).multiply(BigInteger.valueOf(4)), 2800, "void_combat");
        add("strategic_knowledge", ResearchTier.T3, List.of("quantum_knowledge", "autonomous_factories"), BigInteger.TEN.pow(9).multiply(BigInteger.valueOf(5)), 3000, "strategic_knowledge");
        String[] ids = {"cosmic_forge", "spacetime_engineering", "dimensional_engineering", "civilization_engineering", "ultimate_universe"};
        ResearchTier[] tiers = {ResearchTier.T4, ResearchTier.T5, ResearchTier.T6, ResearchTier.T7, ResearchTier.T8};
        int power = 19;
        for (int i = 0; i < ids.length; i++, power += 7) {
            String base = ids[i];
            String energyPrereq = i == 0 ? "singularity_engineering" : ids[i - 1];
            String materialsPrereq = i == 0 ? "exotic_materials" : ids[i - 1] + "_materials";
            String automationPrereq = i == 0 ? "autonomous_factories" : ids[i - 1] + "_automation";
            add(base, tiers[i], List.of(energyPrereq), BigInteger.TEN.pow(power), 4800L << i, base);
            add(base + "_materials", tiers[i], List.of(materialsPrereq), BigInteger.TEN.pow(power).multiply(BigInteger.valueOf(2)), (4800L << i) + 200, base + "_materials");
            add(base + "_automation", tiers[i], List.of(automationPrereq), BigInteger.TEN.pow(power).multiply(BigInteger.valueOf(3)), (4800L << i) + 400, base + "_automation");
            add(base + "_combat", tiers[i], List.of(i == 0 ? "automation_basics" : ids[i - 1] + "_combat"), BigInteger.TEN.pow(power).multiply(BigInteger.valueOf(4)), (4800L << i) + 600, base + "_combat");
            add(base + "_knowledge", tiers[i], List.of(i == 0 ? "materials_basics" : ids[i - 1] + "_knowledge"), BigInteger.TEN.pow(power).multiply(BigInteger.valueOf(5)), (4800L << i) + 800, base + "_knowledge");
        }
        
    }
    private ResearchRegistry() {}
    private static void add(String id, ResearchTier tier, List<String> prerequisites, BigInteger cost, long ticks, String unlock) {
        if (NODES.putIfAbsent(id, new ResearchNode(id, tier, prerequisites, cost, ticks, List.of(unlock))) != null) throw new IllegalStateException("Duplicate research node: " + id);
    }
    public static ResearchNode get(String id) { return id == null ? null : NODES.get(id); }
    public static Collection<ResearchNode> all() { return Collections.unmodifiableCollection(NODES.values()); }
}
