package com.huige233.transcend.tech.research;

import com.google.gson.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ResearchManufacturingTest {
    private static List<JsonObject> recipes() throws Exception {
        try (var files = Files.list(Path.of("src/main/resources/data/transcend/recipes"))) {
            List<JsonObject> result = new ArrayList<>();
            for (Path path : files.filter(p -> p.toString().endsWith(".json")).toList())
                result.add(JsonParser.parseString(Files.readString(path)).getAsJsonObject());
            return result;
        }
    }

    @Test void allPhysicalResearchRewardsHaveActualManufacturingRecipes() throws Exception {
        Set<String> outputs = recipes().stream().map(r -> r.getAsJsonObject("result").get("item").getAsString()).collect(Collectors.toSet());
        for (ResearchNode node : ResearchRegistry.all())
            for (ResourceLocation item : ResearchUnlocks.products(node.id())) assertTrue(outputs.contains(item.toString()), item.toString());
        assertEquals("energy_micro_universe", ResearchUnlocks.required(new ResourceLocation("transcend", "mini_universe_generator")));
        assertEquals("energy_storage_quantum", ResearchUnlocks.required(new ResourceLocation("transcend", "portable_capacitor_ghost")));
    }

    @Test void researchBootstrapIsCraftableFromVanillaMaterialsWithoutResearchOrAssembly() throws Exception {
        Set<String> available = new HashSet<>();
        boolean changed;
        do {
            changed = false;
            for (JsonObject recipe : recipes()) {
                if (!recipe.get("type").getAsString().startsWith("minecraft:crafting_")) continue;
                String output = recipe.getAsJsonObject("result").get("item").getAsString();
                if (ResearchUnlocks.required(new ResourceLocation(output)) != null) continue;
                JsonElement ingredients = recipe.has("key") ? recipe.get("key") : recipe.get("ingredients");
                if (available(ingredients, available)) changed |= available.add(output);
            }
        } while (changed);
        for (String item : List.of("research_station", "assembly_table", "fire_generator", "tech_energy_cell",
                "tech_part_circuit_printed", "blank_research_component", "research_assist_unit"))
            assertTrue(available.contains("transcend:" + item), "Bootstrap loop or missing recipe: " + item);
    }

    private static boolean available(JsonElement element, Set<String> available) {
        if (element.isJsonArray()) {
            for (var child : element.getAsJsonArray()) if (!available(child, available)) return false;
        } else if (element.isJsonObject()) {
            var object = element.getAsJsonObject();
            if (object.has("item")) {
                String item = object.get("item").getAsString();
                return item.startsWith("minecraft:") || available.contains(item);
            }
            for (var child : object.entrySet()) if (!available(child.getValue(), available)) return false;
        }
        return true;
    }

    @Test void newlyAddedSpecializationsDoNotInvalidateExistingCompletedResearch() {
        Set<String> old = ResearchRegistry.all().stream().filter(n -> n.core() || n.id().startsWith("shield_combat_"))
                .map(ResearchNode::id).collect(Collectors.toSet());
        ListTag list = new ListTag(); old.forEach(id -> list.add(StringTag.valueOf(id)));
        CompoundTag tag = new CompoundTag(); tag.put("Completed", list);
        ResearchProgress restored = new ResearchProgress(); restored.load(tag);
        assertEquals(55, old.size());
        assertEquals(old, restored.completed());
        assertTrue(ResearchService.isUnlocked(restored.completed(), "energy_capacitor_design"));
        assertFalse(restored.completed().contains("energy_capacitor_design"));
    }
}
