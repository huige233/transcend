package com.huige233.transcend.tech.assembly;

import com.google.gson.*;
import com.huige233.transcend.tech.research.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.math.BigInteger;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AssemblyWorkflowTest {
    private List<JsonObject> recipes() throws Exception {
        List<JsonObject> result = new ArrayList<>();
        try (var paths = Files.list(Path.of("src/main/resources/data/transcend/recipes"))) {
            for (var path : paths.filter(p -> p.toString().endsWith(".json")).toList())
                result.add(JsonParser.parseString(Files.readString(path)).getAsJsonObject());
        }
        return result;
    }

    @Test void everyProductionDisciplineHasARecipeAtEveryTrainingTier() throws Exception {
        var recipes = recipes();
        for (String process : List.of("press", "grind", "cast", "fabricate")) {
            for (int tier = 1; tier <= 7; tier++) {
                int t = tier;
                assertTrue(recipes.stream().anyMatch(r -> r.get("type").getAsString().equals("transcend:assembly")
                        && r.get("process").getAsString().equals(process) && r.get("tier").getAsInt() == t), process + " T" + tier);
            }
        }
    }

    @Test void eachDisciplineCanReachTheNextTierWithoutThousandsOfRepetitions() {
        int xp = 0;
        for (int tier = 1; tier <= 7; tier++) {
            int goal = AssemblyRules.xpForLevel(Math.min(100, tier * 15));
            int crafts = 0;
            while (xp < goal && crafts < 60) xp += AssemblyRules.gain(xp, tier, crafts++);
            assertEquals(goal, xp, "Unreachable tier " + tier);
            assertTrue(crafts <= 40, "Excessive training at tier " + tier);
            assertEquals(0, AssemblyRules.gain(xp, tier, Integer.MAX_VALUE), "Low-tier experience must stay capped");
        }
        assertTrue(AssemblyKnowledge.canManufacture(7, 0, 7, "recycle"));
        assertFalse(AssemblyKnowledge.canManufacture(6, 100, 7, "recycle"));
    }

    @Test void allSevenEquipmentStagesHaveACompleteMaterialPath() throws Exception {
        List<JsonObject> recipes = recipes();
        for (int stage = 0; stage <= 6; stage++) {
            final int current = stage;
            Set<String> completed = new HashSet<>();
            ResearchRegistry.all().stream().filter(n -> n.tier().index() <= current).forEach(n -> completed.add(n.id()));
            Set<String> available = new HashSet<>();
            boolean changed;
            do {
                changed = false;
                for (JsonObject recipe : recipes) {
                    String type = recipe.get("type").getAsString();
                    if (!type.startsWith("minecraft:crafting_") && !type.equals("transcend:assembly")) continue;
                    if (type.equals("transcend:assembly") && recipe.get("tier").getAsInt() > stage + 1) continue;
                    JsonObject result = recipe.getAsJsonObject("result");
                    String output = result.get("item").getAsString();
                    String research = ResearchUnlocks.required(new ResourceLocation(output));
                    if (research != null && !completed.contains(research)) continue;
                    JsonElement inputs = type.equals("transcend:assembly") ? recipe.getAsJsonObject("first").get("ingredient")
                            : recipe.has("key") ? recipe.get("key") : recipe.get("ingredients");
                    if (!available(inputs, available) || (recipe.has("second") && !available(recipe.getAsJsonObject("second").get("ingredient"), available))) continue;
                    changed |= available.add(output);
                    if (result.has("nbt") && result.getAsJsonObject("nbt").has("TechData"))
                        changed |= available.add(output + "#" + result.getAsJsonObject("nbt").getAsJsonObject("TechData").get("Rank").getAsString());
                }
            } while (changed);
            String rank = List.of("e", "d", "c", "b", "a", "s", "ex").get(stage);
            assertTrue(available.contains("transcend:particle_gun#" + rank), "Gun " + rank);
            assertTrue(available.contains("transcend:phase_shield#" + rank), "Shield " + rank);
        }
    }

    private boolean available(JsonElement node, Set<String> items) {
        if (node.isJsonArray()) { for (var child : node.getAsJsonArray()) if (!available(child, items)) return false; }
        else if (node.isJsonObject()) {
            var object = node.getAsJsonObject();
            if (object.has("item")) {
                String item = object.get("item").getAsString(); return item.startsWith("minecraft:") || items.contains(item);
            }
            for (var entry : object.entrySet()) if (!available(entry.getValue(), items)) return false;
        }
        return true;
    }

    @Test void oldEnergyFractionsMigrateWithoutLosingPaidEnergy() {
        CompoundTag old = new CompoundTag(); old.putInt("AccountingVersion", 1); old.putString("Points", "17");
        old.putLong("Energy", 321); old.putLong("Remainder", 9_999_999);
        ResearchEnergyAccount account = new ResearchEnergyAccount(); account.load(old);
        assertEquals(BigInteger.valueOf(116), account.points());
        assertEquals(99_999, account.remainder()); assertEquals(321, account.stored());
        CompoundTag current = new CompoundTag(); account.save(current);
        assertEquals(3, current.getInt("AccountingVersion"));
        ResearchEnergyAccount restored = new ResearchEnergyAccount(); restored.load(current);
        assertEquals(account.points(), restored.points()); assertEquals(account.remainder(), restored.remainder());
    }

    @Test void starterResearchAndGeneratorUpgradesHavePracticalMonotonicCosts() {
        long rf = ResearchRegistry.get("energy_basics").cost().longValueExact() * ResearchEnergyAccount.RF_PER_POINT;
        assertEquals(100_000L, ResearchEnergyAccount.RF_PER_POINT);
        assertEquals(100_000_000L, rf);
        int base = com.huige233.transcend.block.FEGeneratorBlockEntity.FIRE_BASE_PRODUCTION;
        long fourGeneratorSeconds = rf / (4L * base * 20);
        assertTrue(fourGeneratorSeconds >= 300 && fourGeneratorSeconds <= 900, "Starter research should reward parallel power generation");
        int previous = base;
        for (int tier = 1; tier <= 7; tier++) {
            int next = AssemblyRules.generatorProduction(base, tier);
            assertTrue(next > previous && next <= 100_000_000); previous = next;
        }
        assertEquals("tech_cell_shard", AssemblySalvage.fragment("tech_energy_matrix"));
        assertNull(AssemblySalvage.fragment("tech_energy_cell"));
        assertNull(AssemblySalvage.fragment("tech_energy_core"));
    }

    @Test void versionTwoPointsAndRfFractionsAreNotInflatedByTheNewPrice() {
        CompoundTag old = new CompoundTag(); old.putInt("AccountingVersion", 2); old.putString("Points", "1234");
        old.putLong("Remainder", 999); old.putLong("Energy", 500_000);
        ResearchEnergyAccount account = new ResearchEnergyAccount(); account.load(old);
        assertEquals(BigInteger.valueOf(1234), account.points());
        assertEquals(999, account.remainder()); assertEquals(500_000, account.stored());
        assertEquals(500_000, account.convert(500_000));
        assertEquals(BigInteger.valueOf(1239), account.points()); assertEquals(999, account.remainder());
        old.putLong("Remainder", Long.MAX_VALUE); account.load(old);
        assertEquals(999, account.remainder(), "V2 fractions are capped to the original 1,000 RF price");
    }

    @Test void oneResearchPointCostsExactlyOneHundredThousandRf() {
        ResearchEnergyAccount account = new ResearchEnergyAccount();
        account.receive(99_999, false); account.convert(Long.MAX_VALUE);
        assertEquals(BigInteger.ZERO, account.points()); assertEquals(99_999, account.remainder());
        account.receive(1, false); account.convert(Long.MAX_VALUE);
        assertEquals(BigInteger.ONE, account.points()); assertEquals(0, account.remainder());
    }
}
