package com.huige233.transcend.entity;

import com.huige233.transcend.client.renderer.ShaderSpellRenderer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.entity.PathfinderMob;

public class SpellWisp extends PathfinderMob {

    private static final int LIFETIME = 400;
    private int age = 0;
    private LivingEntity owner;

    public SpellWisp(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public void setOwner(LivingEntity owner) {
        this.owner = owner;
    }

    public LivingEntity getOwner() {
        return owner;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.FLYING_SPEED, 0.4);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 1.0));
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
            float t = (float) age / LIFETIME;
            float r = 0.4F + 0.6F * t;
            float g = 0.8F - 0.6F * t;
            float b = 1.0F;
            Vec3 center = new Vec3(this.getX(), this.getY() + 0.35, this.getZ());
            if ((this.tickCount & 1) == 0) {
                float radius = 0.55F + 0.15F * (1.0F - t);
                ShaderSpellRenderer.addCircle(center, radius, r, g, b, 10, 16, "hexagram");
            }
            if (this.tickCount % 3 == 0) {
                Vec3 sparkFrom = center.add(
                        (this.random.nextDouble() - 0.5) * 0.7,
                        0.2 + this.random.nextDouble() * 0.35,
                        (this.random.nextDouble() - 0.5) * 0.7);
                ShaderSpellRenderer.addSpellEffect(sparkFrom, center, r, g, b, 8, t < 0.5F ? "beam" : "slash");
            }
            if (this.tickCount % 6 == 0) {
                ShaderSpellRenderer.addShieldRipple(center, 0.42F, 0.9F, 0.95F, 1.0F, 8);
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
