package com.huige233.transcend.mixin;

import com.huige233.transcend.combat.protection.TranscendProtectionEngine;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 在服务端真实索引回调入口拦截绕过 Entity.remove 的直接清除。 */
@Mixin(targets = "net.minecraft.world.level.entity.PersistentEntitySectionManager$Callback")
public abstract class PersistentEntityCallbackMixin {
    @Shadow @Final private EntityAccess entity;

    @Inject(method = "onRemove", at = @At("HEAD"), cancellable = true)
    private void transcend$guardWorldRemoval(Entity.RemovalReason reason, CallbackInfo ci) {
        if (entity instanceof LivingEntity living
                && TranscendProtectionEngine.shouldBlockRemoval(living, reason)) {
            ci.cancel();
        }
    }
}
