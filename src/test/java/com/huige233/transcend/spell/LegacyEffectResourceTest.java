package com.huige233.transcend.spell;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyEffectResourceTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final Set<String> OLD_IDS = Set.of("bounce", "delayed", "quickcast", "gravity_well",
            "echo", "armor_break", "lingering", "devour", "absorb", "reflect", "weaken", "unstable");
    private static final Set<String> ORPHANED_ASSET_STEMS = Set.of(
            "carrier_spike", "carrier_teleport", "carrier_ring", "carrier_breath", "carrier_ground",
            "carrier_summon",
            "element_acid", "element_blood", "element_dark", "element_eldritch", "element_holy",
            "element_ice", "element_light", "element_nature", "element_poison", "element_sonic",
            "element_space", "element_thunder", "element_time", "element_void", "element_wind",
            "effect_summon_guardian", "effect_summon_wisp");
    private static final Set<String> LEGACY_ITEM_TEXTURES = Set.of(
            "carrier_spike", "carrier_teleport", "carrier_ring", "carrier_breath", "carrier_ground",
            "carrier_summon",
            "element_acid", "element_blood", "element_dark", "element_eldritch", "element_holy",
            "element_ice", "element_light", "element_nature", "element_poison", "element_sonic",
            "element_space", "element_thunder", "element_time", "element_void", "element_wind",
            "effect_summon_guardian", "effect_summon_wisp");

    @Test
    void noLegacyEffectResourceIdsRemain() throws IOException {
        try (var paths = Files.walk(RESOURCES)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                String normalized = path.toString().replace('\\', '/');
                for (String id : OLD_IDS) {
                    assertFalse(normalized.contains("effect_" + id), () -> "legacy effect resource: " + normalized);
                }
                if (!normalized.endsWith(".json")) continue;
                String json = Files.readString(path);
                for (String id : OLD_IDS) {
                    assertFalse(json.contains("spell.effect." + id), () -> "legacy effect key in " + normalized);
                    assertFalse(json.contains("transcend:effect_" + id), () -> "legacy effect item in " + normalized);
                    assertFalse(json.matches("(?s).*\\\"effect\\\"\\s*:\\s*\\\"" + id + "\\\".*"),
                            () -> "legacy spell effect in " + normalized);
                }
            }
        }
    }

    @Test
    void orphanedCarrierElementAndSummonAssetsAreRemoved() throws IOException {
        List<String> stale = new ArrayList<>();
        Path assets = RESOURCES.resolve("assets/transcend");
        try (var paths = Files.walk(assets)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                String fileName = path.getFileName().toString();
                int extension = fileName.indexOf('.');
                String stem = extension < 0 ? fileName : fileName.substring(0, extension);
                if (ORPHANED_ASSET_STEMS.contains(stem)) {
                    stale.add(normalized(RESOURCES.relativize(path)));
                }
            }
        }
        stale.sort(String::compareTo);
        assertTrue(stale.isEmpty(), () -> "orphaned pre-migration spell assets remain:\n"
                + String.join("\n", stale));
    }

    @Test
    void activeResourcesDoNotReferenceRemovedSpellComponentAssets() throws IOException {
        List<String> stale = new ArrayList<>();
        Path assets = RESOURCES.resolve("assets/transcend");
        try (var paths = Files.walk(assets)) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".json")).toList()) {
                if (ORPHANED_ASSET_STEMS.contains(stem(path))) continue;
                String json = Files.readString(path);
                for (String legacyTexture : LEGACY_ITEM_TEXTURES) {
                    if (json.contains("transcend:item/" + legacyTexture)) {
                        stale.add(normalized(RESOURCES.relativize(path)) + " -> " + legacyTexture);
                    }
                }
            }
        }

        stale.sort(String::compareTo);
        assertTrue(stale.isEmpty(), () -> "stale active spell-component asset references remain:\n"
                + String.join("\n", stale));
    }

    private static String stem(Path path) {
        String fileName = path.getFileName().toString();
        int extension = fileName.indexOf('.');
        return extension < 0 ? fileName : fileName.substring(0, extension);
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }
}
