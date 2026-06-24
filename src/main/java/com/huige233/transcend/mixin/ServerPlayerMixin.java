package com.huige233.transcend.mixin;

import com.huige233.transcend.util.TranscendGuard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 被超越装备保护期间:ServerPlayer.die 完全空转。
 *
 * <p>必要性:{@code ServerPlayer.die} 覆写了 {@code LivingEntity.die},并在调用 {@code super.die()} <b>之前</b>
 * 就把死亡画面包 {@code ClientboundPlayerCombatKillPacket} 发给了客户端。只拦 LivingEntity.die 挡得住「真死」,
 * 却挡不住这个包 —— 客户端照样弹死亡画面。所以必须在 ServerPlayer.die 的 HEAD 直接空转。</p>
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void transcend$guardServerDie(DamageSource source, CallbackInfo ci) {
        if (TranscendGuard.isProtected((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
