package com.huige233.transcend.mixin;

import com.huige233.transcend.items.tools.TranscendSword;
import com.huige233.transcend.util.TranscendPickFlag;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
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

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

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
