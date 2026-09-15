package com.huige233.transcend.mixin;

import com.huige233.transcend.mixinitf.ITranscendMarked;
import com.huige233.transcend.combat.protection.TranscendProtectionEngine;
import com.huige233.transcend.util.TranscendDefense;
import com.huige233.transcend.util.TranscendEditService;
import com.huige233.transcend.util.TranscendGuard;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 为生物注入必死标记、免伤锁血与负面效果防护，应用编辑器强制返回值并维护传送锁和虚空囚禁计时。 */
@Mixin(LivingEntity.class)

public abstract class LivingEntityMixin extends Entity implements ITranscendMarked {

    @Unique
    private boolean transcend$marked = false;

    @Shadow
    protected abstract void actuallyHurt(DamageSource source, float amount);

    @Shadow
    public abstract void die(DamageSource source);

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void transcend$mark() {
        this.transcend$marked = true;
    }

    @Override
    public boolean transcend$isMarked() {
        return this.transcend$marked;
    }

    @Override
    public void transcend$unmark() {
        this.transcend$marked = false;
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void transcend$forceHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!this.transcend$marked && TranscendDefense.hasReviveInvulnerability(self)) {
            TranscendProtectionEngine.repair(self);
            cir.setReturnValue(false);
            return;
        }
        if (!this.transcend$marked && TranscendProtectionEngine.isProtected(self)) {
            TranscendProtectionEngine.repair(self);
            cir.setReturnValue(false);
            return;
        }
        if (this.transcend$marked) {
            this.invulnerableTime = 0;
            this.setInvulnerable(false);
            self.hurtTime = 10;
            self.hurtDuration = 10;

            self.removeAllEffects();
            self.setAbsorptionAmount(0);

            this.actuallyHurt(source, Float.MAX_VALUE);

            self.setHealth(0.0F);

            try {
                self.die(source);
            } catch (Throwable ignored) {}

            if (self.isAlive()) {
                self.setHealth(0.0F);
                self.remove(net.minecraft.world.entity.Entity.RemovalReason.KILLED);
            }

            self.deathTime = 19;
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "canBeSeenByAnyone", at = @At("HEAD"), cancellable = true)
    private void transcend$hideProtectedPlayer(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (TranscendGuard.isProtected(self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isDamageSourceBlocked", at = @At("HEAD"), cancellable = true)
    private void transcend$bypassShield(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (this.transcend$marked) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void transcend$bypassTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (this.transcend$marked) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void transcend$blockHeal(float amount, CallbackInfo ci) {
        if (this.transcend$marked) {
            ci.cancel();
        }
    }

    @Inject(method = "removeAllEffects", at = @At("RETURN"))
    private void transcend$afterRemoveEffects(CallbackInfoReturnable<Boolean> cir) {
        if (this.transcend$marked) {
            this.invulnerableTime = 0;
        }
    }

    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void transcend$guardSetHealth(float health, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (TranscendGuard.isProtected(self)
                && (!Float.isFinite(health) || health <= 0.001F || health > self.getMaxHealth())) {
            ci.cancel();
        }
    }

    @Inject(method = "getHealth", at = @At("HEAD"), cancellable = true)
    private void transcend$lockGetHealth(CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (TranscendGuard.isProtected(self)) {
            cir.setReturnValue(Math.max(1.0F, self.getMaxHealth()));
            return;
        }
        
        Object forced = TranscendEditService
                .getForcedReturn(self.getStringUUID(), "getHealth", "float");
        if (forced instanceof Number num) {
            cir.setReturnValue(num.floatValue());
        }
    }

    @Inject(method = "isDeadOrDying", at = @At("HEAD"), cancellable = true)
    private void transcend$forceDeadOrDying(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        Object forced = TranscendEditService
                .getForcedReturn(self.getStringUUID(), "isDeadOrDying", "boolean");
        if (forced instanceof Boolean b) {
            cir.setReturnValue(b);
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void transcend$forceHurtResult(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (TranscendGuard.isProtected(self)) return;
        Object forced = TranscendEditService
                .getForcedReturn(self.getStringUUID(), "hurt", "boolean");
        if (forced instanceof Boolean b) {
            cir.setReturnValue(b);
        }
    }

    @Inject(method = "kill", at = @At("HEAD"), cancellable = true)
    private void transcend$guardKill(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (TranscendGuard.isProtected(self) || TranscendDefense.hasReviveInvulnerability(self)) {
            self.setHealth(Math.max(1.0F, self.getMaxHealth()));
            self.deathTime = 0;
            ci.cancel();
        }
    }


    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void transcend$guardDie(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (TranscendGuard.isProtected(self) || TranscendDefense.hasReviveInvulnerability(self)) {
            self.deathTime = 0;
            self.hurtTime = 0;
            ci.cancel();
        }
    }

       
                                                        
                                                                 
                                             
       
    @Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
    private void transcend$guardTickDeath(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!TranscendGuard.isProtected(self)) return;
        self.setHealth(self.getMaxHealth());
        self.deathTime = 0;
        self.hurtTime = 0;
        ci.cancel();
    }

       
                                          
                                          
                                                                                           
       
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void transcend$blockNegativeAddEffect(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (effect == null) return;
        if (effect.getEffect().isBeneficial()) return;
        if (TranscendGuard.isProtected(self)) {
            cir.setReturnValue(false);
        }
    }

       
                                     
                                     
                                                     
       
    @Inject(method = "onEffectAdded", at = @At("HEAD"), cancellable = true)
    private void transcend$blockNegativeOnEffectAdded(MobEffectInstance effect, Entity source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (effect == null || effect.getEffect().isBeneficial()) return;
        if (TranscendGuard.isProtected(self)) {
            ci.cancel();
        }
    }

    @Inject(method = "baseTick", at = @At("TAIL"))
    private void transcend$tickElementMarks(CallbackInfo ci) {
        if (!this.level().isClientSide) {
            LivingEntity self = (LivingEntity) (Object) this;
            if (self.getPersistentData().getBoolean("transcend_tp_lock")) {
                if (self instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator())) {
                    self.getPersistentData().remove("transcend_tp_lock");
                    self.getPersistentData().remove("transcend_tp_lock_time");
                } else {
                    int remaining = self.getPersistentData().getInt("transcend_tp_lock_time") - 1;
                    if (remaining <= 0) {
                        self.getPersistentData().remove("transcend_tp_lock");
                        self.getPersistentData().remove("transcend_tp_lock_time");
                    } else {
                        self.getPersistentData().putInt("transcend_tp_lock_time", remaining);
                    }
                }
            }

            if (self.getPersistentData().contains("transcend_void_prison")) {
                int prisonTime = self.getPersistentData().getInt("transcend_void_prison") - 1;
                if (prisonTime <= 0) {
                    self.getPersistentData().remove("transcend_void_prison");
                    self.getPersistentData().remove("transcend_void_prison_x");
                    self.getPersistentData().remove("transcend_void_prison_z");
                } else {
                    self.getPersistentData().putInt("transcend_void_prison", prisonTime);
                    double px = self.getPersistentData().getDouble("transcend_void_prison_x");
                    double pz = self.getPersistentData().getDouble("transcend_void_prison_z");
                    double dx = px - self.getX();
                    double dz = pz - self.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 0.5) {
                        double pull = 0.08;
                        self.setDeltaMovement(self.getDeltaMovement().add(dx / dist * pull, 0, dz / dist * pull));
                    }
                }
            }
        }
    }
}
