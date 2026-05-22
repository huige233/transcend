package com.huige233.transcend.mixin;

import com.huige233.transcend.mixinitf.ITranscendMarked;
import com.huige233.transcend.util.TranscendPickFlag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void transcend$bypassInvulnerable(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ITranscendMarked marked && marked.transcend$isMarked()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
    private void transcend$spectatorPickable(CallbackInfoReturnable<Boolean> cir) {
        if (TranscendPickFlag.isActive()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isAttackable", at = @At("HEAD"), cancellable = true)
    private void transcend$spectatorAttackable(CallbackInfoReturnable<Boolean> cir) {
        if (TranscendPickFlag.isActive()) {
            cir.setReturnValue(true);
        }
    }
}
