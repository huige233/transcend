package com.huige233.transcend.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.SignText;

import java.util.ArrayList;
import java.util.List;

public class FormattingUtil {

    public static MutableComponent parseFormattedString(String text) {
        MutableComponent result = Component.empty();
        Style currentStyle = Style.EMPTY;
        StringBuilder segment = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                char code = text.charAt(i + 1);
                ChatFormatting formatting = ChatFormatting.getByCode(code);
                if (formatting != null) {
                    if (segment.length() > 0) {
                        result.append(Component.literal(segment.toString()).withStyle(currentStyle));
                        segment.setLength(0);
                    }
                    currentStyle = applyFormatting(currentStyle, formatting);
                    i++;
                    continue;
                }
            }
            segment.append(c);
        }

        if (segment.length() > 0) {
            result.append(Component.literal(segment.toString()).withStyle(currentStyle));
        }

        return result;
    }

    public static Component parseComponentFormatting(Component component) {
        String text = component.getString();
        if (!text.contains("§")) {
            return component;
        }
        return parseFormattedString(text);
    }

    public static SignText parseSignText(SignText text) {
        SignText result = text;
        for (int i = 0; i < 4; i++) {
            Component message = result.getMessage(i, false);
            Component filteredMessage = result.getMessage(i, true);
            Component parsedMessage = parseComponentFormatting(message);
            Component parsedFiltered = parseComponentFormatting(filteredMessage);
            if (parsedMessage != message || parsedFiltered != filteredMessage) {
                result = result.setMessage(i, parsedMessage, parsedFiltered);
            }
        }
        return result;
    }

    public static FormattedCharSequence formatEditBoxText(String fullText, String visibleText, int firstCharPos) {
        if (!fullText.contains("§")) {
            return FormattedCharSequence.forward(visibleText, Style.EMPTY);
        }

        Style startStyle = Style.EMPTY;
        for (int i = 0; i < firstCharPos && i < fullText.length(); i++) {
            char c = fullText.charAt(i);
            if (c == ' ') {
                startStyle = Style.EMPTY;
            } else if (c == '§' && i + 1 < fullText.length()) {
                char code = fullText.charAt(i + 1);
                ChatFormatting formatting = ChatFormatting.getByCode(code);
                if (formatting != null) {
                    startStyle = applyFormatting(startStyle, formatting);
                    i++;
                }
            }
        }

        List<FormattedCharSequence> parts = new ArrayList<>();
        Style currentStyle = startStyle;
        StringBuilder segment = new StringBuilder();

        for (int i = 0; i < visibleText.length(); i++) {
            char c = visibleText.charAt(i);
            if (c == '§' && i + 1 < visibleText.length()) {
                char code = visibleText.charAt(i + 1);
                ChatFormatting formatting = ChatFormatting.getByCode(code);
                if (formatting != null) {
                    if (segment.length() > 0) {
                        parts.add(FormattedCharSequence.forward(segment.toString(), currentStyle));
                        segment.setLength(0);
                    }
                    currentStyle = applyFormatting(currentStyle, formatting);
                    i++;
                    continue;
                }
            }
            if (c == ' ') {
                segment.append(c);
                parts.add(FormattedCharSequence.forward(segment.toString(), currentStyle));
                segment.setLength(0);
                currentStyle = Style.EMPTY;
                continue;
            }
            segment.append(c);
        }

        if (segment.length() > 0) {
            parts.add(FormattedCharSequence.forward(segment.toString(), currentStyle));
        }

        return FormattedCharSequence.composite(parts);
    }

    public static String processChatMessage(String message) {
        if (!message.contains("§")) {
            return message;
        }
        StringBuilder result = new StringBuilder(message.length() + 16);
        boolean hasActiveStyle = false;

        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == '§' && i + 1 < message.length()) {
                char code = message.charAt(i + 1);
                ChatFormatting formatting = ChatFormatting.getByCode(code);
                if (formatting != null) {
                    if (formatting == ChatFormatting.RESET) {
                        hasActiveStyle = false;
                    } else {
                        hasActiveStyle = true;
                    }
                    result.append('§').append(code);
                    i++;
                    continue;
                }
            }
            if (c == ' ' && hasActiveStyle) {
                result.append(' ').append("§r");
                hasActiveStyle = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static Style applyFormatting(Style style, ChatFormatting formatting) {
        if (formatting == ChatFormatting.RESET) {
            return Style.EMPTY;
        } else if (formatting.isColor()) {
            return Style.EMPTY.withColor(formatting);
        } else {
            return style.applyFormat(formatting);
        }
    }
}
