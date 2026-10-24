package com.huige233.transcend.mixin;

import com.huige233.transcend.combat.protection.TranscendProtectionEngine;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** 在服务端真实索引回调入口拦截绕过 Entity.remove 的直接清除。 */
@Mixin(targets = "net.minecraft.world.level.entity.PersistentEntitySectionManager$Callback")
public abstract class PersistentEntityCallbackMixin {
    @Shadow @Final private EntityAccess entity;

    @WrapMethod(method = "onRemove")
    private void transcend$guardWorldRemoval(Entity.RemovalReason reason, Operation<Void> original) {
        if (entity instanceof LivingEntity living
                && TranscendProtectionEngine.shouldBlockRemoval(living, reason)) {
            return;
        }
        if (entity instanceof Entity actual) {
            TranscendProtectionEngine.runIndexTransition(actual, () -> original.call(reason));
        } else original.call(reason);
    }

    @WrapMethod(method = "onMove")
    private void transcend$allowSectionTransition(Operation<Void> original) {
        if (entity instanceof Entity actual) {
            TranscendProtectionEngine.runIndexTransition(actual, () -> original.call());
        } else original.call();
    }
}
