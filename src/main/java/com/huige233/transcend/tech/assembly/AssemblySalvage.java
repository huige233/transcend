package com.huige233.transcend.tech.assembly;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** 按实际消耗的末级材料抽取残料；低阶能源不返还能源碎屑。 */
public final class AssemblySalvage {
    private AssemblySalvage() {}
    public static String fragment(String path) {
        if (TechMaterials.metalTier(path) > 0) return path.replace("tech_metal_", "tech_scrap_");
        if (path.startsWith("tech_plate_") || path.startsWith("tech_dust_")) {
            String metal = path.substring(path.lastIndexOf('_') + 1);
            if (TechMaterials.METALS.contains(metal)) return "tech_scrap_" + metal;
        }
        return switch (path) {
            case "tech_part_capacitor", "tech_part_capacitor_core" -> "tech_capacitor_fragment";
            case "tech_core_pilot" -> "tech_core_fragment";
            case "tech_energy_matrix" -> "tech_cell_shard";
            default -> null;
        };
    }
    public static List<ItemStack> recover(List<ItemStack> consumed, double rate, RandomSource random) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ItemStack input : consumed) {
            var id = BuiltInRegistries.ITEM.getKey(input.getItem());
            String fragment = id.getNamespace().equals("transcend") ? fragment(id.getPath()) : null;
            if (fragment == null) continue;
            int limit = fragment.equals("tech_cell_shard") ? 1 : input.getCount();
            for (int i = 0; i < limit; i++) if (random.nextDouble() < Math.max(0, Math.min(1, rate)))
                counts.merge(fragment, 1, Integer::sum);
        }
        List<ItemStack> result = new ArrayList<>();
        counts.forEach((id, count) -> {
            if (id.equals("tech_cell_shard")) count = Math.min(1, count);
            while (count > 0) { int part = Math.min(64, count); result.add(new ItemStack(TechMaterials.item(id), part)); count -= part; }
        });
        return result;
    }
}
