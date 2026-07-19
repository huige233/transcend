package com.huige233.transcend.mixin;

import com.huige233.transcend.util.FormattingUtil;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin {

    @ModifyVariable(method = "setText", at = @At("HEAD"), argsOnly = true)
    private SignText transcend$parseFormattingCodes(SignText text) {
        return FormattingUtil.parseSignText(text);
    }
}
