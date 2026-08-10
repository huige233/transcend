package com.huige233.transcend.mixin;

import com.huige233.transcend.util.TranscendGuard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
/** 服务端玩家 mixin。 */
public abstract class ServerPlayerMixin {

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void transcend$guardServerDie(DamageSource source, CallbackInfo ci) {
        if (TranscendGuard.isProtected((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
