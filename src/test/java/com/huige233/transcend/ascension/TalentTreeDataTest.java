package com.huige233.transcend.ascension;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalentTreeDataTest {
    private static final Set<String> ELEMENT_IDS = Set.of("metal", "wood", "water", "fire", "earth", "chaos");
    private static final Set<String> BANNED_ENGLISH = Set.of(
            "ignite", "summon", "teleport", "cloud", "stun", "curse", "spread", "nova", "rift", "gate");
    private static final Set<String> BANNED_CHINESE = Set.of(
            "点燃", "召唤", "传送", "毒云", "击晕", "诅咒", "蔓延", "新星", "裂缝", "领域");

    @Test
    void everyNodeIsLocalizedAndDescriptionsAvoidStaleMechanics() throws IOException {
        JsonObject english = readJson(Path.of("src/main/resources/assets/transcend/lang/en_us.json"));
        JsonObject chinese = readJson(Path.of("src/main/resources/assets/transcend/lang/zh_cn.json"));
        assertEquals(english.keySet(), chinese.keySet(), "EN/ZH language keys must remain mirrored");
        Map<String, JsonObject> nodes = loadAllNodes();

        assertEquals(121, nodes.size());
        for (Map.Entry<String, JsonObject> entry : nodes.entrySet()) {
            String id = entry.getKey();
            JsonObject node = entry.getValue();
            String nameKey = node.has("name_key") ? node.get("name_key").getAsString() : "node.transcend." + id;
            String descKey = node.has("desc_key") ? node.get("desc_key").getAsString() : nameKey + ".desc";
            assertTrue(english.has(nameKey), nameKey);
            assertTrue(chinese.has(nameKey), nameKey);
            assertTrue(english.has(descKey), descKey);
            assertTrue(chinese.has(descKey), descKey);

            String enDescription = english.get(descKey).getAsString().toLowerCase(Locale.ROOT);
            String zhDescription = chinese.get(descKey).getAsString();
            for (String banned : BANNED_ENGLISH) {
                assertFalse(Pattern.compile("\\b" + Pattern.quote(banned) + "\\b").matcher(enDescription).find(),
                        descKey + " contains " + banned);
            }
            for (String banned : BANNED_CHINESE) {
                assertFalse(zhDescription.contains(banned), descKey + " contains " + banned);
            }
        }
    }

    @Test
    void activeLanguageAvoidsLegacyProgressionTerms() throws IOException {
        for (String locale : Set.of("en_us", "zh_cn")) {
            JsonObject language = readJson(Path.of(
                    "src/main/resources/assets/transcend/lang", locale + ".json"));
            for (Map.Entry<String, JsonElement> entry : language.entrySet()) {
                if (entry.getKey().startsWith("_comment")) continue;
                String text = entry.getValue().getAsString();
                assertFalse(text.contains("Ascension Stage"), entry.getKey());
                assertFalse(text.contains("Ascension Level"), entry.getKey());
                assertFalse(text.contains("飞升阶段"), entry.getKey());
                assertFalse(text.contains("升华阶段"), entry.getKey());
                assertFalse(text.contains("升华等级"), entry.getKey());
            }
        }
    }

    @Test
    void talentTreesUseCanonicalLiveScalingAndBoundedValues() throws IOException {
        Path directory = Path.of("src/main/resources/data/transcend/talent_trees");
        Set<String> owners = new HashSet<>();

        try (var files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                JsonObject tree = readJson(file);
                String owner = tree.get("mage_class").getAsString();
                owners.add(owner);
                String ownerElement = ownerElement(owner);
                for (Map.Entry<String, JsonElement> entry : tree.getAsJsonObject("nodes").entrySet()) {
                    JsonObject node = entry.getValue().getAsJsonObject();
                    assertTrue(node.get("tier").getAsInt() >= 1 && node.get("tier").getAsInt() <= 5, entry.getKey());
                    assertEquals(1, node.get("cost").getAsInt(), entry.getKey());
                    validateNodeNumbers(entry.getKey(), node);
                    if (node.has("element_scaling")) {
                        JsonObject scaling = node.getAsJsonObject("element_scaling");
                        assertTrue(ELEMENT_IDS.containsAll(scaling.keySet()), file.toString());
                        assertTrue(scaling.has(ownerElement), entry.getKey() + " has no owner-applicable scaling");
                        for (JsonElement bonuses : scaling.asMap().values()) {
                            if (bonuses.getAsJsonObject().has("bonus_max_health")) {
                                assertTrue(bonuses.getAsJsonObject().get("bonus_max_health").getAsDouble() >= 2.0,
                                        entry.getKey() + " uses fractional flat HP");
                            }
                        }
                    }
                    if (node.get("tier").getAsInt() == 5 && node.has("stat_bonuses")
                            && node.getAsJsonObject("stat_bonuses").has("spell_power_bonus")) {
                        assertTrue(node.getAsJsonObject("stat_bonuses").get("spell_power_bonus").getAsDouble() <= 0.20,
                                entry.getKey());
                    }
                }
            }
        }

        assertEquals(Set.of("pyromancer", "cryomancer", "stormcaller", "abysswalker",
                "earthshaper", "chronoweaver", "omniscient"), owners);
        JsonObject chronoweaver = readJson(directory.resolve("chronoweaver.json"));
        for (JsonElement node : chronoweaver.getAsJsonObject("nodes").asMap().values()) {
            if (node.getAsJsonObject().has("element_scaling")) {
                assertEquals(Set.of("chaos"), node.getAsJsonObject().getAsJsonObject("element_scaling").keySet());
            }
        }
    }

    @Test
    void naturalPointEconomyCompletesBothTreesWithFlexibility() throws IOException {
        JsonObject universal = readJson(Path.of(
                "src/main/resources/data/transcend/ascension_trees/universal.json"));
        assertEquals(30, universal.getAsJsonObject("nodes").size());
        assertEquals(30, PlayerAscensionData.MAX_LEVEL * PlayerAscensionData.POINTS_PER_LEVEL);
        assertEquals(22, (CultivationRealm.values().length - 1) * 2);

        try (var files = Files.list(Path.of("src/main/resources/data/transcend/talent_trees"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                JsonObject nodes = readJson(file).getAsJsonObject("nodes");
                assertEquals(13, nodes.size(), file.toString());
                assertEquals(43, universal.getAsJsonObject("nodes").size() + nodes.size(), file.toString());
                assertEquals(9, 52 - 43, file.toString());
            }
        }
    }

    @Test
    void graphParentsExistAtLowerTiersAndNoSummonStatsRemain() throws IOException {
        for (Path file : allTreeFiles()) {
            JsonObject nodes = readJson(file).getAsJsonObject("nodes");
            for (Map.Entry<String, JsonElement> entry : nodes.entrySet()) {
                JsonObject node = entry.getValue().getAsJsonObject();
                if (node.has("stat_bonuses")) {
                    assertFalse(node.getAsJsonObject("stat_bonuses").has("summon_damage_bonus"), entry.getKey());
                }
                if (node.has("element_scaling")) {
                    for (JsonElement bonuses : node.getAsJsonObject("element_scaling").asMap().values()) {
                        assertFalse(bonuses.getAsJsonObject().has("summon_damage_bonus"), entry.getKey());
                    }
                }
                for (JsonElement parentElement : node.getAsJsonArray("parents")) {
                    String parentId = parentElement.getAsString();
                    JsonObject parent = nodes.has(parentId) ? nodes.getAsJsonObject(parentId) : null;
                    assertNotNull(parent, entry.getKey() + " missing parent " + parentId);
                    assertTrue(parent.get("tier").getAsInt() < node.get("tier").getAsInt(),
                            parentId + " must be below " + entry.getKey());
                }
            }
        }
    }

    @Test
    void earthPestilenceHasImplementedReactionBonus() throws IOException {
        JsonObject node = readJson(Path.of("src/main/resources/data/transcend/talent_trees/earthshaper.json"))
                .getAsJsonObject("nodes").getAsJsonObject("earth_pestilence");
        assertEquals(0.12, node.getAsJsonObject("stat_bonuses").get("reaction_bonus").getAsDouble(), 0.00001);
    }

    private static void validateNodeNumbers(String id, JsonObject node) {
        validateFinite(id, node);
        if (node.has("stat_bonuses") && node.getAsJsonObject("stat_bonuses").has("crit_multiplier")) {
            double multiplier = node.getAsJsonObject("stat_bonuses").get("crit_multiplier").getAsDouble();
            assertTrue(multiplier >= 1.0 && multiplier <= 2.2, id);
        }
        if (node.has("passive_effects")) {
            for (JsonElement element : node.getAsJsonArray("passive_effects")) {
                JsonObject passive = element.getAsJsonObject();
                for (String probability : Set.of("chance", "threshold", "reduction", "reflect_fraction")) {
                    if (passive.has(probability)) {
                        double value = passive.get(probability).getAsDouble();
                        assertTrue(value >= 0.0 && value <= 1.0, id + " " + probability);
                    }
                }
                for (String positive : Set.of("duration", "cooldown")) {
                    if (passive.has(positive)) assertTrue(passive.get(positive).getAsInt() > 0, id + " " + positive);
                }
            }
        }
    }

    private static void validateFinite(String id, JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            double value = element.getAsDouble();
            assertTrue(Double.isFinite(value) && Math.abs(value) <= 10_000, id + " has invalid value " + value);
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) validateFinite(id, child);
        } else if (element.isJsonObject()) {
            for (JsonElement child : element.getAsJsonObject().asMap().values()) validateFinite(id, child);
        }
    }

    private static Map<String, JsonObject> loadAllNodes() throws IOException {
        Map<String, JsonObject> result = new HashMap<>();
        for (Path file : allTreeFiles()) {
            for (Map.Entry<String, JsonElement> entry : readJson(file).getAsJsonObject("nodes").entrySet()) {
                assertFalse(result.containsKey(entry.getKey()), "duplicate node " + entry.getKey());
                result.put(entry.getKey(), entry.getValue().getAsJsonObject());
            }
        }
        return result;
    }

    private static Set<Path> allTreeFiles() throws IOException {
        Set<Path> result = new HashSet<>();
        for (String folder : Set.of("ascension_trees", "talent_trees")) {
            try (var files = Files.list(Path.of("src/main/resources/data/transcend", folder))) {
                result.addAll(files.filter(path -> path.toString().endsWith(".json")).toList());
            }
        }
        return result;
    }

    private static JsonObject readJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static String ownerElement(String owner) {
        return switch (owner) {
            case "pyromancer", "stormcaller" -> "fire";
            case "cryomancer" -> "water";
            case "earthshaper" -> "earth";
            case "abysswalker", "chronoweaver", "omniscient" -> "chaos";
            default -> throw new IllegalArgumentException(owner);
        };
    }
}
