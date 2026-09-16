package com.huige233.transcend.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.resources.language.FormattedBidiReorder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/** 使用原版断行与双向排序验证缓存行的动态样式，不启动客户端。 */
class ChatFormattingTest {
    private static List<FormattedCharSequence> wrap(FormattedText message, int width, AtomicLong ticks) {
        return com.huige233.transcend.client.ChatFormatting.wrap(message, layout -> vanillaLayout(layout, width), ticks::get);
    }

    private static List<FormattedCharSequence> vanillaLayout(FormattedText layout, int width) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        new StringSplitter((codePoint, style) -> style.isBold() ? 2.0F : 1.0F)
                .splitLines(layout, width, Style.EMPTY, (line, continued) -> {
                    FormattedCharSequence visual = FormattedBidiReorder.reorder(line, false);
                    lines.add(continued ? FormattedCharSequence.composite(
                            FormattedCharSequence.codepoint(' ', Style.EMPTY), visual) : visual);
                });
        return lines;
    }

    @Test
    void sameTextMessagesKeepIndependentEffectsAndPlainMessagePassesThrough() {
        AtomicLong ticks = new AtomicLong();
        var glitch = wrap(Component.literal("§w2ABCD"), 80, ticks).get(0);
        var rainbow = wrap(Component.literal("§vABCD"), 80, ticks).get(0);
        var otherGlitch = wrap(Component.literal("§w1ABCD"), 80, ticks).get(0);
        Component plain = Component.literal("ABCD").withStyle(Style.EMPTY.withInsertion("original"));
        List<FormattedCharSequence> original = vanillaLayout(plain, 80);
        var unchanged = com.huige233.transcend.client.ChatFormatting.wrap(plain, received -> {
            assertSame(plain, received);
            return original;
        }, ticks::get);
        assertSame(original, unchanged);
        assertEquals(2, obfuscated(read(glitch)));
        assertEquals(1, obfuscated(read(otherGlitch)));
        assertEquals(0, obfuscated(read(rainbow)));
        assertEquals(0, obfuscated(read(unchanged.get(0))));
        assertEquals("original", read(unchanged.get(0)).get(0).style().getInsertion());
        ticks.set(60);
        assertEquals(2, obfuscated(read(glitch)));
        assertEquals("ABCD", text(read(glitch)));
        assertEquals("ABCD", plain.getString());
    }

    @Test
    void w2UsesExactlyTwoOriginalGlyphsAndRotatesOnLaterAccept() {
        AtomicLong ticks = new AtomicLong();
        Component source = Component.literal("§w2A😀B中C");
        var cached = wrap(source, 80, ticks).get(0);
        List<Seen> first = read(cached);
        assertEquals("A😀B中C", text(first));
        assertEquals(List.of((int) 'A', 0x1F600), selected(first));
        assertTrue(first.stream().allMatch(g -> g.style().getColor().getValue() == ChatFormatting.RED.getColor()));
        ticks.set(30);
        assertEquals(List.of(0x1F600, (int) 'B'), selected(read(cached)));
        ticks.set(120);
        assertEquals(List.of((int) 'A', (int) 'C'), selected(read(cached)));
        assertEquals("A😀B中C", text(read(cached)));
        assertEquals("§w2A😀B中C", source.getString());
    }

    @Test
    void repeatedWrappedSubstringsKeepTheirOwnOffsetsAndVanillaIndentation() {
        AtomicLong ticks = new AtomicLong();
        var lines = wrap(Component.literal("§w2ABCDABCD"), 4, ticks);
        assertEquals(2, lines.size());
        assertEquals("ABCD", text(read(lines.get(0))));
        assertEquals(" ABCD", text(read(lines.get(1))));
        assertEquals(2, obfuscated(read(lines.get(0))));
        assertEquals(0, obfuscated(read(lines.get(1))));
        ticks.set(120);
        assertEquals(0, obfuscated(read(lines.get(0))));
        assertEquals(2, obfuscated(read(lines.get(1))));
        assertEquals(Style.EMPTY, read(lines.get(1)).get(0).style());
    }

    @Test
    void adjacentGlitchSpansKeepIndependentParameters() {
        AtomicLong ticks = new AtomicLong();
        var line = wrap(Component.literal("§w2ABC§w5DEF"), 80, ticks).get(0);
        List<Seen> glyphs = read(line);
        assertEquals("ABCDEF", text(glyphs));
        assertEquals(2, glyphs.subList(0, 3).stream().filter(glyph -> glyph.style().isObfuscated()).count());
        assertEquals(3, glyphs.subList(3, 6).stream().filter(glyph -> glyph.style().isObfuscated()).count());
        assertTrue(glyphs.subList(0, 3).stream().allMatch(glyph -> glyph.style().getColor().getValue() == ChatFormatting.RED.getColor()));
        assertTrue(glyphs.subList(3, 6).stream().allMatch(glyph -> glyph.style().getColor().getValue() == ChatFormatting.RED.getColor()));
    }

    @Test
    void resetEndsGlitchSpanWithoutLeakingItsParameter() {
        AtomicLong ticks = new AtomicLong();
        var line = wrap(Component.literal("§w2ABC§rDEF"), 80, ticks).get(0);
        List<Seen> glyphs = read(line);
        assertEquals("ABCDEF", text(glyphs));
        assertEquals(2, glyphs.subList(0, 3).stream().filter(glyph -> glyph.style().isObfuscated()).count());
        assertEquals(0, glyphs.subList(3, 6).stream().filter(glyph -> glyph.style().isObfuscated()).count());
        assertTrue(glyphs.subList(3, 6).stream().allMatch(glyph -> glyph.style().getColor() == null));
    }

    @Test
    void cachedRainbowChangesWithTimeWithoutChangingNormalCharacters() {
        AtomicLong ticks = new AtomicLong();
        var cached = wrap(Component.literal("§v3流动ABC normal!?"), 80, ticks).get(0);
        List<Seen> first = read(cached);
        ticks.set(17);
        List<Seen> later = read(cached);
        assertEquals("流动ABC normal!?", text(first));
        assertEquals(text(first), text(later));
        assertNotEquals(first.get(0).style().getColor(), later.get(0).style().getColor());
        assertEquals(first.subList(6, first.size()), later.subList(6, later.size()));
        assertTrue(later.subList(6, later.size()).stream().allMatch(g -> g.style().equals(Style.EMPTY)));
    }

    @Test
    void originalStyleEventsFontAndInsertionSurviveWithoutLeakingMarkers() {
        Style base = Style.EMPTY.withBold(true).withItalic(true).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/say hello"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("hover")))
                .withFont(new ResourceLocation("transcend", "test_font"))
                .withInsertion("transcend:chat_glyph/0");
        Component source = Component.literal("§vAB").withStyle(base)
                .append(Component.literal("§rC").withStyle(Style.EMPTY.withInsertion("other")));
        AtomicLong ticks = new AtomicLong(41);
        var lines = wrap(source, 4, ticks);
        assertEquals("AB", text(read(lines.get(0)))); // 粗体宽度参与原版断行。
        for (Seen glyph : read(lines.get(0))) {
            assertEquals(base, glyph.style().withColor(base.getColor()));
        }
        List<Seen> second = read(lines.get(1));
        assertEquals(" C", text(second));
        assertNull(second.get(0).style().getInsertion());
        assertEquals("other", second.get(1).style().getInsertion());
        assertEquals(base.getClickEvent(), second.get(1).style().getClickEvent());
        assertEquals(base.getHoverEvent(), second.get(1).style().getHoverEvent());
        assertEquals(base.getFont(), second.get(1).style().getFont());
        assertEquals("§vAB§rC", source.getString());
        assertEquals(base, source.getStyle());
    }

    @Test
    void bidiOrderMirroringAndGlyphIdentityMatchVanillaLayout() {
        AtomicLong ticks = new AtomicLong();
        var cached = wrap(Component.literal("§w2אב(גד)"), 80, ticks).get(0);
        List<Seen> expected = read(FormattedBidiReorder.reorder(FormattedText.of("אב(גד)"), false));
        assertEquals(text(expected), text(read(cached)));
        assertEquals(List.of((int) 'ב', (int) 'א'), selected(read(cached)));
        ticks.set(30);
        assertEquals(2, obfuscated(read(cached)));
        assertEquals(text(expected), text(read(cached)));
    }

    @Test
    void countsExcludeCombiningMarksAndClampToEligibleGlyphs() {
        AtomicLong ticks = new AtomicLong();
        var line = wrap(FormattedText.of("§w2ÁBC normal"), 80, ticks).get(0);
        assertEquals("ÁBC normal", text(read(line)));
        assertEquals(List.of((int) 'A', (int) 'B'), selected(read(line)));
        ticks.set(30);
        assertEquals(List.of((int) 'B', (int) 'C'), selected(read(line)));
        assertEquals(0, obfuscated(read(wrap(FormattedText.of("§w0ABC"), 80, ticks).get(0))));
        assertEquals(3, obfuscated(read(wrap(FormattedText.of("§w99ABC"), 80, ticks).get(0))));
    }

    @Test
    void additionalVanillaGlyphsKeepTheirOwnStylesAndIndices() {
        Style extraStyle = Style.EMPTY.withInsertion("vanilla-added").withItalic(true);
        var line = com.huige233.transcend.client.ChatFormatting.wrap(FormattedText.of("§vABC"), layout -> {
            var sequence = vanillaLayout(layout, 80).get(0);
            return List.of(FormattedCharSequence.composite(
                    sink -> sink.accept(17, extraStyle, '>'), sequence));
        }, () -> 0).get(0);
        assertEquals(new Seen(17, '>', extraStyle), read(line).get(0));
        assertEquals(">ABC", text(read(line)));
        assertTrue(read(line).subList(1, 4).stream().allMatch(glyph -> glyph.style().getInsertion() == null));
    }

    @Test
    void wrappedSequenceIsFrozenOnceAndAcceptHonorsEarlyStop() {
        AtomicInteger traversals = new AtomicInteger();
        AtomicInteger tickReads = new AtomicInteger();
        var lines = com.huige233.transcend.client.ChatFormatting.wrap(Component.literal("§vABC"), layout -> {
            var sequence = vanillaLayout(layout, 80).get(0);
            return List.of(sink -> {
                traversals.incrementAndGet();
                return sequence.accept(sink);
            });
        }, () -> tickReads.incrementAndGet());
        assertEquals(1, traversals.get());
        assertFalse(lines.get(0).accept((index, style, codePoint) -> false));
        assertEquals(1, tickReads.get());
        read(lines.get(0));
        read(lines.get(0));
        assertEquals(1, traversals.get());
        assertEquals(3, tickReads.get());
    }

    private record Seen(int index, int codePoint, Style style) {}

    private static List<Seen> read(FormattedCharSequence sequence) {
        List<Seen> result = new ArrayList<>();
        assertTrue(sequence.accept((index, style, codePoint) -> {
            result.add(new Seen(index, codePoint, style));
            return true;
        }));
        return result;
    }

    private static String text(List<Seen> glyphs) {
        StringBuilder result = new StringBuilder();
        glyphs.forEach(glyph -> result.appendCodePoint(glyph.codePoint()));
        return result.toString();
    }

    private static long obfuscated(List<Seen> glyphs) {
        return glyphs.stream().filter(glyph -> glyph.style().isObfuscated()).count();
    }

    private static List<Integer> selected(List<Seen> glyphs) {
        return glyphs.stream().filter(glyph -> glyph.style().isObfuscated()).map(Seen::codePoint).toList();
    }
}
