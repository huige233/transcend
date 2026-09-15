package com.huige233.transcend.mixin;

import com.huige233.transcend.util.TranscendGuard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/** 拦截受保护玩家的死亡实体事件广播，并重新施加保护状态。 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "broadcastEntityEvent", at = @At("HEAD"), cancellable = true)
    private void transcend$guardDeathBroadcast(Entity entity, byte eventId, CallbackInfo ci) {
        if (eventId == 3 && entity instanceof Player player && TranscendGuard.isProtected(player)) {
            TranscendGuard.enforce(player);
            ci.cancel();
        }
    }

}
