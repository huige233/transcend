package com.huige233.transcend.mixin;

import com.huige233.transcend.util.FormattingUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps dynamic formatting out of the cached line, and reapplies it when ChatComponent renders. */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    private static final Map<String, FormattingUtil.ParsedText> TRANSCEND$PARSED = new ConcurrentHashMap<>();

    @Redirect(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ComponentRenderUtils;wrapComponents(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/client/gui/Font;)Ljava/util/List;"))
    private List<FormattedCharSequence> transcend$rememberFormatting(FormattedText message, int width, Font font) {
        if (message instanceof Component component && component.getString().contains("§")) {
            FormattingUtil.ParsedText parsed = FormattingUtil.parse(component);
            TRANSCEND$PARSED.put(parsed.component().getString(), parsed);
            message = parsed.component();
        }
        return ComponentRenderUtils.wrapComponents(message, width, font);
    }

    @Redirect(method = "render(Lnet/minecraft/client/gui/GuiGraphics;III)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"))
    private int transcend$animateCachedLine(GuiGraphics graphics, Font font, FormattedCharSequence line,
                                            int x, int y, int color) {
        StringBuilder plain = new StringBuilder();
        line.accept((index, style, codePoint) -> { plain.appendCodePoint(codePoint); return true; });
        FormattingUtil.ParsedText parsed = TRANSCEND$PARSED.get(plain.toString());
        int offset = 0;
        if (parsed == null) {
            for (Map.Entry<String, FormattingUtil.ParsedText> entry : TRANSCEND$PARSED.entrySet()) {
                int found = entry.getKey().indexOf(plain.toString());
                if (found >= 0) { parsed = entry.getValue(); offset = entry.getKey().substring(0, found).codePointCount(0, found); break; }
            }
        }
        if (parsed == null) return graphics.drawString(font, line, x, y, color);
        long tick = Minecraft.getInstance().gui.getGuiTicks();
        int length = plain.codePointCount(0, plain.length());
        return graphics.drawString(font, parsed.plainSequence(offset, offset + length, () -> tick), x, y, color);
    }
}
