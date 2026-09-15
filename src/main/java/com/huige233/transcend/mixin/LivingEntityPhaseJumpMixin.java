package com.huige233.transcend.mixin;

import com.huige233.transcend.util.PhaseGuard;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

   
                                                                      
  
                                                                                
                                                                                              
                                                         
  
                                                                   
                                                                             
   
/** 将玩家地面起跳包裹为自主移动上下文，避免相位锁定拦截起跳冲量。 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityPhaseJumpMixin {

    @Inject(method = "jumpFromGround", at = @At("HEAD"))
    private void transcend$beginSelfJump(CallbackInfo ci) {
        if ((Object) this instanceof Player p) {
            PhaseGuard.beginSelf(p);
        }
    }

    @Inject(method = "jumpFromGround", at = @At("RETURN"))
    private void transcend$endSelfJump(CallbackInfo ci) {
        if ((Object) this instanceof Player p) {
            PhaseGuard.endSelf(p);
        }
    }
}