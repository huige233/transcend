package com.huige233.transcend.util;

import net.minecraft.ChatFormatting;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Server-safe registry and bounded ASCII parameter scanner for section-code effects. */
public final class FormattingEffectRegistry {
    public static final char FLOWING_RAINBOW = 'v';
    public static final char RED_GLITCH = 'w';
    private static final Map<Character, FormattingEffect> EFFECTS = new ConcurrentHashMap<>();

    static {
        register(FLOWING_RAINBOW, FormattingEffect.RAINBOW);
        register(RED_GLITCH, FormattingEffect.RED_GLITCH);
    }

    private FormattingEffectRegistry() {}

    public static void register(char code, FormattingEffect effect) {
        if (code == '§' || effect == null || ChatFormatting.getByCode(code) != null) {
            throw new IllegalArgumentException("Invalid formatting effect registration");
        }
        EFFECTS.put(Character.toLowerCase(code), effect);
    }

    public static FormattingEffect get(char code) {
        return EFFECTS.get(Character.toLowerCase(code));
    }

    public static int parameter(String text, int codeIndex, int defaultValue) {
        int end = parameterEnd(text, codeIndex);
        if (end == codeIndex + 1) return defaultValue;
        long value = 0;
        for (int i = codeIndex + 1; i < end; i++) {
            value = Math.min(Integer.MAX_VALUE, value * 10 + text.charAt(i) - '0');
        }
        // Count is NOT a density or a speed tier: never cap §w at 20 characters.
        return get(text.charAt(codeIndex)) == FormattingEffect.RAINBOW
                ? (int) Math.min(20, value) : (int) value;
    }

    public static int parameterEnd(String text, int codeIndex) {
        int end = codeIndex + 1;
        while (end < text.length() && text.charAt(end) >= '0' && text.charAt(end) <= '9') end++;
        return end;
    }

    public static int defaultParameter(FormattingEffect effect) {
        return 1;
    }

    public enum FormattingEffect { RAINBOW, RED_GLITCH }
}
