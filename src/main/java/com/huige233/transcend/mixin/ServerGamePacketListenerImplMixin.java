package com.huige233.transcend.mixin;

import com.huige233.transcend.items.tools.TranscendSword;
import com.huige233.transcend.util.PhaseGuard;
import com.huige233.transcend.util.TranscendPickFlag;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;
import java.util.stream.Stream;

/** 为服务端移动和交互包设置相位及特殊选取上下文，同步飞行意图并保留告示牌格式码。 */
@Mixin(ServerGamePacketListenerImpl.class)

public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void transcend$beginPlayerMove(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        PhaseGuard.beginMovePacket(this.player);
        
        if (PhaseGuard.isPhaseActiveForMove(this.player)) {
            this.player.noPhysics = true;
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void transcend$endPlayerMove(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        PhaseGuard.endMovePacket(this.player);
    }

                                                  
                                                                      
    @Inject(method = "handlePlayerAbilities", at = @At("RETURN"))
    private void transcend$syncPhaseFlightFromPacket(ServerboundPlayerAbilitiesPacket packet, CallbackInfo ci) {
        if (this.player == null) return;
        
        this.player.getPersistentData().putBoolean("transcend_phase_flying", packet.isFlying());
    }

    @Redirect(method = "handleSignUpdate",
              at = @At(value = "INVOKE",
                       target = "Ljava/util/stream/Stream;map(Ljava/util/function/Function;)Ljava/util/stream/Stream;"))
    private Stream<String> transcend$keepFormattingOnSigns(Stream<String> stream, Function<String, String> mapper) {
        return stream;
    }

    @Inject(method = "handleInteract", at = @At("HEAD"))
    private void transcend$beforeInteract(ServerboundInteractPacket packet, CallbackInfo ci) {
        if (this.player.getMainHandItem().getItem() instanceof TranscendSword) {
            TranscendPickFlag.set(true);
        }
    }

    @Inject(method = "handleInteract", at = @At("RETURN"))
    private void transcend$afterInteract(ServerboundInteractPacket packet, CallbackInfo ci) {
        TranscendPickFlag.set(false);
    }
}
