package com.huige233.transcend.tech.assembly;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import java.util.*;

/** 制作链的金属、形态与残料目录；注册、回收、发电机升级和研究共用。 */
public final class TechMaterials {
    public static final List<String> METALS = List.of("crude", "steel", "alloy", "phase", "exo", "void", "singularity");
    public static final List<String> RESEARCH = List.of("materials_basics", "material_processing", "advanced_materials",
            "exotic_materials", "cosmic_forge_materials", "spacetime_engineering_materials", "dimensional_engineering_materials");
    private static final int[] COLORS = {0xc0b5a1, 0x8faabb, 0x53b8df, 0xa286ec, 0x6edbbf, 0x775dc4, 0xffb85c};
    private static final Map<String, RegistryObject<Item>> ITEMS = new LinkedHashMap<>();
    private TechMaterials() {}

    public static List<RegistryObject<Item>> register(DeferredRegister<Item> registry) {
        for (int i = 0; i < METALS.size(); i++) {
            for (String form : List.of("metal", "plate", "dust", "scrap"))
                add(registry, "tech_" + form + "_" + METALS.get(i), i + 1, COLORS[i]);
        }
        add(registry, "tech_capacitor_fragment", 3, 0x79cbe7);
        add(registry, "tech_core_fragment", 7, 0xf4ad6c);
        add(registry, "tech_cell_shard", 5, 0xaa83ed);
        add(registry, "tech_control_core", 2, 0x71d4bc);
        return List.copyOf(ITEMS.values());
    }
    private static void add(DeferredRegister<Item> registry, String id, int tier, int color) {
        ITEMS.put(id, registry.register(id, () -> new com.huige233.transcend.items.tech.WorkflowMaterialItem(tier, color)));
    }
    public static Item item(String id) { return ITEMS.get(id).get(); }
    public static String texture(String path) {
        if (path.startsWith("tech_dust_")) return "sugar";
        if (path.startsWith("tech_scrap_")) return "iron_nugget";
        return switch (path) {
            case "tech_capacitor_fragment" -> "prismarine_shard";
            case "tech_core_fragment" -> "echo_shard";
            case "tech_cell_shard" -> "amethyst_shard";
            case "tech_control_core" -> "comparator";
            default -> "iron_ingot";
        };
    }
    public static int metalTier(String path) {
        return path.startsWith("tech_metal_") ? METALS.indexOf(path.substring("tech_metal_".length())) + 1 : 0;
    }
    public static String research(String path) {
        for (String form : List.of("metal", "plate", "dust", "scrap")) {
            String prefix = "tech_" + form + "_";
            if (path.startsWith(prefix)) {
                int index = METALS.indexOf(path.substring(prefix.length()));
                if (index >= 0) return RESEARCH.get(index);
            }
        }
        return switch (path) {
            case "tech_capacitor_fragment" -> "precision_components_materials";
            case "tech_cell_shard" -> "cosmic_forge";
            case "tech_core_fragment" -> "dimensional_engineering_materials";
            case "tech_control_core" -> "control_systems_automation";
            default -> null;
        };
    }
}
