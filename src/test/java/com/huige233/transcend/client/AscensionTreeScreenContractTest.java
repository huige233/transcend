package com.huige233.transcend.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AscensionTreeScreenContractTest {
    private static final Path SCREEN = Path.of(
            "src/main/java/com/huige233/transcend/client/AscensionTreeScreen.java");

    @Test
    void overviewExposesEveryCultivationStatusWithoutRequiringAClass() throws IOException {
        String overview = methodBody(Files.readString(SCREEN), "private void drawOverview(");
        assertAll("class-independent cultivation overview",
                () -> assertStatusRead(overview, "getCultivationRealm", "major realm"),
                () -> assertStatusRead(overview, "getCultivationStage", "minor stage"),
                () -> assertStatusRead(overview, "getCultivationXP", "cultivation XP"),
                () -> assertStatusRead(overview, "getCultivationXPRequired", "cultivation XP requirement"),
                () -> assertStatusRead(overview, "isReadyForTribulation", "tribulation readiness"),
                () -> assertStatusRead(overview, "hasCultivationDeviation", "cultivation deviation"),
                () -> assertStatusRead(overview, "isTribulationActive", "active tribulation"));

        for (String selectedClassBlock : blocksGuardedBy(overview, "if (data.hasSelectedClass())")) {
            assertAll("cultivation status must not be hidden by class selection",
                    () -> assertFalse(selectedClassBlock.contains("getCultivationRealm(")),
                    () -> assertFalse(selectedClassBlock.contains("getCultivationStage(")),
                    () -> assertFalse(selectedClassBlock.contains("getCultivationXP(")),
                    () -> assertFalse(selectedClassBlock.contains("isReadyForTribulation(")),
                    () -> assertFalse(selectedClassBlock.contains("hasCultivationDeviation(")),
                    () -> assertFalse(selectedClassBlock.contains("isTribulationActive(")));
        }
    }

    @Test
    void auraGuardControlExistsOutsideSelectedClassBranch() throws IOException {
        String buttons = methodBody(Files.readString(SCREEN), "private void buildOverviewButtons(");
        assertTrue(buttons.contains("data.isAuraGuardEnabled()"),
                "overview must expose the current Aura Guard state");
        assertTrue(buttons.contains("gui.transcend.aura_guard.enable")
                        && buttons.contains("gui.transcend.aura_guard.disable"),
                "overview must expose both Aura Guard actions");
        for (String selectedClassBlock : blocksGuardedBy(buttons, "if (data.hasSelectedClass())")) {
            assertFalse(selectedClassBlock.contains("gui.transcend.aura_guard"),
                    "Aura Guard is cultivation state and must remain available before class selection");
        }
    }

    private static void assertStatusRead(String method, String accessor, String status) {
        assertTrue(method.contains("data." + accessor + "("),
                "Ascension overview must render " + status + " via PlayerAscensionData." + accessor + "()");
    }

    private static List<String> blocksGuardedBy(String source, String guard) {
        List<String> blocks = new ArrayList<>();
        int searchFrom = 0;
        while (true) {
            int guardIndex = source.indexOf(guard, searchFrom);
            if (guardIndex < 0) return blocks;
            int brace = source.indexOf('{', guardIndex + guard.length());
            if (brace < 0) throw new AssertionError("Missing block for " + guard);
            blocks.add(bracedBody(source, brace));
            searchFrom = brace + blocks.get(blocks.size() - 1).length();
        }
    }

    private static String methodBody(String source, String signature) {
        int method = source.indexOf(signature);
        if (method < 0) throw new AssertionError("Missing " + signature + " in " + SCREEN);
        int brace = source.indexOf('{', method + signature.length());
        if (brace < 0) throw new AssertionError("Missing body for " + signature);
        return bracedBody(source, brace);
    }

    private static String bracedBody(String source, int openingBrace) {
        int depth = 0;
        for (int index = openingBrace; index < source.length(); index++) {
            char character = source.charAt(index);
            if (character == '{') depth++;
            if (character == '}' && --depth == 0) return source.substring(openingBrace, index + 1);
        }
        throw new AssertionError("Unclosed source block at " + openingBrace);
    }
}
