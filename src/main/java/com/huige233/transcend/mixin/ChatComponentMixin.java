package com.huige233.transcend.mixin;

import com.huige233.transcend.client.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/** 只替换显示行，聊天历史始终保留原始消息。 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Redirect(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ComponentRenderUtils;wrapComponents(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/client/gui/Font;)Ljava/util/List;"))
    private List<FormattedCharSequence> transcend$wrapFormatting(FormattedText message, int width, Font font) {
        return ChatFormatting.wrap(message, width, font, () -> Minecraft.getInstance().gui.getGuiTicks());
    }
}
