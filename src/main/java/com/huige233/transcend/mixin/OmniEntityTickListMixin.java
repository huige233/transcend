package com.huige233.transcend.mixin;

import com.huige233.transcend.combat.protection.TranscendProtectionEngine;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTickList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityTickList.class, priority = 2147483647)
public class OmniEntityTickListMixin {
    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void transcend$protect(Entity entity, CallbackInfo ci) {
        if (entity instanceof net.minecraft.world.entity.LivingEntity living
                && TranscendProtectionEngine.shouldBlockIndexRemoval(living)) ci.cancel();
    }
}
