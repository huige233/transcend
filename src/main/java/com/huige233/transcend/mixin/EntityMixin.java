package com.huige233.transcend.mixin;

import com.huige233.transcend.mixinitf.ITranscendMarked;
import com.huige233.transcend.util.PhaseGuard;
import com.huige233.transcend.util.TranscendGuard;
import com.huige233.transcend.util.TranscendPickFlag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

   
                                                       
                                                          
   
/** 在实体底层拦截受保护目标的移除和危险位移，维护相位移动限制，并调整目标选取与无敌判定。 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void transcend$bypassInvulnerable(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ITranscendMarked marked && marked.transcend$isMarked()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void transcend$guardRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        if (reason != Entity.RemovalReason.KILLED && reason != Entity.RemovalReason.DISCARDED) {
            return;
        }
        if ((Object) this instanceof LivingEntity le && TranscendGuard.isProtected(le)) {
            ci.cancel();
        }
    }

    
    @Inject(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"), cancellable = true)
    private void transcend$blockExternalPhaseMove(net.minecraft.world.entity.MoverType type,
                                                  net.minecraft.world.phys.Vec3 delta, CallbackInfo ci) {
        if (!((Object) this instanceof Player p)) return;
        if (PhaseGuard.isPhaseActiveForMove(p)) {
            p.noPhysics = true;
            return;
        }
        if (PhaseGuard.blocksForces(p)) {
            ci.cancel();
        }
    }

    
    @Inject(method = "setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"), cancellable = true)
    private void transcend$blockExternalPhaseForce(net.minecraft.world.phys.Vec3 delta, CallbackInfo ci) {
        if ((Object) this instanceof Player p && PhaseGuard.blocksForces(p)) {
            ci.cancel();
        }
    }

    
    @Inject(method = "setPosRaw(DDD)V", at = @At("HEAD"), cancellable = true)
    private void transcend$blockExternalPhasePos(double x, double y, double z, CallbackInfo ci) {
        if (!((Object) this instanceof Player p)
                || !PhaseGuard.blocksTeleports(p)
                || PhaseGuard.isInnerMovePositionUpdate()) {
            return;
        }
        if (Double.compare(p.position().x, x) != 0
                || Double.compare(p.position().y, y) != 0
                || Double.compare(p.position().z, z) != 0) {
            ci.cancel();
        }
    }

    
    @Inject(method = "setPos(DDD)V", at = @At("HEAD"), cancellable = true)
    private void transcend$guardUnsafePosition(double x, double y, double z, CallbackInfo ci) {
        if (!((Object) this instanceof Player player) || !TranscendGuard.isProtected(player)) return;
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || y < player.level().getMinBuildHeight()
                || !player.level().getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(x, y, z))) {
            TranscendGuard.enforce(player);
            ci.cancel();
        }
    }

    @Inject(method = "changeDimension(Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/entity/Entity;",
            at = @At("HEAD"), cancellable = true)
    private void transcend$blockExternalPhaseDimension(ServerLevel destination, CallbackInfoReturnable<Entity> cir) {
        if ((Object) this instanceof Player p && PhaseGuard.blocksTeleports(p)) {
            cir.setReturnValue((Entity) (Object) this);
        }
    }

    @Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true)
    private void transcend$guardSetRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        if (reason != Entity.RemovalReason.KILLED && reason != Entity.RemovalReason.DISCARDED) {
            return;
        }
        if ((Object) this instanceof LivingEntity le && TranscendGuard.isProtected(le)) {
            ci.cancel();
        }
    }

    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
    private void transcend$spectatorPickable(CallbackInfoReturnable<Boolean> cir) {
        if (TranscendPickFlag.isActive()) {
            cir.setReturnValue(true);
            return;
        }
        if ((Object) this instanceof LivingEntity living && TranscendGuard.isProtected(living)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isAttackable", at = @At("HEAD"), cancellable = true)
    private void transcend$spectatorAttackable(CallbackInfoReturnable<Boolean> cir) {
        if (TranscendPickFlag.isActive()) {
            cir.setReturnValue(true);
            return;
        }
        if ((Object) this instanceof LivingEntity living && TranscendGuard.isProtected(living)) {
            cir.setReturnValue(false);
        }
    }

    
    @Inject(method = "canBeHitByProjectile", at = @At("HEAD"), cancellable = true)
    private void transcend$projectileTargeting(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof LivingEntity living && TranscendGuard.isProtected(living)) {
            cir.setReturnValue(false);
        }
    }
}
