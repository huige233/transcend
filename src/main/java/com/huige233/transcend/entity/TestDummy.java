package com.huige233.transcend.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

public class TestDummy extends Mob {

    private static final EntityDataAccessor<Float> DATA_LAST_DAMAGE =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TOTAL_DAMAGE =
            SynchedEntityData.defineId(TestDummy.class, EntityDataSerializers.FLOAT);

    private int damageDisplayTimer = 0;
    private float dpsAccum = 0;
    private int dpsTicks = 0;
    private float burstAccum = 0;
    private int burstTimer = 0;
    private boolean announceHits = true;
    private int resistanceLevel = 0;
    private static final int BURST_WINDOW = 100;

    public TestDummy(EntityType<? extends TestDummy> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000000.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_LAST_DAMAGE, 0.0F);
        this.entityData.define(DATA_TOTAL_DAMAGE, 0.0F);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0, 0, 0);
        this.setNoGravity(true);

        if (damageDisplayTimer > 0) {
            damageDisplayTimer--;
        }
        if (burstTimer > 0) {
            burstTimer--;
            if (burstTimer <= 0) burstAccum = 0;
        }

        dpsTicks++;
        if (dpsTicks >= 20) {
            float dps = dpsAccum;
            dpsAccum = 0;
            dpsTicks = 0;
            if (!this.level().isClientSide) {
                String burstStr = burstTimer > 0
                        ? String.format(" | §d%.0f §7/ 5s", burstAccum)
                        : "";
                this.setCustomName(Component.literal(
                        String.format("§c%.1f §7dmg | §e%.1f §7DPS%s",
                                this.entityData.get(DATA_LAST_DAMAGE), dps, burstStr)));
                this.setCustomNameVisible(true);
            }
        }

        if (!this.level().isClientSide && this.getHealth() < this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }

        if (this.level().isClientSide && damageDisplayTimer > 0) {
            float lastDmg = this.entityData.get(DATA_LAST_DAMAGE);
            float intensity = Math.min(1.0F, lastDmg / 50.0F);
            DustParticleOptions dust = new DustParticleOptions(
                    new Vector3f(1.0F, 0.2F + (1 - intensity) * 0.8F, 0.2F),
                    1.5F + intensity * 1.5F);
            for (int i = 0; i < 3; i++) {
                this.level().addParticle(dust,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.8,
                        this.getY() + 0.2 + this.random.nextDouble() * 0.6,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.8,
                        (this.random.nextDouble() - 0.5) * 0.05, 0.01, (this.random.nextDouble() - 0.5) * 0.05);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) return false;

        // 超越剑标记：记录伤害后允许真正死亡
        if (this instanceof com.huige233.transcend.mixinitf.ITranscendMarked m && m.transcend$isMarked()) {
            String displayAmount = amount >= Float.MAX_VALUE / 2 ? "∞" : String.format("%.2f", amount);
            this.entityData.set(DATA_LAST_DAMAGE, amount >= Float.MAX_VALUE / 2 ? Float.MAX_VALUE : amount);
            this.entityData.set(DATA_TOTAL_DAMAGE, this.entityData.get(DATA_TOTAL_DAMAGE) + amount);
            if (source.getEntity() instanceof Player player) {
                player.displayClientMessage(Component.literal(
                        String.format("§4[KILL] §c%s §7(§e%s§7) | Total: §c%.0f",
                                displayAmount, source.getMsgId(), this.entityData.get(DATA_TOTAL_DAMAGE))), true);
            }
            this.setCustomName(Component.literal(String.format("§4KILLED §7| §c%s §7dmg", displayAmount)));
            this.setCustomNameVisible(true);
            return super.hurt(source, amount);
        }

        boolean isDot = source.getMsgId().contains("inFire") || source.getMsgId().contains("onFire")
                || source.getMsgId().contains("lava") || source.getMsgId().contains("wither")
                || source.getMsgId().contains("poison") || source.getMsgId().contains("freeze");

        // DOT伤害只计入统计，不覆盖单次伤害显示
        if (!isDot) {
            this.entityData.set(DATA_LAST_DAMAGE, amount);
            damageDisplayTimer = 40;
        }
        this.entityData.set(DATA_TOTAL_DAMAGE, this.entityData.get(DATA_TOTAL_DAMAGE) + amount);
        dpsAccum += amount;
        burstAccum += amount;
        burstTimer = BURST_WINDOW;

        if (announceHits && source.getEntity() instanceof Player player) {
            String prefix = isDot ? "§6[DOT] " : "§6[Dummy] ";
            player.displayClientMessage(Component.literal(
                    String.format("%s§c%.2f §7(§e%s§7) | §d5s: %.0f §7| Total: §c%.0f",
                            prefix, amount, source.getMsgId(), burstAccum, this.entityData.get(DATA_TOTAL_DAMAGE))), true);
        }

        if (this.level() instanceof ServerLevel sl) {
            float intensity = Math.min(1.0F, amount / 50.0F);
            DustParticleOptions dust = new DustParticleOptions(
                    new Vector3f(1.0F, 0.3F, 0.3F), 2.0F + intensity * 2.0F);
            sl.sendParticles(dust, this.getX(), this.getY() + 0.8, this.getZ(),
                    5 + (int)(intensity * 10), 0.3, 0.5, 0.3, 0.1);
        }

        this.setHealth(this.getMaxHealth());
        return true;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND) {
            if (player.level().isClientSide) {
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        new com.huige233.transcend.client.TestDummyScreen(this));
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    public void resetData() {
        this.entityData.set(DATA_LAST_DAMAGE, 0.0F);
        this.entityData.set(DATA_TOTAL_DAMAGE, 0.0F);
        this.dpsAccum = 0;
        this.dpsTicks = 0;
        this.burstAccum = 0;
        this.burstTimer = 0;
        this.setCustomName(Component.literal("§7Data cleared"));
    }

    public void toggleAnnounce() {
        this.announceHits = !this.announceHits;
    }

    public boolean isAnnounceHits() {
        return this.announceHits;
    }

    public void setResistanceLevel(int level) {
        this.resistanceLevel = level;
        if (level > 0) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, level - 1, false, false));
        } else {
            this.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        }
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean isPickable() { return true; }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public boolean canBeCollidedWith() { return true; }

    @Override
    protected boolean shouldDespawnInPeaceful() { return false; }

    @Override
    public double getMyRidingOffset() { return 0; }

    @Override
    protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions size) {
        return 2.5F;
    }

    @Override
    public void teleportTo(double x, double y, double z) {
    }
}
