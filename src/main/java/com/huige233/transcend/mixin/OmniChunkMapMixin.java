package com.huige233.transcend.mixin;

import com.huige233.transcend.combat.protection.TranscendProtectionEngine;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChunkMap.class, priority = 2147483647)
public class OmniChunkMapMixin {
    @Inject(method = "removeEntity", at = @At("HEAD"), cancellable = true)
    private void transcend$protect(Entity entity, CallbackInfo ci) {
        if (entity instanceof net.minecraft.world.entity.LivingEntity living
                && TranscendProtectionEngine.shouldBlockIndexRemoval(living)) ci.cancel();
    }
}
