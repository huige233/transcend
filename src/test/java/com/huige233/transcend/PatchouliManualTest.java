package com.huige233.transcend;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatchouliManualTest {
    private static final Path ROOT = Path.of(
            "src/main/resources/assets/transcend/patchouli_books/magic_guide");
    private static final Path LANGUAGE_ROOT = Path.of(
            "src/main/resources/assets/transcend/lang");
    private static final Pattern FORMAT_PLACEHOLDER = Pattern.compile(
            "%(?:\\d+\\$)?[-#+ 0,(]*\\d*(?:\\.\\d+)?[a-zA-Z]");

    @Test
    void localizedBooksAreMirroredAndValidJson() throws IOException {
        Set<String> english = relativeJsonFiles(ROOT.resolve("en_us"));
        Set<String> chinese = relativeJsonFiles(ROOT.resolve("zh_cn"));

        assertEquals(english, chinese);
        assertEquals(18, english.size());
        assertEquals(5, english.stream().filter(path -> path.startsWith("categories/")).count());
        assertEquals(13, english.stream().filter(path -> path.startsWith("entries/")).count());
    }

    @Test
    void localizedLanguageKeysAndPlaceholdersAreMirrored() throws IOException {
        JsonObject english = readObject(LANGUAGE_ROOT.resolve("en_us.json"));
        JsonObject chinese = readObject(LANGUAGE_ROOT.resolve("zh_cn.json"));

        assertEquals(english.keySet(), chinese.keySet());
        for (String key : english.keySet()) {
            assertEquals(formatPlaceholders(english.get(key).getAsString()),
                    formatPlaceholders(chinese.get(key).getAsString()), key);
        }
    }

    @Test
    void lifestealTextMatchesLandedDamageRuntime() throws IOException {
        String english = Files.readString(LANGUAGE_ROOT.resolve("en_us.json"));
        String chinese = Files.readString(LANGUAGE_ROOT.resolve("zh_cn.json"));
        String patchouliEnglish = Files.readString(
                ROOT.resolve("en_us/entries/spellcraft/effects.json"));
        String patchouliChinese = Files.readString(
                ROOT.resolve("zh_cn/entries/spellcraft/effects.json"));

        assertTrue(english.contains("15% of actual landed health damage"));
        assertTrue(chinese.contains("实际命中生命伤害的 15%"));
        assertTrue(patchouliEnglish.contains("15% of actual landed health damage"));
        assertTrue(patchouliChinese.contains("实际命中的生命伤害的 15%"));
        assertFalse(english.contains("30% of base spell damage"));
        assertFalse(chinese.contains("基础法术伤害的 30%"));
    }

    @Test
    void patchouliDistinguishesChaosFromNexusEntropy() throws IOException {
        String english = Files.readString(
                ROOT.resolve("en_us/entries/spellcraft/elements_reactions.json"));
        String chinese = Files.readString(
                ROOT.resolve("zh_cn/entries/spellcraft/elements_reactions.json"));

        assertTrue(english.contains("Nexus ENTROPY is not an element"));
        assertTrue(chinese.contains("ENTROPY 不是元素"));
    }

    @Test
    void patchouliExplainsRawHudAndAuthoritativeContextualPayment() throws IOException {
        String english = Files.readString(
                ROOT.resolve("en_us/entries/spellcraft/mana_cost.json"));
        String chinese = Files.readString(
                ROOT.resolve("zh_cn/entries/spellcraft/mana_cost.json"));

        assertTrue(english.contains("HUD displays the raw configuration cost"));
        assertTrue(english.contains("authoritative payment"));
        assertTrue(english.contains("chunk aspect multiplier once"));
        assertTrue(chinese.contains("HUD 显示法术公式的原始配置消耗"));
        assertTrue(chinese.contains("服务端权威支付"));
        assertTrue(chinese.contains("只应用一次当前区块相染倍率"));
    }

    @Test
    void tribulationFailureMessageStatesFullLateStageXpGate() throws IOException {
        String english = Files.readString(LANGUAGE_ROOT.resolve("en_us.json"));
        String chinese = Files.readString(LANGUAGE_ROOT.resolve("zh_cn.json"));

        assertTrue(english.contains("fill Late-stage cultivation XP in a non-final realm"));
        assertTrue(chinese.contains("在非最终境界填满后期修炼经验"));
    }

    @Test
    void placeholderComparisonPreservesArgumentSemantics() {
        assertEquals(formatPlaceholders("%s then %d"),
                formatPlaceholders("%2$d then %1$s"));
        assertEquals(formatPlaceholders("%2$d then %1$s"),
                formatPlaceholders("%1$s then %2$d"));
        assertFalse(formatPlaceholders("%s then %d")
                .equals(formatPlaceholders("%d then %s")));
    }

    @Test
    void manualAvoidsLegacyPlayerFacingProgressionTerms() throws IOException {
        for (String locale : Set.of("en_us", "zh_cn")) {
            try (var files = Files.walk(ROOT.resolve(locale))) {
                for (Path path : files.filter(file -> file.toString().endsWith(".json")).toList()) {
                    String text = Files.readString(path);
                    assertTrue(!text.contains("Ascension Stage") && !text.contains("Ascension Level")
                                    && !text.contains("飞升阶段") && !text.contains("升华阶段")
                                    && !text.contains("升华等级"),
                            path.toString());
                }
            }
        }
    }

    @Test
    void resistanceAndLifestealValuesMatchRuntimeBalance() throws IOException {
        String resistanceEn = Files.readString(ROOT.resolve("en_us/entries/combat/elemental_resistance.json"));
        String resistanceZh = Files.readString(ROOT.resolve("zh_cn/entries/combat/elemental_resistance.json"));
        String effectsEn = Files.readString(ROOT.resolve("en_us/entries/spellcraft/effects.json"));
        String effectsZh = Files.readString(ROOT.resolve("zh_cn/entries/spellcraft/effects.json"));

        assertTrue(resistanceEn.contains("20%") && resistanceEn.contains("5%")
                && resistanceEn.contains("10%") && resistanceEn.contains("0%"));
        assertTrue(resistanceZh.contains("20%") && resistanceZh.contains("5%")
                && resistanceZh.contains("10%") && resistanceZh.contains("0%"));
        assertTrue(effectsEn.contains("15%") && effectsEn.contains("25%"));
        assertTrue(effectsZh.contains("15%") && effectsZh.contains("25%"));
    }

    @Test
    void vowsAndReloadOwnershipAreDocumentedInBothLanguages() throws IOException {
        String progressionEn = Files.readString(ROOT.resolve("en_us/entries/cultivation/progression_terms.json"));
        String progressionZh = Files.readString(ROOT.resolve("zh_cn/entries/cultivation/progression_terms.json"));
        String talentsEn = Files.readString(ROOT.resolve("en_us/entries/talents/tree_basics.json"));
        String talentsZh = Files.readString(ROOT.resolve("zh_cn/entries/talents/tree_basics.json"));

        assertTrue(progressionEn.contains("25%") && progressionEn.contains("5 minutes")
                && progressionEn.contains("1.40") && progressionEn.contains("Generation")
                && progressionEn.contains("Overcoming"));
        assertTrue(progressionZh.contains("25%") && progressionZh.contains("5 分钟")
                && progressionZh.contains("1.40") && progressionZh.contains("相生")
                && progressionZh.contains("相克"));
        assertTrue(talentsEn.contains("orphan") && talentsEn.contains("no automatic refund"));
        assertTrue(talentsZh.contains("孤儿") && talentsZh.contains("不会自动返还"));
    }

    private static Set<String> relativeJsonFiles(Path localeRoot) throws IOException {
        try (var files = Files.walk(localeRoot)) {
            return files.filter(path -> path.toString().endsWith(".json"))
                    .peek(PatchouliManualTest::assertValidJson)
                    .map(localeRoot::relativize)
                    .map(path -> path.toString().replace('\\', '/'))
                    .collect(Collectors.toSet());
        }
    }

    private static void assertValidJson(Path path) {
        try {
            assertTrue(JsonParser.parseString(Files.readString(path)).isJsonObject(), path.toString());
        } catch (IOException exception) {
            throw new AssertionError(path.toString(), exception);
        }
    }

    private static JsonObject readObject(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static List<String> formatPlaceholders(String text) {
        List<String> placeholders = new ArrayList<>();
        Matcher matcher = FORMAT_PLACEHOLDER.matcher(text);
        int nextImplicitArgument = 1;
        while (matcher.find()) {
            String placeholder = matcher.group();
            int end = matcher.end();
            if (placeholder.contains(" ") && end < text.length()
                    && Character.isLetter(text.charAt(end))) continue;
            int dollar = placeholder.indexOf('$');
            String argument = dollar > 0
                    ? placeholder.substring(1, dollar)
                    : Integer.toString(nextImplicitArgument++);
            char conversion = placeholder.charAt(placeholder.length() - 1);
            placeholders.add(argument + ":" + conversion);
        }
        placeholders.sort(String::compareTo);
        return placeholders;
    }
}
