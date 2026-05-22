package com.huige233.transcend.entity;

import com.huige233.transcend.client.renderer.ShaderSpellRenderer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.entity.PathfinderMob;

public class SpellGuardian extends PathfinderMob {

    private static final int LIFETIME = 600;
    private int age = 0;
    private LivingEntity owner;

    public SpellGuardian(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public void setOwner(LivingEntity owner) {
        this.owner = owner;
    }

    public LivingEntity getOwner() {
        return owner;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        age++;
        if (age > LIFETIME) {
            this.discard();
            return;
        }

        if (this.level().isClientSide) {
            float r = 1.0F;
            float g = 0.88F;
            float b = 0.45F;
            Vec3 center = new Vec3(this.getX(), this.getY() + 1.0, this.getZ());
            if ((this.tickCount & 1) == 0) {
                ShaderSpellRenderer.addCircle(center, 0.95F, r, g, b, 12, 20, "pentagram");
            }
            if (this.tickCount % 3 == 0) {
                ShaderSpellRenderer.addShieldRipple(center, 0.72F, r, g, b, 10);
            }
            if (this.tickCount % 4 == 0) {
                double bodyAngle = age * 0.15;
                Vec3 orb = center.add(Math.cos(bodyAngle) * 0.65, 0.15, Math.sin(bodyAngle) * 0.65);
                ShaderSpellRenderer.addSpellEffect(orb, center, 0.95F, 0.75F, 0.25F, 10, "slash");
            }
        }

        if (!this.level().isClientSide && owner != null) {
            if (owner.isDeadOrDying() || !owner.isAlive()) {
                this.discard();
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !this.level().isClientSide && super.hurt(source, amount);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }
}
