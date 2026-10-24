package com.huige233.transcend.tech.research;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Recipe reachability includes knowledge and research, rather than merely checking that an output exists. */
class TechnologyProgressionResourcesTest {
    private static final Path DATA = Path.of("src/main/resources/data/transcend");

    @Test void firstResearchProcessorIsAvailableInItsUnlockEra() throws Exception {
        Set<String> available = availableAtEra(0);
        assertFalse(available.contains("transcend:tech_part_circuit"),
                "Finished circuits intentionally require the next era's steel and T2 knowledge");
        assertTrue(available.contains("transcend:research_processor"),
                "The T0 research processor must not depend on next-era assembly materials");
        for (String item : List.of("assembly_table", "research_station", "fire_generator", "wind_generator",
                "blank_research_component", "research_assist_unit", "portable_capacitor", "long_storage_basic"))
            assertTrue(available.contains("transcend:" + item), "Unreachable starter infrastructure: " + item);
    }

    @Test void infrastructureProgressesThroughQuantumBreedingAndUniversePower() throws Exception {
        Map<Integer, List<String>> milestones = Map.of(
                1, List.of("tech_control_core", "portable_capacitor_advanced", "long_storage_advanced"),
                2, List.of("research_quantum_computer", "portable_capacitor_ghost", "long_storage_quantum"),
                3, List.of("black_hole_seed", "magnetic_confinement_container", "black_hole_seed_breeder", "mini_singularity"),
                4, List.of("tech_energy_matrix", "mini_universe_generator", "research_cosmic_simulator"),
                5, List.of("long_storage_phantom"));
        for (var milestone : milestones.entrySet()) {
            Set<String> available = availableAtEra(milestone.getKey());
            for (String item : milestone.getValue())
                assertTrue(available.contains("transcend:" + item),
                        "Unreachable at research T" + milestone.getKey() + ": " + item);
        }
    }

    @Test void everyWorkbenchRecipeCanBeDiscoveredFromAnIngredient() throws Exception {
        Map<String, JsonObject> recipes = recipes();
        for (var entry : recipes.entrySet()) {
            JsonObject recipe = entry.getValue();
            if (!recipe.get("type").getAsString().startsWith("minecraft:crafting_")) continue;
            Path path = DATA.resolve("advancements/recipes/technology/" + entry.getKey() + ".json");
            assertTrue(Files.isRegularFile(path), "No recipe-book discovery for " + entry.getKey());
            JsonObject advancement = read(path);
            String id = "transcend:" + entry.getKey();
            assertEquals("minecraft:recipes/root", advancement.get("parent").getAsString());
            assertEquals(id, advancement.getAsJsonObject("rewards").getAsJsonArray("recipes").get(0).getAsString());
            JsonObject criteria = advancement.getAsJsonObject("criteria");
            JsonObject inventory = criteria.getAsJsonObject("has_ingredient");
            assertEquals("minecraft:inventory_changed", inventory.get("trigger").getAsString());
            Set<String> discoveryItems = new HashSet<>();
            var itemPredicate = inventory.getAsJsonObject("conditions").getAsJsonArray("items");
            assertEquals(1, itemPredicate.size(), "Discovery must accept any ingredient, not require all ingredients");
            itemPredicate.get(0).getAsJsonObject().getAsJsonArray("items")
                    .forEach(item -> discoveryItems.add(item.getAsString()));
            Set<String> ingredients = new HashSet<>();
            inputs(recipe).forEach(input -> collectItems(input, ingredients));
            assertEquals(ingredients, discoveryItems, id);
            assertEquals(id, criteria.getAsJsonObject("has_the_recipe").getAsJsonObject("conditions").get("recipe").getAsString());
            var requirements = advancement.getAsJsonArray("requirements");
            assertEquals(1, requirements.size(), "Inventory discovery must not also require an already unlocked recipe");
            Set<String> alternatives = new HashSet<>();
            requirements.get(0).getAsJsonArray().forEach(key -> alternatives.add(key.getAsString()));
            assertEquals(Set.of("has_ingredient", "has_the_recipe"), alternatives);
        }
    }

    private static Set<String> availableAtEra(int era) throws Exception {
        Set<String> completed = new HashSet<>();
        ResearchRegistry.all().stream().filter(node -> node.tier().index() <= era)
                .forEach(node -> completed.add(node.id()));
        int knowledge = ResearchUnlocks.knowledgeTier(completed);
        Set<String> available = new HashSet<>();
        var recipes = recipes().values();
        boolean changed;
        do {
            changed = false;
            for (JsonObject recipe : recipes) {
                String type = recipe.get("type").getAsString();
                if (!type.startsWith("minecraft:crafting_") && !type.equals("transcend:assembly")) continue;
                if (type.equals("transcend:assembly") && recipe.get("tier").getAsInt() > knowledge) continue;
                String output = recipe.getAsJsonObject("result").get("item").getAsString();
                String research = ResearchUnlocks.required(new ResourceLocation(output));
                if (research != null && !completed.contains(research)) continue;
                if (inputs(recipe).stream().allMatch(input -> ingredientAvailable(input, available)))
                    changed |= available.add(output);
            }
            // The breeder is a machine operation, not a crafting recipe. This resource-level path
            // includes its device, container, seed and matter inputs; it does not simulate machine ticks.
            if (available.containsAll(Set.of("transcend:black_hole_seed_breeder", "transcend:black_hole_seed",
                    "transcend:magnetic_confinement_container")) && hasAvailableBreedingMatter(available))
                changed |= available.add("transcend:mini_singularity");
        } while (changed);
        return available;
    }

    private static boolean hasAvailableBreedingMatter(Set<String> available) throws Exception {
        for (var value : read(DATA.resolve("tags/items/singularity_matter.json")).getAsJsonArray("values")) {
            String item = value.isJsonPrimitive() ? value.getAsString() : value.getAsJsonObject().get("id").getAsString();
            if (item.startsWith("minecraft:") || available.contains(item)) return true;
        }
        return false;
    }

    private static List<JsonElement> inputs(JsonObject recipe) {
        List<JsonElement> inputs = new ArrayList<>();
        if (recipe.has("first")) {
            inputs.add(recipe.getAsJsonObject("first").get("ingredient"));
            if (recipe.has("second")) inputs.add(recipe.getAsJsonObject("second").get("ingredient"));
        } else if (recipe.has("key")) {
            recipe.getAsJsonObject("key").entrySet().forEach(entry -> inputs.add(entry.getValue()));
        } else {
            recipe.getAsJsonArray("ingredients").forEach(inputs::add);
        }
        return inputs;
    }

    private static boolean ingredientAvailable(JsonElement input, Set<String> available) {
        if (input.isJsonArray()) {
            for (var alternative : input.getAsJsonArray()) if (ingredientAvailable(alternative, available)) return true;
            return false;
        }
        JsonObject object = input.getAsJsonObject();
        assertTrue(object.has("item"), "Resolve new tag ingredients explicitly in the progression test: " + input);
        String item = object.get("item").getAsString();
        return item.startsWith("minecraft:") || available.contains(item);
    }

    private static void collectItems(JsonElement input, Set<String> items) {
        if (input.isJsonArray()) input.getAsJsonArray().forEach(child -> collectItems(child, items));
        else items.add(input.getAsJsonObject().get("item").getAsString());
    }

    private static Map<String, JsonObject> recipes() throws Exception {
        Map<String, JsonObject> recipes = new LinkedHashMap<>();
        try (var paths = Files.list(DATA.resolve("recipes"))) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".json")).sorted().toList()) {
                String filename = path.getFileName().toString();
                recipes.put(filename.substring(0, filename.length() - 5), read(path));
            }
        }
        return recipes;
    }

    private static JsonObject read(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
