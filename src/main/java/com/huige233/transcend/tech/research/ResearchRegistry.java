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
        add("energy_storage", ResearchTier.T2, List.of("energy_network"), BigInteger.TEN.pow(6), 1200, "energy_storage");
        add("advanced_materials", ResearchTier.T2, List.of("material_processing"), BigInteger.TEN.pow(6).multiply(BigInteger.valueOf(2)), 1300, "advanced_materials");
        add("precision_automation", ResearchTier.T2, List.of("automation"), BigInteger.TEN.pow(6).multiply(BigInteger.valueOf(3)), 1400, "precision_automation");
        add("singularity_engineering", ResearchTier.T3, List.of("energy_storage"), BigInteger.TEN.pow(7), 2400, "singularity");
        add("exotic_materials", ResearchTier.T3, List.of("advanced_materials"), BigInteger.TEN.pow(7).multiply(BigInteger.valueOf(2)), 2500, "exotic_materials");
        add("autonomous_factories", ResearchTier.T3, List.of("precision_automation"), BigInteger.TEN.pow(7).multiply(BigInteger.valueOf(3)), 2600, "autonomous_factories");
        add("combat_basics", ResearchTier.T0, List.of(), BigInteger.valueOf(1_800), 260, "combat_basics");
        add("knowledge_basics", ResearchTier.T0, List.of(), BigInteger.valueOf(2_000), 280, "knowledge_basics");
        add("advanced_combat", ResearchTier.T1, List.of("combat_basics"), BigInteger.valueOf(220_000), 820, "advanced_combat");
        add("advanced_knowledge", ResearchTier.T1, List.of("knowledge_basics"), BigInteger.valueOf(240_000), 840, "advanced_knowledge");
        add("quantum_combat", ResearchTier.T2, List.of("advanced_combat", "advanced_materials"), BigInteger.TEN.pow(6).multiply(BigInteger.valueOf(4)), 1500, "quantum_combat");
        add("quantum_knowledge", ResearchTier.T2, List.of("advanced_knowledge", "precision_automation"), BigInteger.TEN.pow(6).multiply(BigInteger.valueOf(5)), 1600, "quantum_knowledge");
        add("void_combat", ResearchTier.T3, List.of("quantum_combat", "singularity_engineering"), BigInteger.TEN.pow(7).multiply(BigInteger.valueOf(4)), 2800, "void_combat");
        add("strategic_knowledge", ResearchTier.T3, List.of("quantum_knowledge", "autonomous_factories"), BigInteger.TEN.pow(7).multiply(BigInteger.valueOf(5)), 3000, "strategic_knowledge");
        String[] ids = {"cosmic_forge", "spacetime_engineering", "dimensional_engineering", "civilization_engineering", "ultimate_universe"};
        ResearchTier[] tiers = {ResearchTier.T4, ResearchTier.T5, ResearchTier.T6, ResearchTier.T7, ResearchTier.T8};
        int power = 8;
        for (int i = 0; i < ids.length; i++, power++) {
            String base = ids[i];
            String energyPrereq = i == 0 ? "singularity_engineering" : ids[i - 1];
            String materialsPrereq = i == 0 ? "exotic_materials" : ids[i - 1] + "_materials";
            String automationPrereq = i == 0 ? "autonomous_factories" : ids[i - 1] + "_automation";
            add(base, tiers[i], List.of(energyPrereq), BigInteger.TEN.pow(power), 4800L << i, base);
            add(base + "_materials", tiers[i], List.of(materialsPrereq), BigInteger.TEN.pow(power).multiply(BigInteger.valueOf(2)), (4800L << i) + 200, base + "_materials");
            add(base + "_automation", tiers[i], List.of(automationPrereq), BigInteger.TEN.pow(power).multiply(BigInteger.valueOf(3)), (4800L << i) + 400, base + "_automation");
            add(base + "_combat", tiers[i], List.of(i == 0 ? "void_combat" : ids[i - 1] + "_combat"), BigInteger.TEN.pow(power).multiply(BigInteger.valueOf(4)), (4800L << i) + 600, base + "_combat");
            add(base + "_knowledge", tiers[i], List.of(i == 0 ? "strategic_knowledge" : ids[i - 1] + "_knowledge"), BigInteger.TEN.pow(power).multiply(BigInteger.valueOf(5)), (4800L << i) + 800, base + "_knowledge");
        }
        for (String category : List.of("melee", "projectile", "explosion", "fire", "magic", "environment")) {
            specialization("shield_combat_" + category, ResearchTier.T1,
                    List.of("advanced_combat", "energy_network"), 50_000, 600, "shield_module_b_" + category);
        }
        specialization("shield_combat_mechanical", ResearchTier.T2, List.of("quantum_combat", "advanced_materials"), 1_500_000, 1800, "shield_module_t5_mechanical");
        specialization("shield_combat_spectrum_a", ResearchTier.T2, List.of("quantum_combat", "advanced_materials"), 1_000_000, 1800, "shield_module_a_omni");
        specialization("shield_combat_spectrum_s", ResearchTier.T3, List.of("void_combat", "singularity_engineering"), 10_000_000, 3600, "shield_module_s_omni");
        specialization("shield_combat_spectrum_ex", ResearchTier.T4, List.of("cosmic_forge_combat", "cosmic_forge"), 100_000_000, 7200, "shield_module_ex_omni");
        specialization("energy_capacitor_design", ResearchTier.T0, List.of("energy_basics"), 1_500, 240, "long_storage_basic");
        specialization("phase_optics_materials", ResearchTier.T0, List.of("materials_basics"), 1_800, 280, "tech_part_lens_phase");
        specialization("research_processor_automation", ResearchTier.T0, List.of("automation_basics", "knowledge_basics"), 2_000, 300, "research_processor");
        specialization("energy_storage_advanced", ResearchTier.T1, List.of("energy_network", "energy_capacitor_design"), 100_000, 800, "long_storage_advanced");
        specialization("precision_components_materials", ResearchTier.T1, List.of("material_processing"), 120_000, 900, "tech_part_capacitor");
        specialization("control_systems_automation", ResearchTier.T1, List.of("automation"), 100_000, 800, "tech_core_pilot");
        specialization("energy_storage_quantum", ResearchTier.T2, List.of("energy_storage", "energy_storage_advanced"), 1_000_000, 1800, "long_storage_quantum");
        specialization("quantum_computing_automation", ResearchTier.T2, List.of("precision_automation", "advanced_materials"), 1_500_000, 2000, "research_quantum_computer");
        specialization("singularity_containment_materials", ResearchTier.T3, List.of("exotic_materials", "singularity_engineering"), 10_000_000, 3000, "magnetic_confinement_container");
        specialization("singularity_breeding_automation", ResearchTier.T3, List.of("autonomous_factories", "singularity_containment_materials"), 15_000_000, 3600, "black_hole_seed_breeder");
        specialization("energy_micro_universe", ResearchTier.T4, List.of("cosmic_forge", "singularity_breeding_automation"), 100_000_000, 6000, "mini_universe_generator");
        specialization("cosmic_simulation_automation", ResearchTier.T4, List.of("cosmic_forge_automation", "cosmic_forge_knowledge"), 150_000_000, 7200, "research_cosmic_simulator");
        specialization("energy_phantom_storage", ResearchTier.T5, List.of("spacetime_engineering", "energy_storage_quantum"), 1_000_000_000, 9600, "long_storage_phantom");
        validate();
    }
    private ResearchRegistry() {}
    private static void add(String id, ResearchTier tier, List<String> prerequisites, BigInteger cost, long ticks, String unlock) {
        if (NODES.putIfAbsent(id, new ResearchNode(id, tier, prerequisites, cost, ticks, List.of(unlock))) != null) throw new IllegalStateException("Duplicate research node: " + id);
    }
    private static void specialization(String id, ResearchTier tier, List<String> prerequisites, long cost, long ticks, String unlock) {
        if (NODES.putIfAbsent(id, new ResearchNode(id, tier, prerequisites, BigInteger.valueOf(cost), ticks, List.of(unlock), false)) != null)
            throw new IllegalStateException("Duplicate research node: " + id);
    }
    private static void validate() {
        Set<String> reachable = new HashSet<>();
        boolean changed;
        do {
            changed = false;
            for (ResearchNode node : NODES.values()) {
                for (String id : node.prerequisites()) if (!NODES.containsKey(id)) throw new IllegalStateException("Unknown research prerequisite: " + id);
                if (reachable.containsAll(node.prerequisites())) changed |= reachable.add(node.id());
            }
        } while (changed);
        if (reachable.size() != NODES.size()) throw new IllegalStateException("Cyclic research prerequisites");
    }
    public static ResearchNode get(String id) { return id == null ? null : NODES.get(id); }
    public static Collection<ResearchNode> all() { return Collections.unmodifiableCollection(NODES.values()); }
}
