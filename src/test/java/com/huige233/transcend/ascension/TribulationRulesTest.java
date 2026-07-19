package com.huige233.transcend.ascension;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TribulationRulesTest {
    @Test
    void deviationDurationScalesAndClampsByRealm() {
        assertEquals(6000, TribulationRules.deviationDurationTicks(1));
        assertEquals(32400, TribulationRules.deviationDurationTicks(12));
        assertEquals(6000, TribulationRules.deviationDurationTicks(-4));
        assertEquals(32400, TribulationRules.deviationDurationTicks(99));
    }

    @Test
    void peakXpPenaltyStaysWithinRequiredRange() {
        assertEquals(0.15F, TribulationRules.peakXpPenaltyFraction(-1.0F), 0.0001F);
        assertEquals(0.20F, TribulationRules.peakXpPenaltyFraction(0.5F), 0.0001F);
        assertEquals(0.25F, TribulationRules.peakXpPenaltyFraction(2.0F), 0.0001F);
    }

    @Test
    void highTierHasOneInFourChaosBranchAndOtherwiseCountersSpecificMastery() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/huige233/transcend/ascension/TribulationManager.java"));
        String selector = methodBody(source, "private static StrikeElement chooseElement(");
        String highTier = guardedBlock(selector, "if (targetRank >= 7)");
        Pattern anyStrikeReturn = Pattern.compile("return\\s+new\\s+StrikeElement\\([^;]+;", Pattern.DOTALL);
        Pattern chaosReturn = Pattern.compile(
                "return\\s+new\\s+StrikeElement\\([^;]+,\\s*true\\s*\\);", Pattern.DOTALL);

        assertAll("deterministic rank-seven selection",
                () -> assertTrue(highTier.contains("nextInt(4) == 0"),
                        "rank >= 7 must use exactly one of four rolls for the chaos branch"),
                () -> assertTrue(chaosReturn.matcher(highTier).find(),
                        "the 25% high-tier branch must explicitly select chaos resistance"),
                () -> assertFalse(anyStrikeReturn.matcher(chaosReturn.matcher(highTier).replaceAll("")).find(),
                        "non-chaos high-tier rolls must fall through to mastery-counter selection"));

        int highTierEnd = selector.indexOf(highTier) + highTier.length();
        String fallback = selector.substring(highTierEnd);
        assertTrue(fallback.contains("mastery != null && mastery.isSpecific()"),
                "all non-chaos rolls must reach the specific-mastery counter branch");
        assertAll("five-element mastery counters",
                () -> assertTrue(fallback.contains("case METAL -> SpellElement.FIRE")),
                () -> assertTrue(fallback.contains("case WOOD -> SpellElement.METAL")),
                () -> assertTrue(fallback.contains("case WATER -> SpellElement.EARTH")),
                () -> assertTrue(fallback.contains("case FIRE -> SpellElement.WATER")),
                () -> assertTrue(fallback.contains("case EARTH -> SpellElement.WOOD")));
    }

    private static String methodBody(String source, String signature) {
        int method = source.indexOf(signature);
        if (method < 0) throw new AssertionError("Missing production selector " + signature);
        int brace = source.indexOf('{', method + signature.length());
        return bracedBody(source, brace);
    }

    private static String guardedBlock(String source, String guard) {
        int condition = source.indexOf(guard);
        if (condition < 0) throw new AssertionError("Missing production guard " + guard);
        int brace = source.indexOf('{', condition + guard.length());
        return bracedBody(source, brace);
    }

    private static String bracedBody(String source, int openingBrace) {
        if (openingBrace < 0) throw new AssertionError("Missing opening brace");
        int depth = 0;
        for (int index = openingBrace; index < source.length(); index++) {
            char character = source.charAt(index);
            if (character == '{') depth++;
            if (character == '}' && --depth == 0) return source.substring(openingBrace, index + 1);
        }
        throw new AssertionError("Unclosed source block at " + openingBrace);
    }
}
