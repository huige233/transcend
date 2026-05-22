package com.huige233.transcend.mixin;

import com.huige233.transcend.util.FormattingUtil;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Shadow
    protected EditBox input;

    @Inject(method = "init", at = @At("TAIL"))
    private void transcend$addFormattingRenderer(CallbackInfo ci) {
        this.input.setFormatter((text, firstCharPos) ->
                FormattingUtil.formatEditBoxText(this.input.getValue(), text, firstCharPos));
    }

    @ModifyVariable(method = "handleChatInput", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String transcend$processFormattingCodes(String message) {
        return FormattingUtil.processChatMessage(message);
    }
}
