package com.huige233.transcend.mixin;

import com.huige233.transcend.combat.protection.TranscendProtectionEngine;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntitySection.class, priority = 2147483647)
public class OmniEntitySectionMixin {
    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private <T extends EntityAccess> void transcend$protect(T entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Entity e && e instanceof net.minecraft.world.entity.LivingEntity living
                && TranscendProtectionEngine.isProtected(living)) cir.setReturnValue(false);
    }
}
