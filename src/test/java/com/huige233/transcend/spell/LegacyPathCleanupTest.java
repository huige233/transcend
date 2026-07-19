package com.huige233.transcend.spell;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyPathCleanupTest {
    private static final Path MAIN = Path.of("src/main/java");
    private static final Pattern DIRECT_CARRIER_FIELD = Pattern.compile(
            "\\.(?:gravity|aoeRadius|projectileSpeed|baseCooldown)\\b(?!\\s*\\()");
    private static final Set<String> DORMANT_REACTIONS = Set.of(
            "getReactionName", "findReactiveMark", "dispatchByAspect", "steamExplosion",
            "toxicFlame", "twilightBurst", "wildSurge", "lifeSiphon", "witherFrost", "snareGrowth");

    @Test
    void spellScrollRightClickCannotCastOrConsumeResources() throws IOException {
        String source = Files.readString(MAIN.resolve(
                "com/huige233/transcend/items/SpellScrollItem.java"));
        String use = between(source, "public InteractionResultHolder<ItemStack> use(", "// ── Tooltip");

        assertTrue(use.contains("msg.transcend.scroll.migration"));
        assertTrue(use.contains("InteractionResultHolder.fail(stack)"));
        assertFalse(use.contains("consumeMana"));
        assertFalse(use.contains("shrink("));
        assertFalse(use.contains("SpellProjectile"));
        assertFalse(use.contains("addFreshEntity"));
    }

    @Test
    void dormantFourAspectReactionMethodsStayRemoved() throws IOException {
        String source = Files.readString(MAIN.resolve(
                "com/huige233/transcend/spell/ElementReaction.java"));
        for (String name : DORMANT_REACTIONS) assertFalse(source.contains(name), name);
        assertFalse(source.contains("target.hurt("));
        assertFalse(source.contains("SpellAspect"));
    }

    @Test
    void productionUsesCarrierOverridesInsteadOfDefaultFields() throws IOException {
        try (var files = Files.walk(MAIN)) {
            for (Path path : files.filter(file -> file.toString().endsWith(".java")).toList()) {
                if (path.endsWith("SpellCarrier.java") || path.endsWith("CarrierStats.java")) continue;
                assertFalse(DIRECT_CARRIER_FIELD.matcher(Files.readString(path)).find(), path.toString());
            }
        }
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        int endIndex = source.indexOf(end, startIndex);
        assertTrue(startIndex >= 0 && endIndex > startIndex);
        return source.substring(startIndex, endIndex);
    }
}
