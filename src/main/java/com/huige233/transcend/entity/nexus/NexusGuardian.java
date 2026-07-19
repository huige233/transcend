package com.huige233.transcend.entity.nexus;

import com.huige233.transcend.visual.ServerVisualBroadcaster;
import com.huige233.transcend.world.TranscendDimensions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class NexusGuardian extends PathfinderMob {

    private String nexusId = "";

    public NexusGuardian(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public void setNexusId(String id) {
        this.nexusId = id;
    }

    public String getNexusId() {
        return nexusId;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(NexusGuardian.class, NexusSentinel.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide && this.level().dimension() != TranscendDimensions.NEXUS_LEVEL) {
            this.discard();
            return;
        }

        if (this.level() instanceof ServerLevel sl) {
            Vec3 center = new Vec3(this.getX(), this.getY() + 1.0, this.getZ());
            if ((this.tickCount & 1) == 0) {
                ServerVisualBroadcaster.circle(sl, center, 0.85F, 0.6F, 0.2F, 0.8F, 12, 20, "hexagram");
            }
            if (this.tickCount % 4 == 0) {
                ServerVisualBroadcaster.shieldRipple(sl, center, 0.65F, 0.5F, 0.1F, 0.7F, 10);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) return false;
        return super.hurt(source, amount);
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("NexusId", nexusId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        nexusId = tag.getString("NexusId");
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }
}
