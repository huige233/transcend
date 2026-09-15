package com.huige233.transcend.mixin.client;

import com.huige233.transcend.util.PhaseGuard;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

   
             
  
                                                            
                                                                          
                                                                            
                                                               
                                                                 
                                                     
  
                                                              
   
/** 将本地玩家行为更新标记为自主移动以放行飞行升降输入，并在相位飞行卡墙时尝试维持飞行状态。 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerPhaseMovementMixin {

    
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void transcend$beginSelfAiStep(CallbackInfo ci) {
        PhaseGuard.beginSelf((LocalPlayer) (Object) this);
    }

    @Inject(method = "aiStep", at = @At("RETURN"))
    private void transcend$endSelfAiStep(CallbackInfo ci) {
        PhaseGuard.endSelf((LocalPlayer) (Object) this);
    }

                                                                        
                                                     
                                                       
    @Inject(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;onUpdateAbilities()V",
                    ordinal = 1,
                    shift = At.Shift.BEFORE))
    private void transcend$rejectPhaseFlightToggleInsideWall(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (!PhaseGuard.isPhaseEnabled(player)) return;
        if (!player.getAbilities().flying) return;
        if (!player.level().noCollision(player, player.getBoundingBox())) {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }
    }
}