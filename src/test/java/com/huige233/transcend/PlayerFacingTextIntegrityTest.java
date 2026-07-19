package com.huige233.transcend;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerFacingTextIntegrityTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final Path LANG = RESOURCES.resolve("assets/transcend/lang");
    private static final Path MANUAL = RESOURCES.resolve("assets/transcend/patchouli_books/magic_guide");
    private static final Pattern PLACEHOLDER = Pattern.compile("%(?:(\\d+)\\$)?\\d*(?:\\.\\d+)?[dfs]");
    private static final Pattern REGISTER = Pattern.compile("\\.register\\(\"([a-z0-9_]+)\"");
    private static final Pattern HAN_LITERAL = Pattern.compile("Component\\.literal\\([^\\r\\n]*[\\p{IsHan}]");
    private static final Set<String> REMOVED_IDS = Set.of(
            "carrier_spike", "carrier_teleport", "carrier_ring", "carrier_breath", "carrier_ground", "carrier_summon",
            "element_ice", "element_thunder", "element_wind", "element_void", "element_holy", "element_blood",
            "element_dark", "element_poison", "element_time", "element_space", "element_nature",
            "effect_bounce", "effect_delayed", "effect_quickcast", "effect_gravity_well", "effect_echo",
            "effect_armor_break", "effect_lingering", "effect_devour", "effect_absorb", "effect_reflect",
            "effect_weaken", "effect_unstable", "effect_summon_guardian", "effect_summon_wisp");

    @Test
    void languagesHaveIdenticalKeysAndPlaceholders() throws IOException {
        Map<String, String> english = language("en_us");
        Map<String, String> chinese = language("zh_cn");
        assertEquals(english.keySet(), chinese.keySet());
        for (String key : english.keySet()) {
            assertEquals(placeholders(english.get(key)), placeholders(chinese.get(key)), key);
        }
    }

    @Test
    void languagesContainNoDevelopmentOrRemovedSystemKeys() throws IOException {
        for (String locale : List.of("en_us", "zh_cn")) {
            for (String key : language(locale).keySet()) {
                assertFalse(key.startsWith("_comment_") || key.toLowerCase().contains("round_development"), key);
                for (String removed : REMOVED_IDS) {
                    assertFalse(key.contains(removed), key);
                }
            }
        }
    }

    @Test
    void activeTextAndManualAvoidRemovedTaxonomyClaims() throws IOException {
        List<String> banned = List.of("all fourteen elements", "fourteen threads", "十四种元素", "十四丝线",
                "spell.element.ice", "spell.element.thunder", "spell.element.void", "summon_guardian", "carrier_summon");
        for (Path root : List.of(LANG, MANUAL)) {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.filter(file -> file.toString().endsWith(".json")).toList()) {
                    String text = Files.readString(path);
                    for (String value : banned) assertFalse(text.contains(value), () -> value + " in " + path);
                }
            }
        }
    }

    @Test
    void patchouliLocalesAreMirrored() throws IOException {
        assertEquals(relativeJsonFiles(MANUAL.resolve("en_us")), relativeJsonFiles(MANUAL.resolve("zh_cn")));
    }

    @Test
    void advancementTranscendItemsAreRegistered() throws IOException {
        Set<String> registered = new HashSet<>();
        for (String source : List.of("src/main/java/com/huige233/transcend/init/ModItems.java",
                "src/main/java/com/huige233/transcend/init/ModBlocks.java",
                "src/main/java/com/huige233/transcend/init/ModEntities.java")) {
            Matcher matcher = REGISTER.matcher(Files.readString(Path.of(source)));
            while (matcher.find()) registered.add(matcher.group(1));
        }
        Path advancements = RESOURCES.resolve("data/transcend/advancements");
        try (var paths = Files.walk(advancements)) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".json")).toList()) {
                JsonElement json = JsonParser.parseString(Files.readString(path));
                for (String id : advancementItems(json)) {
                    if (!id.startsWith("transcend:")) continue;
                    String pathId = id.substring("transcend:".length());
                    assertTrue(registered.contains(pathId),
                            () -> "Unregistered advancement item " + id + " in " + path);
                }
            }
        }
    }

    @Test
    void playerFacingJavaHasNoHanScriptComponentLiteral() throws IOException {
        Path java = Path.of("src/main/java");
        try (var paths = Files.walk(java)) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".java")).toList()) {
                assertFalse(HAN_LITERAL.matcher(Files.readString(path)).find(), path.toString());
            }
        }
    }

    private static Map<String, String> language(String locale) throws IOException {
        JsonObject object = JsonParser.parseString(Files.readString(LANG.resolve(locale + ".json"))).getAsJsonObject();
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            assertTrue(entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString(), entry.getKey());
            result.put(entry.getKey(), entry.getValue().getAsString());
        }
        return result;
    }

    private static List<String> placeholders(String value) {
        Matcher matcher = PLACEHOLDER.matcher(value.replace("%%", ""));
        List<String> result = new java.util.ArrayList<>();
        while (matcher.find()) result.add(matcher.group());
        return result.stream().sorted().toList();
    }

    private static Set<String> advancementItems(JsonElement element) {
        Set<String> result = new HashSet<>();
        collectAdvancementItems(element, result);
        return result;
    }

    private static void collectAdvancementItems(JsonElement element, Set<String> result) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectAdvancementItems(child, result));
            return;
        }
        if (!element.isJsonObject()) return;
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            JsonElement value = entry.getValue();
            if (entry.getKey().equals("item") && value.isJsonPrimitive()) {
                result.add(value.getAsString());
            } else if (entry.getKey().equals("items") && value.isJsonArray()) {
                value.getAsJsonArray().forEach(item -> {
                    if (item.isJsonPrimitive()) result.add(item.getAsString());
                    else collectAdvancementItems(item, result);
                });
            } else {
                collectAdvancementItems(value, result);
            }
        }
    }

    private static Set<String> relativeJsonFiles(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".json"))
                    .map(root::relativize)
                    .map(path -> path.toString().replace('\\', '/'))
                    .collect(java.util.stream.Collectors.toSet());
        }
    }
}
