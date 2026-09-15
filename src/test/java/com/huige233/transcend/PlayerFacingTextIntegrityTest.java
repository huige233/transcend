package com.huige233.transcend;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;


/** 检查中英文语言文件的键集合、格式占位符、重复键及界面动态翻译族是否完整一致。 */
class PlayerFacingTextIntegrityTest {
    private static final Path LANG = Path.of("src/main/resources/assets/transcend/lang");
    private static final Pattern VALID_PLACEHOLDER = Pattern.compile("%(?:\\d+\\$)?(?:[-+0-9.#]*[a-zA-Z]|%)");
    private static final Set<String> GUI_CORE = Set.of(
            "gui.transcend.test_dummy.title", "gui.transcend.test_dummy.tab_stats",
            "gui.transcend.test_dummy.tab_settings", "gui.transcend.test_dummy.tab_shield",
            "gui.transcend.test_dummy.tab_buffs", "gui.transcend.test_dummy.kind",
            "gui.transcend.test_dummy.cat_physical", "gui.transcend.test_dummy.cat_magic",
            "gui.transcend.test_dummy.heal_mode", "gui.transcend.test_dummy.shield",
            "gui.transcend.test_dummy.buff_add", "gui.transcend.test_sword.title",
            "gui.transcend.test_sword.search", "gui.transcend.test_sword.apply",
            "hud.transcend.particle_gun.title", "hud.transcend.phase_shield.status");
    private static final Set<String> DYNAMIC_FAMILIES = Set.of(
            "assembly.transcend.status.", "gunmodule.transcend.", "ammo.transcend.",
            "gui.transcend.test_dummy.cat_", "gui.transcend.test_dummy.kind_",
            "gui.transcend.test_dummy.heal_", "phase_shield.transcend.status.");

    @Test void localesHaveSameKeysAndValidComponentPlaceholders() throws IOException {
        Map<String,String> en = language("en_us"), zh = language("zh_cn");
        assertEquals(en.keySet(), zh.keySet());
        for (String key : en.keySet()) {
            assertTrue(allPlaceholdersValid(en.get(key)), key + " en");
            assertTrue(allPlaceholdersValid(zh.get(key)), key + " zh");
            assertEquals(placeholders(en.get(key)), placeholders(zh.get(key)), key);
        }
    }

    @Test void noDuplicateKeysArePresent() throws IOException {
        for (String locale : new String[]{"en_us", "zh_cn"}) {
            String raw = Files.readString(LANG.resolve(locale + ".json"));
            var matcher = Pattern.compile("\\\"([^\\\"\\\\]+)\\\"\\s*:").matcher(raw);
            java.util.Set<String> keys = new java.util.HashSet<>();
            while (matcher.find()) assertTrue(keys.add(matcher.group(1)), "duplicate " + matcher.group(1));
        }
    }

    @Test void guiCoreAndDynamicFamiliesAreTranslated() throws IOException {
        Map<String,String> values = language("en_us");
        for (String key : GUI_CORE) assertTrue(values.containsKey(key), key);
        for (String family : DYNAMIC_FAMILIES)
            assertTrue(values.keySet().stream().anyMatch(k -> k.startsWith(family)), family);
        for (String key : new String[]{"phase_shield.transcend.status.inactive",
                "phase_shield.transcend.status.active", "phase_shield.transcend.status.empty"})
            assertTrue(values.containsKey(key), key);
    }

    private static Map<String,String> language(String locale) throws IOException {
        var object = JsonParser.parseString(Files.readString(LANG.resolve(locale + ".json"))).getAsJsonObject();
        Map<String,String> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : object.entrySet()) {
            assertTrue(e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isString(), e.getKey());
            assertNull(result.put(e.getKey(), e.getValue().getAsString()), "duplicate " + e.getKey());
        }
        return result;
    }
    private static boolean allPlaceholdersValid(String value) {
        String remainder = VALID_PLACEHOLDER.matcher(value).replaceAll("");
        return !remainder.contains("%");
    }
    private static Set<String> placeholders(String value) {
        var out = new java.util.TreeSet<String>();
        var m = VALID_PLACEHOLDER.matcher(value);
        while (m.find()) if (!m.group().equals("%%")) out.add(m.group());
        return out;
    }
}
