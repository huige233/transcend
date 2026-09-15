package com.mega.uom.mixin;

import com.mega.uom.event.entity.AttackEntityEvent;
import com.mega.uom.util.entity.EntityASMUtil;
import com.mega.uom.util.entity.PlayerInvulnerableEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {
    @Shadow
    @Final
    private ItemCooldowns cooldowns;

    protected PlayerMixin(EntityType<? extends LivingEntity> p_20966_, Level p_20967_) {
        super(p_20966_, p_20967_);
    }

    @SuppressWarnings("WrongEntityDataParameterClass")
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void clinit(CallbackInfo ci) {
        PlayerInvulnerableEntityData.FE_INVULNERABLE_DATA = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BOOLEAN);
    }

    @Shadow
    public abstract boolean hurt(DamageSource p_36154_, float p_36155_);

    @Shadow
    public abstract ItemCooldowns getCooldowns();

    @Shadow
    public abstract void playNotifySound(SoundEvent p_36140_, SoundSource p_36141_, float p_36142_, float p_36143_);

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void attack_head(Entity entity, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new AttackEntityEvent.Pre((Player) (Object) this, entity)))
            ci.cancel();
    }

    @Inject(method = "attack", at = @At("TAIL"))
    private void attack_tail(Entity entity, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new AttackEntityEvent.Post((Player) (Object) this, entity));
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readAdditionalSaveData(CompoundTag compoundTag, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        PlayerInvulnerableEntityData.setInvul(player, compoundTag.getBoolean(PlayerInvulnerableEntityData.FE_INVULNERABLE));
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void addAdditionalSaveData(CompoundTag p_21145_, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        p_21145_.putBoolean(PlayerInvulnerableEntityData.FE_INVULNERABLE, PlayerInvulnerableEntityData.isInvul(player));
    }

    @Inject(method = "defineSynchedData", at = @At("HEAD"))
    private void defineSynchedData(CallbackInfo ci) {
        this.entityData.define(PlayerInvulnerableEntityData.FE_INVULNERABLE_DATA, false);
    }

    @Override
    public float getHealth() {
        if (PlayerInvulnerableEntityData.isInvul((Player) (Object) this)) {
            float health = Math.max(1F, getMaxHealth());
            deathTime = hurtTime = 0;
            return health;
        }
        return super.getHealth();
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void hurt(DamageSource p_36154_, float p_36155_, CallbackInfoReturnable<Boolean> cir) {
        if (PlayerInvulnerableEntityData.isInvul((Player) (Object) this)) cir.setReturnValue(false);
    }
}
