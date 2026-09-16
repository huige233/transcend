package com.huige233.transcend.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.SignText;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;

import static com.huige233.transcend.util.FormattingEffectRegistry.FormattingEffect;

/** Shared, server-safe parsing. Animation changes styles, never the source text or glyph advance. */
public final class FormattingUtil {
    private FormattingUtil() {}

    public record Span(FormattingEffect effect, int parameter, int eligibleLength) {}
    public record Glyph(int codePoint, int sourceIndex, Style style, int span, int effectIndex) {}

    public record ParsedText(List<Glyph> glyphs, List<Span> spans, int sourceLength) {
        public ParsedText {
            glyphs = List.copyOf(glyphs);
            spans = List.copyOf(spans);
        }

        public Style styleAt(Glyph glyph, long tick) {
            return styleAt(glyph, glyph.span() < 0 ? null : spans.get(glyph.span()), tick);
        }

        public static Style styleAt(Glyph glyph, Span span, long tick) {
            if (span == null) return glyph.style();
            if (span.effect() == FormattingEffect.RAINBOW) {
                // Default: one cycle per 20 seconds. Bound before multiplying to avoid overflow.
                float hue = (glyph.effectIndex() * 0.08F
                        + (Math.floorMod(tick, 400L) * span.parameter() % 400L) / 400.0F) % 1.0F;
                return glyph.style().withColor(java.awt.Color.HSBtoRGB(hue, 0.9F, 1.0F) & 0xFFFFFF);
            }
            int length = span.eligibleLength();
            int count = Math.min(span.parameter(), length);
            int start = length == 0 ? 0 : (int) Math.floorMod(Math.floorDiv(tick, 30L), length);
            boolean selected = glyph.effectIndex() >= 0 && length > 0
                    && Math.floorMod(glyph.effectIndex() - start, length) < count;
            // Vanilla Font picks a random glyph from the ORIGINAL glyph's width bucket.
            // Do not replace codepoints, or obfuscate the entire span (which defeats the count).
            return glyph.style().withColor(ChatFormatting.RED).withObfuscated(selected);
        }

        public FormattedCharSequence plainSequence(int plainStart, int plainEnd, LongSupplier ticks) {
            int from = Math.max(0, plainStart), to = Math.min(glyphs.size(), plainEnd);
            return sink -> {
                long tick = ticks.getAsLong();
                for (int i = from; i < to; i++) {
                    Glyph glyph = glyphs.get(i);
                    if (!sink.accept(i - from, styleAt(glyph, tick), glyph.codePoint())) return false;
                }
                return true;
            };
        }

        public FormattedCharSequence sequence(int sourceStart, int sourceEnd, LongSupplier ticks) {
            int start = Math.max(0, sourceStart);
            int end = Math.min(sourceLength, sourceEnd);
            return sink -> {
                long tick = ticks.getAsLong();
                for (Glyph glyph : glyphs) {
                    if (glyph.sourceIndex() >= start && glyph.sourceIndex() < end
                            && !sink.accept(glyph.sourceIndex() - start, styleAt(glyph, tick), glyph.codePoint())) {
                        return false;
                    }
                }
                return true;
            };
        }

        public MutableComponent component() {
            MutableComponent result = Component.empty();
            for (Glyph glyph : glyphs) {
                // Static components/signs never persist a frame-generated scramble.
                Style style = glyph.style();
                if (glyph.span() >= 0 && spans.get(glyph.span()).effect() == FormattingEffect.RED_GLITCH) {
                    style = style.withColor(ChatFormatting.RED).withObfuscated(false);
                }
                result.append(Component.literal(new String(Character.toChars(glyph.codePoint()))).withStyle(style));
            }
            return result;
        }
    }

    public static ParsedText parse(String text) {
        return parse(FormattedText.of(text));
    }

    public static ParsedText parse(FormattedText text) {
        StringBuilder source = new StringBuilder();
        List<Style> styles = new ArrayList<>();
        text.visit((style, part) -> {
            source.append(part);
            for (int i = 0; i < part.length(); i++) styles.add(style);
            return Optional.empty();
        }, Style.EMPTY);
        List<Glyph> glyphs = new ArrayList<>();
        List<Span> spans = new ArrayList<>();
        Style inline = Style.EMPTY;
        int activeSpan = -1;
        for (int i = 0; i < source.length();) {
            if (source.charAt(i) == '§' && i + 1 < source.length()) {
                char code = source.charAt(i + 1);
                ChatFormatting vanilla = ChatFormatting.getByCode(code);
                FormattingEffect effect = FormattingEffectRegistry.get(code);
                if (vanilla != null) {
                    inline = inline.applyLegacyFormat(vanilla);
                    if (vanilla == ChatFormatting.RESET || vanilla.isColor()) activeSpan = -1;
                    i += 2;
                    continue;
                }
                if (effect != null) {
                    String raw = source.toString();
                    int parameter = FormattingEffectRegistry.parameter(raw, i + 1,
                            FormattingEffectRegistry.defaultParameter(effect));
                    spans.add(new Span(effect, parameter, 0));
                    activeSpan = spans.size() - 1;
                    i = FormattingEffectRegistry.parameterEnd(raw, i + 1);
                    continue;
                }
            }
            int cp = Character.codePointAt(source, i);
            if (Character.isSurrogate(source.charAt(i)) && Character.charCount(cp) == 1) cp = 0xFFFD;
            Style style = inline.applyTo(styles.get(i));
            int effectIndex = -1;
            if (activeSpan >= 0) {
                Span span = spans.get(activeSpan);
                if (eligible(cp)) {
                    effectIndex = span.eligibleLength();
                    spans.set(activeSpan, new Span(span.effect(), span.parameter(), effectIndex + 1));
                }
            }
            glyphs.add(new Glyph(cp, i, style, activeSpan, effectIndex));
            i += Character.charCount(cp);
            // Preserve the existing word-local formatting behavior, shared with the sending path.
            if (cp == ' ') {
                inline = Style.EMPTY;
                activeSpan = -1;
            }
        }
        return new ParsedText(glyphs, spans, source.length());
    }

    private static boolean eligible(int cp) {
        int type = Character.getType(cp);
        return !Character.isWhitespace(cp) && !Character.isSpaceChar(cp) && !Character.isISOControl(cp)
                && type != Character.FORMAT && type != Character.NON_SPACING_MARK
                && type != Character.ENCLOSING_MARK && type != Character.COMBINING_SPACING_MARK;
    }

    public static MutableComponent parseFormattedString(String text) {
        return parse(text).component();
    }

    public static Component parseComponentFormatting(Component component) {
        return component.getString().contains("§") ? parse(component).component() : component;
    }

    public static SignText parseSignText(SignText text) {
        SignText result = text;
        for (int i = 0; i < 4; i++) {
            Component message = result.getMessage(i, false);
            Component filtered = result.getMessage(i, true);
            Component parsed = parseComponentFormatting(message);
            Component parsedFiltered = parseComponentFormatting(filtered);
            if (parsed != message || parsedFiltered != filtered) result = result.setMessage(i, parsed, parsedFiltered);
        }
        return result;
    }

    public static FormattedCharSequence formatEditBoxText(String fullText, String visibleText, int firstCharPos) {
        return editBoxSequence(fullText, visibleText, firstCharPos, () -> System.currentTimeMillis() / 50L);
    }

    public static FormattedCharSequence formatEditBoxText(String fullText, String visibleText, int firstCharPos, long tick) {
        return editBoxSequence(fullText, visibleText, firstCharPos, () -> tick);
    }

    private static FormattedCharSequence editBoxSequence(String fullText, String visibleText, int firstCharPos, LongSupplier ticks) {
        if (!fullText.contains("§")) return FormattedCharSequence.forward(visibleText, Style.EMPTY);
        int start = Math.max(0, Math.min(firstCharPos, fullText.length()));
        int end = start + Math.min(visibleText.length(), fullText.length() - start);
        // Parse full source first: scrolling/cursor splits inside §v123 or a surrogate cannot leak digits.
        return parse(fullText).sequence(start, end, ticks);
    }

    public static String processChatMessage(String message) {
        if (!message.contains("§")) return message;
        StringBuilder result = new StringBuilder(message.length() + 16);
        boolean hasActiveStyle = false;
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == '§' && i + 1 < message.length()) {
                char code = message.charAt(i + 1);
                ChatFormatting formatting = ChatFormatting.getByCode(code);
                if (formatting != null || FormattingEffectRegistry.get(code) != null) {
                    hasActiveStyle = formatting != ChatFormatting.RESET;
                    result.append('§').append(code);
                    i++;
                    continue;
                }
            }
            if (c == ' ' && hasActiveStyle) {
                result.append(' ').append("§r");
                hasActiveStyle = false;
            } else result.append(c);
        }
        return result.toString();
    }
}
