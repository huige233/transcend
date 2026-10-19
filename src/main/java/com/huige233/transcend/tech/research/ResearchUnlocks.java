package com.huige233.transcend.tech.research;

import com.huige233.transcend.items.tech.*;
import com.huige233.transcend.tech.assembly.AssemblyKnowledge;
import com.huige233.transcend.tech.core.TechItemData;
import com.huige233.transcend.tech.rank.TechRank;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;

/** 制造校验与研究界面共用同一份成果映射；研究不会限制已获得装备的使用。 */
public final class ResearchUnlocks {
    private static final Map<ResourceLocation, String> PRODUCTS;
    static {
        Map<ResourceLocation, String> products = new LinkedHashMap<>();
        products(products, "energy_basics", "wind_generator", "standard_capacitor");
        products(products, "materials_basics", "tech_part_gear_refined", "tech_part_gear");
        products(products, "automation_basics", "tech_part_circuit_wafer", "tech_part_circuit");
        products(products, "energy_capacitor_design", "portable_capacitor", "long_storage_basic");
        products(products, "phase_optics_materials", "tech_part_glass_phase", "tech_part_lens_phase", "tech_part_focusing");
        products(products, "research_processor_automation", "research_processor");
        products(products, "energy_storage_advanced", "portable_capacitor_advanced", "long_storage_advanced");
        products(products, "material_processing", "tech_part_capacitor_core");
        products(products, "precision_components_materials", "tech_part_capacitor");
        products(products, "control_systems_automation", "tech_control_core");
        products(products, "dimensional_engineering_materials", "tech_core_pilot");
        products(products, "energy_storage", "tech_energy_core");
        products(products, "energy_storage_quantum", "portable_capacitor_ghost", "long_storage_quantum");
        products(products, "quantum_computing_automation", "research_quantum_computer");
        products(products, "singularity_engineering", "black_hole_seed");
        products(products, "singularity_containment_materials", "magnetic_confinement_container");
        products(products, "singularity_breeding_automation", "black_hole_seed_breeder");
        products(products, "cosmic_forge", "tech_energy_matrix");
        products(products, "energy_micro_universe", "mini_universe_generator");
        products(products, "cosmic_simulation_automation", "research_cosmic_simulator");
        products(products, "energy_phantom_storage", "long_storage_phantom");
        for (String form : List.of("metal", "plate", "dust")) {
            for (String metal : com.huige233.transcend.tech.assembly.TechMaterials.METALS) {
                String path = "tech_" + form + "_" + metal;
                products(products, com.huige233.transcend.tech.assembly.TechMaterials.research(path), path);
            }
        }
        PRODUCTS = Collections.unmodifiableMap(products);
    }
    private static final List<String> KNOWLEDGE = List.of("knowledge_basics", "advanced_knowledge", "quantum_knowledge",
            "strategic_knowledge", "cosmic_forge_knowledge", "spacetime_engineering_knowledge", "dimensional_engineering_knowledge");
    private ResearchUnlocks() {}

    private static void products(Map<ResourceLocation, String> products, String research, String... paths) {
        for (String path : paths) {
            if (products.putIfAbsent(new ResourceLocation("transcend", path), research) != null)
                throw new IllegalStateException("Duplicate research product: " + path);
        }
    }

    public static String required(ResourceLocation item) { return PRODUCTS.get(item); }

    public static List<ResourceLocation> products(String research) {
        return PRODUCTS.entrySet().stream().filter(entry -> entry.getValue().equals(research)).map(Map.Entry::getKey).toList();
    }

    public static String equipment(TechRank rank) {
        return switch (rank) {
            case E, D -> "combat_basics";
            case C, B -> "advanced_combat";
            case A -> "quantum_combat";
            case S -> "void_combat";
            case EX -> "cosmic_forge_combat";
        };
    }

    public static String module(ShieldModule module) {
        return switch (module) {
            case BASIC, REINFORCED, EFFICIENT -> "advanced_combat";
            case T5_MECHANICAL -> "shield_combat_mechanical";
            case B_MELEE, B_PROJECTILE, B_EXPLOSION, B_FIRE, B_MAGIC, B_ENVIRONMENT -> "shield_combat_" + module.id().substring(2);
            case A_OMNI -> "shield_combat_spectrum_a";
            case S_OMNI -> "shield_combat_spectrum_s";
            case EX_OMNI -> "shield_combat_spectrum_ex";
        };
    }

    public static String required(ItemStack output) {
        if (output.isEmpty()) return null;
        if (output.getItem() instanceof PhaseShield || output.getItem() instanceof ParticleGun) return equipment(TechItemData.getRank(output));
        if (output.getItem() instanceof ShieldModuleItem) {
            ShieldModule module = ShieldModuleItem.getModule(output);
            return module == null ? null : module(module);
        }
        if (output.getItem() instanceof SiriusModuleItem) return "quantum_combat";
        if (output.getItem() instanceof GunModuleItem) {
            GunModule module = GunModuleItem.getModule(output);
            return module == GunModule.MUZZLE_SIRIUS ? "quantum_combat"
                    : module == GunModule.AMMO_STD || module == GunModule.BARREL_STD ? "combat_basics" : "advanced_combat";
        }
        return required(BuiltInRegistries.ITEM.getKey(output.getItem()));
    }

    public static boolean canManufacture(Player player, ItemStack output) {
        String id = required(output);
        return id == null || player.getCapability(ResearchCapabilities.PROGRESS)
                .map(progress -> progress.completed().contains(id)).orElse(false);
    }

    public static int knowledgeTier(Set<String> completed) {
        int result = 0;
        for (int i = 0; i < KNOWLEDGE.size(); i++) if (completed.contains(KNOWLEDGE.get(i))) result = i + 1;
        return result;
    }

    public static void applyKnowledge(Player player, Set<String> completed) {
        if (player.level().isClientSide) return;
        int target = knowledgeTier(completed);
        while (AssemblyKnowledge.tier(player) < target) {
            if (!AssemblyKnowledge.learn(player, AssemblyKnowledge.tier(player) + 1)) break;
        }
    }

    public static List<Component> rewards(ResearchNode node) {
        List<Component> result = new ArrayList<>();
        for (ResourceLocation product : products(node.id())) result.add(BuiltInRegistries.ITEM.get(product).getDescription());
        List<String> ranks = new ArrayList<>();
        for (TechRank rank : TechRank.values()) if (equipment(rank).equals(node.id())) ranks.add(rank.name());
        if (!ranks.isEmpty()) result.add(Component.translatable("gui.transcend.research.reward.equipment", String.join("/", ranks)));
        for (ShieldModule module : ShieldModule.values()) if (module(module).equals(node.id()))
            result.add(Component.translatable("shieldmodule.transcend." + module.id()));
        if (node.id().equals("combat_basics")) result.add(Component.translatable("gui.transcend.research.reward.standard_gun_modules"));
        if (node.id().equals("advanced_combat")) result.add(Component.translatable("gui.transcend.research.reward.advanced_gun_modules"));
        if (node.id().equals("quantum_combat")) result.add(GunModule.MUZZLE_SIRIUS.displayName());
        if (node.id().equals("automation")) result.add(Component.translatable("gui.transcend.research.reward.automation"));
        int knowledge = KNOWLEDGE.indexOf(node.id());
        if (knowledge >= 0) result.add(Component.translatable("gui.transcend.research.reward.knowledge", knowledge + 1));
        if (result.isEmpty()) result.add(Component.translatable("gui.transcend.research.reward.followups"));
        return List.copyOf(result);
    }
}
