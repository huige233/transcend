package com.huige233.transcend.spell;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;

class SpellDamagePipelineGuardTest {
    private static final Pattern DIRECT_ELEMENT_BALANCE_FIELD = Pattern.compile(
            "\\.(?:baseDamage|manaCost|particleR|particleG|particleB)\\b(?!\\s*\\()");

    @Test
    void knownSpellSecondaryMethodsDoNotDirectlyHurtTargets() throws IOException {
        String wand = Files.readString(Path.of(
                "src/main/java/com/huige233/transcend/items/TranscendWand.java"));
        String projectile = Files.readString(Path.of(
                "src/main/java/com/huige233/transcend/spell/SpellProjectile.java"));

        String wandEffects = between(wand, "private void applyEffect(", "private void applyVisualExplosion(");
        String projectileEffects = between(projectile, "private void applyEffectModifiers(",
                "private void dealSecondary(");
        assertFalse(wandEffects.contains("target.hurt("));
        assertFalse(wandEffects.contains("extra.hurt("));
        assertFalse(wandEffects.contains("e -> e.hurt("));
        assertFalse(projectileEffects.contains("target.hurt("));
        assertFalse(projectileEffects.contains("chainTarget.hurt("));
        assertFalse(projectileEffects.contains("e -> e.hurt("));
    }

    @Test
    void spellExplosionEffectsNeverUseVanillaExplosionDamage() throws IOException {
        String wand = Files.readString(Path.of(
                "src/main/java/com/huige233/transcend/items/TranscendWand.java"));
        String projectile = Files.readString(Path.of(
                "src/main/java/com/huige233/transcend/spell/SpellProjectile.java"));

        assertFalse(wand.contains("level.explode("));
        assertFalse(projectile.contains("level().explode("));
    }

    @Test
    void productionCallersUseElementBalanceGetters() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        try (var files = Files.walk(sourceRoot)) {
            for (Path path : files.filter(file -> file.toString().endsWith(".java")).toList()) {
                if (path.endsWith("SpellElement.java")) continue;
                assertFalse(DIRECT_ELEMENT_BALANCE_FIELD.matcher(Files.readString(path)).find(), path.toString());
            }
        }
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        int endIndex = source.indexOf(end, startIndex);
        return source.substring(startIndex, endIndex);
    }
}
