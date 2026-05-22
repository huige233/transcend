package com.huige233.transcend.entity.nexus;

import com.huige233.transcend.client.renderer.ShaderSpellRenderer;
import com.huige233.transcend.world.TranscendDimensions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 法则哨兵 — 枢纽的飞行护卫，小型浮空晶体生物。
 * 飞行、射击远程攻击、永久存在。
 * 以飞行的形态巡逻在枢纽上空，玩家接近时发动攻击。
 */
public class NexusSentinel extends PathfinderMob {

    private String nexusId = "";

    public NexusSentinel(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setNoGravity(true);
    }

    public void setNexusId(String id) {
        this.nexusId = id;
    }

    public String getNexusId() {
        return nexusId;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.4)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomFlyingGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(NexusGuardian.class, NexusSentinel.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        // Only exist in nexus dimension
        if (!this.level().isClientSide && this.level().dimension() != TranscendDimensions.NEXUS_LEVEL) {
            this.discard();
            return;
        }

        // Client-side shader visuals — cyan rotating diamond + orbiting sparks
        if (this.level().isClientSide) {
            Vec3 center = new Vec3(this.getX(), this.getY() + 0.5, this.getZ());
            if ((this.tickCount & 1) == 0) {
                ShaderSpellRenderer.addCircle(center, 0.5F, 0.3F, 0.8F, 1.0F, 8, 16, "diamond");
            }
            if (this.tickCount % 3 == 0) {
                double angle = this.tickCount * 0.2;
                Vec3 orb = center.add(Math.cos(angle) * 0.8, 0.2, Math.sin(angle) * 0.8);
                ShaderSpellRenderer.addSpellEffect(orb, center, 0.4F, 0.9F, 1.0F, 8, "spark");
            }
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false; // Flying entity — no fall damage
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
        return false; // Never despawn
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }
}
