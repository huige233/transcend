package com.huige233.transcend.mixin;

import com.huige233.transcend.combat.protection.TranscendProtectionEngine;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClassInstanceMultiMap.class, priority = 2147483647)
public class OmniClassInstanceMultiMapMixin {
    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void transcend$protect(Object entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Entity e && e instanceof net.minecraft.world.entity.LivingEntity living
                && TranscendProtectionEngine.isProtected(living)) cir.setReturnValue(false);
    }
}
