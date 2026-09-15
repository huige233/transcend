package com.mega.uom.mixin;

import com.mega.endinglib.util.mc.entity.armor.ArmorUtils;
import com.mega.uom.ModSource;
import com.mega.uom.auto.AutoRegisterManager;
import com.mega.uom.auto.SpellAutoRegisterHandler;
import com.mega.uom.common.damagesource.ModDamageSources;
import com.mega.uom.common.entity.boss.uom.UomWither;
import com.mega.uom.common.network.PacketHandler;
import com.mega.uom.common.network.s2c.render.BeamBottomRendererPacket;
import com.mega.uom.common.network.s2c.sound.EldritchBlastSoundPacket;
import com.mega.uom.common.spells.fantasy.attack.MultipleEldritchBlastSpell;
import com.mega.uom.util.data.LivingEntityExpandedContext;
import com.mega.uom.util.entity.*;
import com.mega.uom.util.itf.LivingEntityEC;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.eldritch_blast.EldritchBlastVisualEntity;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;

@Mixin(value = LivingEntity.class, priority = 1133)
public abstract class LivingEntityMixin extends Entity implements LivingEntityEC {
    @Unique
    private LivingEntityExpandedContext uom$ecData;
    public LivingEntityMixin(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
    }
    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(EntityType<? extends LivingEntity> p_20966_, Level p_20967_, CallbackInfo ci) {
        /*
        try {
            LivingEntityExpandedContext entityExpandedContent = new LivingEntityExpandedContext((LivingEntity) (Object) this);
            this.uom$setECData(entityExpandedContent);
        } catch (Throwable e) {
            ModSource.out("CreateECData ERROR:%S", e);
            e.printStackTrace();
            e.printStackTrace(FantasyEndingCore.stream);
            System.exit(-1);
        }
         */
    }
    @Override
    public LivingEntityExpandedContext uom$livingECData() {
        if (uom$ecData == null) this.uom$setECData(new LivingEntityExpandedContext((LivingEntity)(Object) this));
        return uom$ecData;
    }

    @Override
    public void uom$setECData(LivingEntityExpandedContext data) {
        this.uom$ecData = data;
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void clinit(CallbackInfo ci) {
        EntityASMUtil.FE_GET_HEALTH_DATA = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
        EntityDataInjector.MEB_LEVEL = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
        EntityDataInjector.MEB_TIME = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
        EntityDataInjector.MEB_SOURCE = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.OPTIONAL_UUID);
        EntityDataInjector.MEB_DAMAGE = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
        EntityDataInjector.FE_ARMORED_PLAYER_EVASION_TIME = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
    }

    @Shadow
    public abstract float getHealth();

    @Shadow
    public abstract boolean isDeadOrDying();

    @Shadow
    public abstract float getMaxHealth();

    @Shadow
    public abstract boolean addEffect(MobEffectInstance p_21165_);

    @Shadow
    public abstract RandomSource getRandom();

    @Shadow
    public abstract void die(DamageSource p_21014_);

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        LivingEntity o = (LivingEntity) (Object) this;
        EntityASMUtil.setHealthDelta(o, tag.getInt(EntityASMUtil.FE_GET_HEALTH));
        EntityDataInjector.setMebLevel(o, tag.getInt(EntityDataInjector.MEB_LEVEL_NAME));
        EntityDataInjector.setMebTime(o, tag.getInt(EntityDataInjector.MEB_TIME_NAME));
        EntityDataInjector.setMebDamage(o, tag.getInt(EntityDataInjector.MEB_DAMAGE_NAME));
        try {
            if (tag.contains(EntityDataInjector.MEB_SOURCE_NAME))
                EntityDataInjector.setMebSource(o, Optional.of(tag.getUUID(EntityDataInjector.MEB_SOURCE_NAME)));
        } catch (Throwable throwable) {
            EntityDataInjector.setMebSource(o, Optional.empty());
        }
        EntityDataInjector.setEvasionTime(o, tag.getInt(EntityDataInjector.FEAP_EVASION_TIME));
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void addAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        LivingEntity o = (LivingEntity) (Object) this;
        tag.putFloat(EntityASMUtil.FE_GET_HEALTH, EntityASMUtil.getHealthDelta(o));
        tag.putInt(EntityDataInjector.MEB_LEVEL_NAME, EntityDataInjector.getMebLevel(o));
        tag.putUUID(EntityDataInjector.MEB_SOURCE_NAME, EntityDataInjector.getMebSource(o).orElseGet(UUID::randomUUID));
        tag.putInt(EntityDataInjector.MEB_TIME_NAME, EntityDataInjector.getMebTime(o));
        tag.putFloat(EntityDataInjector.MEB_DAMAGE_NAME, EntityDataInjector.getMebDamage(o));
        tag.putInt(EntityDataInjector.FEAP_EVASION_TIME, EntityDataInjector.getEvasionTime(o));
    }

    @Inject(method = "defineSynchedData", at = @At("HEAD"))
    private void defineSynchedData(CallbackInfo ci) {
        this.entityData.define(EntityASMUtil.FE_GET_HEALTH_DATA, 0F);
        this.entityData.define(EntityDataInjector.MEB_LEVEL, -1);
        this.entityData.define(EntityDataInjector.MEB_SOURCE, Optional.empty());
        this.entityData.define(EntityDataInjector.MEB_TIME, -1);
        this.entityData.define(EntityDataInjector.MEB_DAMAGE, 0F);
        this.entityData.define(EntityDataInjector.FE_ARMORED_PLAYER_EVASION_TIME, -1);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        ProfilerFiller filler = level().getProfiler();
        filler.push(ModSource.MODID+"_custom_tickData");
        try {
            LivingEntity living = (LivingEntity) (Object) this;
            LivingEntityExpandedContext entityEC = uom$livingECData();
            entityEC.setCurrentArmorSet(ArmorUtils.getArmorSet(living));
            if (entityEC.invulnerableTime > 0)
                entityEC.invulnerableTime--;
            if (EntityDataInjector.getEvasionTime(living) > 0)
                EntityDataInjector.setEvasionTime(living, EntityDataInjector.getEvasionTime(living) - 1);
            int mebTime = EntityDataInjector.getMebTime(living);
            if (!level().isClientSide) {
                if (this.tickCount % 100 == 0 && !isDeadOrDying()) {
                    LivingEntity livingEntity = (LivingEntity) (Object) this;
                    float delta = EntityASMUtil.getHealthDelta(livingEntity);
                    if (delta <= -1)
                        EntityASMUtil.addDelta(livingEntity, 1.0F);
                    else if (delta >= 1)
                        EntityASMUtil.addDelta(livingEntity, -1.0F);
                    else if (delta < 1)
                        EntityASMUtil.setHealthDelta(livingEntity, 0F);
                }

                if (mebTime > 1)
                    EntityDataInjector.setMebTime(living, EntityDataInjector.getMebTime(living) - 1);
                if (mebTime > 1 && mebTime % 2 == 0) {
                    Optional<UUID> optionalUUID = EntityDataInjector.getMebSource(living);
                    int spellLevel = EntityDataInjector.getMebLevel(living);
                    float damage = EntityDataInjector.getMebDamage(living);
                    if (optionalUUID.isPresent() && ((ServerLevel) level()).getEntity(optionalUUID.get()) instanceof Player player) {
                        if (spellLevel > 0 && !isDeadOrDying()) {
                            EntityActuallyHurt actuallyHurt = new EntityActuallyHurt(living);
                            Vec3 pos = new Vec3(this.getRandomX(18),
                                    this.getY() + living.getRandom().triangle(4F, 3F),
                                    this.getRandomZ(18));
                            Vec3 delta = position().subtract(pos);
                            double[] doubles = RotationUtil.getRadiansFromMovement(delta.x, delta.y, delta.z);
                            UomWither.drawDreamShadowBeam(this, pos, getRandom());
                            PacketHandler.sendToAll(new BeamBottomRendererPacket(pos.x, pos.y, pos.z, (float) doubles[0] / 0.017F, (float) doubles[1] / 0.017F, 100));
                            uom$eldSpell(actuallyHurt, player, pos, (float) doubles[0] / 0.017F, (float) doubles[1] / 0.017F, damage);
                            PacketHandler.sendToAll(new EldritchBlastSoundPacket(player.getId()));
                        }
                    }
                }
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        filler.pop();
    }

    @Unique
    /**
     * @author iron's spell book
     */
    protected void uom$eldSpell(EntityActuallyHurt entityActuallyHurt, Player mob, Vec3 sourcePos, float xRot, float yRot, float damage) {
        {
            if (this.isDeadOrDying())
                return;
            Level level = mob.level();
            EldritchBlastVisualEntity entity = new EldritchBlastVisualEntity(level, sourcePos, position(), mob);
            entity.setXRot(xRot);
            entity.setYRot(yRot);
            level.addFreshEntity(entity);
            DamageSources.applyDamage(this, 3 + damage, AutoRegisterManager.INSTANCE().spell.get(MultipleEldritchBlastSpell.class).getDamageSource(mob));
            entityActuallyHurt.actuallyHurt(ModDamageSources.causeDeathDsDamage(mob), damage);
            float delta = -damage * 0.3F - 1;
            if (mob.isDeadOrDying() && delta < 0){
                entityActuallyHurt.actuallyHurtForDelta(ModDamageSources.causeDeathDsDamage(mob), -delta, false);
            }
            this.addEffect(new MobEffectInstance(MobEffects.WITHER, (int) damage * 10, 3));
            MagicManager.spawnParticles(
                    level,
                    ParticleHelper.UNSTABLE_ENDER,
                    this.position().x,
                    this.position().y,
                    this.position().z,
                    50,
                    0.0,
                    0.0,
                    0.0,
                    0.3,
                    false
            );
        }
    }

}
