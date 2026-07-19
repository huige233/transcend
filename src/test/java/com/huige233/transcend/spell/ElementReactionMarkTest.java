package com.huige233.transcend.spell;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementReactionMarkTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void incomingFiveElementAlwaysBecomesLatestMark() {
        ElementReaction.MarkResult result = ElementReaction.resolveMark(
                SpellElement.METAL, SpellElement.WATER, 0.0F);
        assertEquals(SpellElement.WATER, result.mark());
        assertEquals(1.5F, result.damageMultiplier(), EPSILON);
        assertEquals("five_elements_generation", result.vowReaction());
    }

    @Test
    void unrelatedAndOvercomingHitsReplaceWithoutEnumOrderLookup() {
        ElementReaction.MarkResult unrelated = ElementReaction.resolveMark(
                SpellElement.METAL, SpellElement.EARTH, 0.0F);
        assertEquals(SpellElement.EARTH, unrelated.mark());
        assertEquals(1.0F, unrelated.damageMultiplier(), EPSILON);

        ElementReaction.MarkResult overcoming = ElementReaction.resolveMark(
                SpellElement.METAL, SpellElement.WOOD, 0.0F);
        assertEquals(SpellElement.WOOD, overcoming.mark());
        assertEquals(0.70F, overcoming.damageMultiplier(), EPSILON);
        assertEquals("five_elements_overcoming", overcoming.vowReaction());
    }

    @Test
    void chaosDoesNotReplaceFiveElementMark() {
        ElementReaction.MarkResult result = ElementReaction.resolveMark(
                SpellElement.FIRE, SpellElement.CHAOS, 0.0F);
        assertEquals(SpellElement.FIRE, result.mark());
        assertEquals(1.0F, result.damageMultiplier(), EPSILON);
    }

    @Test
    void baselineNormalAndChaosMarkSemanticsRemainDistinct() {
        ElementReaction.MarkResult normal = ElementReaction.resolveMark(
                SpellElement.METAL, SpellElement.WATER, 0.0F);
        ElementReaction.MarkResult chaos = ElementReaction.resolveMark(
                SpellElement.FIRE, SpellElement.CHAOS, 0.0F);

        assertEquals(SpellElement.WATER, normal.mark());
        assertEquals("five_elements_generation", normal.vowReaction());
        assertEquals(SpellElement.FIRE, chaos.mark());
        assertEquals(1.0F, chaos.damageMultiplier(), EPSILON);
    }

    @ParameterizedTest(name = "CHAOS forced index {0} resolves to {1}")
    @MethodSource("fiveElementIndices")
    void chaosResolutionCoversEveryForcedFiveElementIndex(int index, SpellElement expected) {
        assertEquals(expected, SpellDamageService.resolveElement(SpellElement.CHAOS, false, index));
    }

    @ParameterizedTest(name = "active ENTROPY forced index {0} resolves to {1}")
    @MethodSource("fiveElementIndices")
    void activeEntropyCoversEveryForcedFiveElementIndex(int index, SpellElement expected) {
        assertEquals(expected, SpellDamageService.resolveElement(SpellElement.FIRE, true, index));
    }

    @Test
    void inactiveEntropyLeavesNormalReactionResolutionUnchanged() {
        SpellElement resolved = SpellDamageService.resolveElement(SpellElement.WATER, false, 4);
        ElementReaction.MarkResult reaction = ElementReaction.resolveMark(
                SpellElement.METAL, resolved, 0.0F);

        assertEquals(SpellElement.WATER, resolved);
        assertEquals(SpellElement.WATER, reaction.mark());
        assertEquals("five_elements_generation", reaction.vowReaction());
    }

    @Test
    void entropyResolvesBeforeReactionWhileDirectChaosPreservesOldMark() {
        SpellElement entropyResolved = SpellDamageService.resolveElement(SpellElement.CHAOS, true, 2);
        ElementReaction.MarkResult scrambled = ElementReaction.resolveMark(
                SpellElement.METAL, entropyResolved, 0.0F);
        ElementReaction.MarkResult directChaos = ElementReaction.resolveMark(
                SpellElement.FIRE, SpellElement.CHAOS, 0.0F);

        assertEquals(SpellElement.WATER, entropyResolved);
        assertEquals(SpellElement.WATER, scrambled.mark());
        assertEquals(SpellElement.FIRE, directChaos.mark());
        assertEquals("chaos", SpellElement.CHAOS.id);
        assertEquals(SpellElement.CHAOS, SpellElement.getById("chaos"));
    }

    @Test
    void resolverRejectsMalformedSourceAndOutOfRangeIndices() {
        assertThrows(IllegalArgumentException.class,
                () -> SpellDamageService.resolveElement(null, false, 0));
        assertThrows(IllegalArgumentException.class,
                () -> SpellDamageService.resolveElement(SpellElement.CHAOS, false, -1));
        assertThrows(IllegalArgumentException.class,
                () -> SpellDamageService.resolveElement(SpellElement.CHAOS, false, 5));
    }

    @ParameterizedTest(name = "authoritative ENTROPY selection {0} is reused as {1}")
    @MethodSource("fiveElementIndices")
    void authoritativeEntropySelectionConsumesExactlyOneForcedIndex(int index, SpellElement expected)
            throws Exception {
        Method method = SpellDamageService.class.getDeclaredMethod(
                "selectElement", SpellElement.class, boolean.class, IntSupplier.class);
        method.setAccessible(true);
        AtomicInteger selections = new AtomicInteger();
        IntSupplier selector = () -> {
            selections.incrementAndGet();
            return index;
        };

        SpellElement resolved = (SpellElement) method.invoke(null, SpellElement.FIRE, true, selector);
        ElementReaction.MarkResult reaction = ElementReaction.resolveMark(
                SpellElement.METAL, resolved, 0.0F);

        assertEquals(expected, resolved);
        assertEquals(expected, reaction.mark());
        assertEquals(1, selections.get(), "one server-selected value must drive the full hit");
    }

    @Test
    void inactiveEntropyDoesNotConsumeSelectionAndChaosRemainsSeparatelyRandomized() throws Exception {
        Method method = SpellDamageService.class.getDeclaredMethod(
                "selectElement", SpellElement.class, boolean.class, IntSupplier.class);
        method.setAccessible(true);
        AtomicInteger selections = new AtomicInteger();
        IntSupplier selector = () -> {
            selections.incrementAndGet();
            return 3;
        };

        assertEquals(SpellElement.WATER,
                method.invoke(null, SpellElement.WATER, false, selector));
        assertEquals(0, selections.get(), "inactive normal elements must be unchanged");
        assertEquals(SpellElement.FIRE,
                method.invoke(null, SpellElement.CHAOS, false, selector));
        assertEquals(1, selections.get(), "CHAOS keeps its independent random resolution");
    }

    private static Stream<Arguments> fiveElementIndices() {
        return Stream.of(
                Arguments.of(0, SpellElement.METAL),
                Arguments.of(1, SpellElement.WOOD),
                Arguments.of(2, SpellElement.WATER),
                Arguments.of(3, SpellElement.FIRE),
                Arguments.of(4, SpellElement.EARTH));
    }

    @Test
    void landedMarksRetainTheHundredTickDuration() throws IOException {
        String reaction = Files.readString(Path.of(
                "src/main/java/com/huige233/transcend/spell/ElementReaction.java"));
        assertTrue(reaction.contains("private static final int MARK_DURATION = 100;"));
        assertTrue(reaction.contains("putInt(MARK_TICKS_TAG, MARK_DURATION)"));
    }
}
