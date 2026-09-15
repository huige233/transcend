package com.huige233.transcend.entity.projectile;

import com.huige233.transcend.TranscendDamage;
import com.huige233.transcend.tech.combat.PenetrationProfile;
import com.huige233.transcend.visual.ServerVisualBroadcaster;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

   
                                                                                                  
                                                                                              
   
/** 实现粒子枪弹体的飞行碰撞、穿透与溅射伤害，并同步外观参数和播放命中特效。 */
public class ParticleBolt extends Projectile {
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(ParticleBolt.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE =
            SynchedEntityData.defineId(ParticleBolt.class, EntityDataSerializers.FLOAT);
    private static final int MAX_LIFETIME_TICKS = 60;

    private int life;
    private float damage = 4.0F;
    private int remainingPierce;
    private float splashRadius;
    private boolean explosive;
    private boolean sirius;
    private PenetrationProfile penetration = PenetrationProfile.none();
    
    @Nullable private Set<Integer> hitEntities;

    public ParticleBolt(EntityType<? extends ParticleBolt> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public ParticleBolt(Level level, LivingEntity owner, Vec3 direction, float speed, float damage) {
        this(com.huige233.transcend.init.ModEntities.PARTICLE_BOLT.get(), level);
        setOwner(owner);
        this.damage = Math.max(0.0F, damage);
        setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        setDeltaMovement(direction.normalize().scale(speed));
    }

    public static ParticleBolt shoot(Level level, LivingEntity owner, Vec3 direction, float speed, float damage,
                                     int color, float size, int pierce, float splashRadius, boolean explosive) {
        return shoot(level, owner, direction, speed, damage, color, size, pierce, splashRadius, explosive,
                PenetrationProfile.none());
    }

    public static ParticleBolt shoot(Level level, LivingEntity owner, Vec3 direction, float speed, float damage,
                                     int color, float size, int pierce, float splashRadius, boolean explosive,
                                     PenetrationProfile penetration, boolean sirius) {
        ParticleBolt bolt = new ParticleBolt(level, owner, direction, speed, damage);
        bolt.configure(color, size, pierce, splashRadius, explosive);
        bolt.penetration = penetration == null ? PenetrationProfile.none() : penetration;
        bolt.sirius = sirius && bolt.penetration.hasDirectPercentDamage();
        level.addFreshEntity(bolt);
        return bolt;
    }

    public static ParticleBolt shoot(Level level, LivingEntity owner, Vec3 direction, float speed, float damage,
                                     int color, float size, int pierce, float splashRadius, boolean explosive,
                                     PenetrationProfile penetration) {
        return shoot(level, owner, direction, speed, damage, color, size, pierce, splashRadius,
                explosive, penetration, false);
    }

    public void configure(int color, float size, int pierce, float splashRadius, boolean explosive) {
        entityData.set(DATA_COLOR, color);
        entityData.set(DATA_SIZE, Mth.clamp(size, 0.25F, 3.0F));
        remainingPierce = Mth.clamp(pierce, 0, 16);
        this.splashRadius = Mth.clamp(explosive ? Math.max(2.5F, splashRadius) : splashRadius, 0.0F, 16.0F);
        this.explosive = explosive;
    }

    public int getColor() { return entityData.get(DATA_COLOR); }
    public float getVisualSize() { return entityData.get(DATA_SIZE); }
    public PenetrationProfile penetration() { return penetration; }
    public boolean isSirius() { return sirius; }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_COLOR, 0xFF00E5FF);
        entityData.define(DATA_SIZE, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            onHit(hit);
            if (isRemoved()) return;
        }
        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        setBoundingBox(makeBoundingBox());
        if (level().isClientSide && tickCount % 4 == 0) {
            level().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        }
        if (life++ > MAX_LIFETIME_TICKS || boltInWall()) discard();
    }

    private boolean boltInWall() {
        return !level().noCollision(this, getBoundingBox().inflate(0.0F, -0.05F, 0.0F));
    }

    @Override
    protected boolean canHitEntity(@Nullable Entity target) {
        return target != null && !target.isSpectator() && target.isPickable() && !ownedBy(target)
                && (hitEntities == null || !hitEntities.contains(target.getId()));
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide) return;
        Entity target = result.getEntity();
        DamageSource source = getDamageSource();
        
        float bossBonus = target instanceof LivingEntity living
                ? com.huige233.transcend.tech.combat.BossDefenseRegistry.percentDamage(living, source, penetration)
                : 0.0F;
        boolean hurt = target.hurt(source, com.huige233.transcend.tech.combat.PenetrationPolicy.combineDamage(damage, bossBonus));
        Vec3 hit = result.getLocation();
        if (splashRadius > 0.0F) applySafeSplash(hit, target, source);
        if (explosive && level() instanceof ServerLevel server) {
            showBlast(server, hit);
        } else if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, hit.x, hit.y, hit.z, 8,
                    0.15, 0.15, 0.15, 0.05);
            if (hurt) server.playSound(null, hit.x, hit.y, hit.z, SoundEvents.GENERIC_EXPLODE,
                    SoundSource.PLAYERS, 0.3F, 1.8F);
        }
        if (remainingPierce-- > 0) {
            if (hitEntities == null) hitEntities = new HashSet<>(4);
            hitEntities.add(target.getId());
            Vec3 movement = getDeltaMovement();
            if (movement.lengthSqr() > 0.0D) setPos(hit.add(movement.normalize().scale(0.05F)));
        } else {
            discard();
        }
    }

    private void applySafeSplash(Vec3 center, @Nullable Entity directTarget, DamageSource source) {
        float radius = splashRadius;
        double radiusSquared = radius * radius;
        Entity owner = getOwner();
        for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                entity -> entity != directTarget && entity != owner)) {
            double dx = entity.getX() - center.x;
            double dy = entity.getY() + entity.getBbHeight() * 0.5D - center.y;
            double dz = entity.getZ() - center.z;
            double distanceSquared = dx * dx + dy * dy + dz * dz;
            if (distanceSquared > radiusSquared) continue;
            float scaledDamage = damage * (1.0F - (float) Math.sqrt(distanceSquared) / radius) * 0.5F;
            if (scaledDamage > 0.0F) {
                entity.hurt(source, scaledDamage);
            }
        }
    }

    private void showBlast(ServerLevel server, Vec3 pos) {
        server.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        server.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 0.7F, 1.2F);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level() instanceof ServerLevel server) {
            Vec3 pos = result.getLocation().add(Vec3.atLowerCornerOf(result.getDirection().getNormal()).scale(0.01D));
            if (splashRadius > 0.0F) applySafeSplash(pos, null, getDamageSource());
            if (explosive) showBlast(server, pos);
            ServerVisualBroadcaster.shockwave(server, pos, 0.6F, 0.3F, 0.95F, 1.0F, 6);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 6,
                    0.1, 0.1, 0.1, 0.05);
        }
        discard();
    }

    @Override
    protected void onHit(HitResult result) {
        if (result instanceof EntityHitResult entityHit) onHitEntity(entityHit);
        else if (result instanceof BlockHitResult blockHit) onHitBlock(blockHit);
    }

    private DamageSource getDamageSource() {
        return TranscendDamage.particleBolt(level(), this, getOwner());
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) { return false; }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("BoltDamage", damage);
        tag.putInt("BoltLife", life);
        tag.putInt("BoltColor", getColor());
        tag.putFloat("BoltSize", getVisualSize());
        tag.putByte("BoltPierce", (byte) remainingPierce);
        tag.putFloat("BoltSplash", splashRadius);
        tag.putBoolean("BoltExplosive", explosive);
        tag.putBoolean("BoltSirius", sirius);
        tag.putInt("BoltArmorPenetration", penetration.armorStrength());
        tag.putInt("BoltShieldPenetration", penetration.shieldStrength());
        tag.putInt("BoltBossPenetration", penetration.bossStrength());
        tag.putFloat("BoltHealthPercent", penetration.maxHealthDamagePercent());
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = Math.max(0.0F, tag.getFloat("BoltDamage"));
        life = tag.getInt("BoltLife");
        configure(tag.getInt("BoltColor"), tag.getFloat("BoltSize"), Byte.toUnsignedInt(tag.getByte("BoltPierce")),
                tag.getFloat("BoltSplash"), tag.getBoolean("BoltExplosive"));
        penetration = new PenetrationProfile(tag.getInt("BoltArmorPenetration"),
                tag.getInt("BoltShieldPenetration"), tag.getInt("BoltBossPenetration"),
                tag.getFloat("BoltHealthPercent"));
        sirius = tag.getBoolean("BoltSirius") && penetration.hasDirectPercentDamage();
    }
}
