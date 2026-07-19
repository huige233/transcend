package com.huige233.transcend.mixin;

import com.huige233.transcend.items.tools.TranscendSword;
import com.huige233.transcend.util.TranscendPickFlag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "pick", at = @At("HEAD"))
    private void transcend$beforePick(float partialTicks, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.getMainHandItem().getItem() instanceof TranscendSword) {
            TranscendPickFlag.set(true);
        }
    }

    @Inject(method = "pick", at = @At("RETURN"))
    private void transcend$afterPick(float partialTicks, CallbackInfo ci) {
        TranscendPickFlag.set(false);
    }
}
