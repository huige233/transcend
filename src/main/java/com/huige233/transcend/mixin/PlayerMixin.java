package com.huige233.transcend.mixin;

import com.huige233.transcend.util.PhaseGuard;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/** 在玩家刻中恢复相位飞行状态，并将移动与行为更新包裹为自主移动以放行自身速度变化。 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;updateIsUnderwater()Z",
            shift = At.Shift.BEFORE))
    private void transcend$enforcePhaseForTick(CallbackInfo ci) {
        Player p = (Player) (Object) this;
        
        PhaseGuard.syncPhaseFlight(p);
        PhaseGuard.enforcePhaseForTick(p);
    }

    @Inject(method = "travel", at = @At("HEAD"))
    private void transcend$beginSelfTravel(net.minecraft.world.phys.Vec3 travel, CallbackInfo ci) {
        PhaseGuard.beginSelf((Player) (Object) this);
    }

    @Inject(method = "travel", at = @At("RETURN"))
    private void transcend$endSelfTravel(net.minecraft.world.phys.Vec3 travel, CallbackInfo ci) {
        PhaseGuard.endSelf((Player) (Object) this);
    }

                                                                    
                                                                  
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void transcend$beginSelfAiStep(CallbackInfo ci) {
        PhaseGuard.beginSelf((Player) (Object) this);
    }

    @Inject(method = "aiStep", at = @At("RETURN"))
    private void transcend$endSelfAiStep(CallbackInfo ci) {
        PhaseGuard.endSelf((Player) (Object) this);
    }
}