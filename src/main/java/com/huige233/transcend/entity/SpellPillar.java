package com.huige233.transcend.entity;

import com.huige233.transcend.client.renderer.ShaderSpellRenderer;
import com.huige233.transcend.entity.boss.AbstractTranscendBoss;
import com.huige233.transcend.entity.boss.TranscendenceAvatar;
import com.huige233.transcend.spell.SpellElement;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SpellPillar extends Mob {

    private static final EntityDataAccessor<String> DATA_ELEMENT =
            SynchedEntityData.defineId(SpellPillar.class, EntityDataSerializers.STRING);

    private LivingEntity owner;

    public SpellPillar(EntityType<? extends SpellPillar> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 0.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ELEMENT, "fire");
    }

    public void setRequiredElement(SpellElement element) {
        this.entityData.set(DATA_ELEMENT, element.id);
    }

    public SpellElement getRequiredElement() {
        return SpellElement.getById(this.entityData.get(DATA_ELEMENT));
    }

    public void setOwner(LivingEntity owner) {
        this.owner = owner;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0, 0, 0);

        if (owner != null && !owner.isAlive()) {
            this.discard();
            return;
        }

        if (this.level().isClientSide) {
            SpellElement el = getRequiredElement();
            float r = el.particleR;
            float g = el.particleG;
            float b = el.particleB;
            Vec3 base = new Vec3(this.getX(), this.getY() + 0.08, this.getZ());
            if ((this.tickCount & 1) == 0) {
                float radius = 0.9F + 0.08F * (float) Math.sin(this.tickCount * 0.18F);
                ShaderSpellRenderer.addCircle(base, radius, r, g, b, 10, 18, "hexagram");
            }
            if (this.tickCount % 3 == 0) {
                ShaderSpellRenderer.addSpellEffect(base.add(0.0, 2.2, 0.0), base.add(0.0, 0.35, 0.0), r, g, b, 10, "beam");
            }
            if (this.tickCount % 5 == 0) {
                ShaderSpellRenderer.addShieldRipple(base.add(0.0, 1.4, 0.0), 0.75F, r, g, b, 10);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) return false;

        if (this instanceof com.huige233.transcend.mixinitf.ITranscendMarked m && m.transcend$isMarked()) {
            return super.hurt(source, amount);
        }

        if (source.getDirectEntity() instanceof com.huige233.transcend.spell.SpellProjectile proj) {
            if (proj.getElement() == getRequiredElement()) {
                return super.hurt(source, amount);
            }
        }

        return false;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel sl) {
            SpellElement el = getRequiredElement();
            Vec3 center = new Vec3(this.getX(), this.getY() + 1.5, this.getZ());
            ShaderSpellRenderer.addShockwave(center, 2.8F, el.particleR, el.particleG, el.particleB, 18);
            ShaderSpellRenderer.addCircle(center, 2.1F, el.particleR, el.particleG, el.particleB, 14, 24, "hexagram");
            ShaderSpellRenderer.addSpellEffect(center.add(0.0, 2.6, 0.0), center, el.particleR, el.particleG, el.particleB, 12, "beam");
            sl.playSound(null, this.blockPosition(),
                    net.minecraft.sounds.SoundEvents.GLASS_BREAK,
                    net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.6F);
        }
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean canBeCollidedWith() { return true; }

    @Override
    public boolean isPickable() { return true; }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }
}
