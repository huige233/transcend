package com.huige233.transcend.mixin;

import net.minecraft.SharedConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 修改聊天字符合法性检查，允许输入节号格式码字符。 */
@Mixin(SharedConstants.class)

public abstract class SharedConstantsMixin {

    @Inject(method = "isAllowedChatCharacter", at = @At("HEAD"), cancellable = true)
    private static void transcend$allowSectionSign(char c, CallbackInfoReturnable<Boolean> cir) {
        if (c == '§') {
            cir.setReturnValue(true);
        }
    }
}
