package com.huige233.transcend.mixin;

import com.huige233.transcend.util.TranscendGuard;
import com.huige233.transcend.combat.protection.TranscendProtectionEngine;
import com.huige233.transcend.combat.attack.TranscendKillSettlement;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 取消受保护服务端玩家的死亡处理。 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @WrapMethod(method = "die")
    private void transcend$settlePlayerDeathOnce(DamageSource source, Operation<Void> original) {
        TranscendKillSettlement.runDeath((LivingEntity) (Object) this, () -> original.call(source));
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void transcend$guardServerDie(DamageSource source, CallbackInfo ci) {
        if (TranscendGuard.isProtected((LivingEntity) (Object) this)) {
            TranscendProtectionEngine.repair((LivingEntity) (Object) this);
            ci.cancel();
        }
    }
}
