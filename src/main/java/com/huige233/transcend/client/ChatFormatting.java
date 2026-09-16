package com.huige233.transcend.client;

import com.huige233.transcend.util.FormattingUtil;
import com.huige233.transcend.util.FormattingUtil.Glyph;
import com.huige233.transcend.util.FormattingUtil.Span;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.LongSupplier;

/** 用临时字形标记跨越原版换行，每行独立保存动画数据。 */
public final class ChatFormatting {
    private ChatFormatting() {}

    public static List<FormattedCharSequence> wrap(FormattedText message, int width, Font font, LongSupplier ticks) {
        return wrap(message, layout -> ComponentRenderUtils.wrapComponents(layout, width, font), ticks);
    }

    static List<FormattedCharSequence> wrap(FormattedText message,
                                           Function<FormattedText, List<FormattedCharSequence>> wrapper,
                                           LongSupplier ticks) {
        if (!message.getString().contains("§")) return wrapper.apply(message);

        FormattingUtil.ParsedText parsed = FormattingUtil.parse(message);
        MutableComponent layout = Component.empty();
        Map<String, Glyph> markers = new HashMap<>();
        for (Glyph glyph : parsed.glyphs()) {
            String marker = "transcend:chat_glyph/" + markers.size();
            markers.put(marker, glyph);
            layout.append(Component.literal(new String(Character.toChars(glyph.codePoint())))
                    .withStyle(glyph.style().withInsertion(marker)));
        }

        List<FormattedCharSequence> result = new ArrayList<>();
        for (FormattedCharSequence line : wrapper.apply(layout)) {
            List<VisualGlyph> visualGlyphs = new ArrayList<>();
            line.accept((index, style, codePoint) -> {
                Glyph source = markers.get(style.getInsertion());
                Span span = source != null && source.span() >= 0 ? parsed.spans().get(source.span()) : null;
                // 保留原版缩进、双向排序及镜像字形，标记不进入最终样式。
                if (source == null) source = new Glyph(codePoint, index, style, -1, -1);
                visualGlyphs.add(new VisualGlyph(index, codePoint, source, span));
                return true;
            });
            result.add(animate(List.copyOf(visualGlyphs), ticks));
        }
        return result;
    }

    private static FormattedCharSequence animate(List<VisualGlyph> glyphs, LongSupplier ticks) {
        // 闭包只持有本行字形，不持有原消息、临时布局或标记表。
        return sink -> {
            long tick = ticks.getAsLong();
            for (VisualGlyph glyph : glyphs) {
                if (!sink.accept(glyph.index(), FormattingUtil.ParsedText.styleAt(glyph.source(), glyph.span(), tick),
                        glyph.codePoint())) return false;
            }
            return true;
        };
    }

    private record VisualGlyph(int index, int codePoint, Glyph source, Span span) {}
}
