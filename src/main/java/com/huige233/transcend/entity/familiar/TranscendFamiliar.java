package com.huige233.transcend.entity.familiar;

import com.huige233.transcend.balance.BalanceConfig;
import com.huige233.transcend.client.magic.MagicCrystalHelper;
import com.huige233.transcend.init.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.UUID;

/**
 * Round 20: Familiar 体系 — 召唤式持续魔法助手。
 *
 * <p>4 aspect 各 1 个 familiar，对应 4 种 TypedManaCrystal：
 * <ul>
 *   <li>{@link FamiliarType#AETHER_WISP} — 自动捡 6 格内掉落物送回主人</li>
 *   <li>{@link FamiliarType#BLOOD_HOUND} — 攻击 12 格内最近敌怪</li>
 *   <li>{@link FamiliarType#COSMIC_OWL} — 夜间给主人 NIGHT_VISION + SLOW_FALLING</li>
 *   <li>{@link FamiliarType#TAINTED_IMP} — 抽 8 格内敌怪 HP 转为主人 mana</li>
 * </ul>
 *
 * <p>绑定机制：通过 UUID 关联 owner。owner 离线 → familiar 暂停活动并停留。
 * 60 秒未见主人 → despawn。
 */
public class TranscendFamiliar extends PathfinderMob {

    public enum FamiliarType {
        AETHER_WISP(0.85F, 0.95F, 0.6F),    // 金光
        BLOOD_HOUND(0.85F, 0.10F, 0.10F),   // 鲜红
        COSMIC_OWL(0.45F, 0.50F, 0.95F),    // 星蓝
        TAINTED_IMP(0.50F, 0.10F, 0.55F);   // 暗紫

        public final float r, g, b;

        FamiliarType(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    private static final EntityDataAccessor<Integer> DATA_TYPE =
            SynchedEntityData.defineId(TranscendFamiliar.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(TranscendFamiliar.class,
                    EntityDataSerializers.OPTIONAL_UUID);

    private int despawnTimer = 0;
    private int behaviorCooldown = 0;

    public TranscendFamiliar(EntityType<? extends TranscendFamiliar> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        // Round 36: BalanceConfig defaults (early static read — JSON reload won't reapply)
        BalanceConfig.FamiliarBalance f = BalanceConfig.get().familiar;
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, f.max_health)
                .add(Attributes.MOVEMENT_SPEED, f.movement_speed)
                .add(Attributes.ATTACK_DAMAGE, f.attack_damage)
                .add(Attributes.FOLLOW_RANGE, f.follow_range);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_TYPE, 0);
        this.entityData.define(DATA_OWNER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("familiar_type", this.entityData.get(DATA_TYPE));
        this.entityData.get(DATA_OWNER).ifPresent(u -> tag.putUUID("owner_uuid", u));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_TYPE, tag.getInt("familiar_type"));
        if (tag.hasUUID("owner_uuid")) {
            this.entityData.set(DATA_OWNER, Optional.of(tag.getUUID("owner_uuid")));
        }
    }

    public void setFamiliarType(FamiliarType type) {
        this.entityData.set(DATA_TYPE, type.ordinal());
    }

    public FamiliarType getFamiliarType() {
        int ord = this.entityData.get(DATA_TYPE);
        FamiliarType[] all = FamiliarType.values();
        return all[Math.min(ord, all.length - 1)];
    }

    public void setOwner(Player player) {
        this.entityData.set(DATA_OWNER, Optional.of(player.getUUID()));
    }

    public java.util.Optional<UUID> getOwnerUUID() {
        return this.entityData.get(DATA_OWNER);
    }

    public Player findOwner() {
        return this.entityData.get(DATA_OWNER)
                .map(uuid -> this.level().getPlayerByUUID(uuid))
                .orElse(null);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            spawnTypeParticles();
            return;
        }

        Player owner = findOwner();
        if (owner == null) {
            despawnTimer++;
            if (despawnTimer >= BalanceConfig.get().familiar.despawn_timer) {
                this.discard();
            }
            return;
        }
        despawnTimer = 0;

        // 跟随 owner
        double distSq = this.distanceToSqr(owner);
        if (distSq > 256.0) { // 16 blocks
            // 远离 → 传送回 owner
            this.teleportTo(owner.getX() + (this.random.nextDouble() - 0.5) * 3,
                    owner.getY() + 0.5,
                    owner.getZ() + (this.random.nextDouble() - 0.5) * 3);
        } else if (distSq > 16.0) { // 4 blocks
            Vec3 toOwner = new Vec3(
                    owner.getX() - this.getX(), 0, owner.getZ() - this.getZ()).normalize();
            this.getNavigation().moveTo(owner.getX(), owner.getY(), owner.getZ(), 1.0);
        }

        // 行为节流（每 behavior_interval tick 触发一次）
        behaviorCooldown--;
        if (behaviorCooldown <= 0) {
            behaviorCooldown = BalanceConfig.get().familiar.behavior_interval;
            executeBehavior(owner);
        }
    }

    /** 行为派发 — 4 type × 各自逻辑 */
    private void executeBehavior(Player owner) {
        if (!(this.level() instanceof ServerLevel sl)) return;

        switch (getFamiliarType()) {
            case AETHER_WISP -> behaviorAetherWisp(sl, owner);
            case BLOOD_HOUND -> behaviorBloodHound(sl, owner);
            case COSMIC_OWL -> behaviorCosmicOwl(sl, owner);
            case TAINTED_IMP -> behaviorTaintedImp(sl, owner);
        }
    }

    /** AETHER_WISP: 捡掉落物 → 直接放入 owner 背包（radius 由 balance 控制） */
    private void behaviorAetherWisp(ServerLevel sl, Player owner) {
        AABB search = this.getBoundingBox().inflate(BalanceConfig.get().familiar.aether_pickup_radius);
        var items = sl.getEntitiesOfClass(ItemEntity.class, search,
                ie -> ie.isAlive() && !ie.hasPickUpDelay());
        for (ItemEntity ie : items) {
            ItemStack stack = ie.getItem();
            if (owner.getInventory().add(stack.copy())) {
                ie.discard();
                sl.playSound(null, this.blockPosition(),
                        net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                        net.minecraft.sounds.SoundSource.NEUTRAL, 0.4F, 1.8F);
                break; // 一次只捡 1 个
            }
        }
    }

    /** BLOOD_HOUND: 最近敌怪 → 攻击（radius/damage 由 balance 控制） */
    private void behaviorBloodHound(ServerLevel sl, Player owner) {
        BalanceConfig.FamiliarBalance f = BalanceConfig.get().familiar;
        AABB search = this.getBoundingBox().inflate(f.blood_search_radius);
        var enemies = sl.getEntitiesOfClass(LivingEntity.class, search,
                e -> e != owner && e != this && (e instanceof Enemy ||
                        (e instanceof Mob m && !m.isAlliedTo(owner))) && e.isAlive());
        if (enemies.isEmpty()) return;
        LivingEntity target = enemies.stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(this), b.distanceToSqr(this)))
                .orElse(null);
        if (target == null) return;
        // 接近 + 攻击
        if (this.distanceToSqr(target) < 4.0) {
            target.hurt(this.damageSources().mobAttack(this), f.blood_attack_damage);
            sl.playSound(null, target.blockPosition(),
                    net.minecraft.sounds.SoundEvents.WOLF_GROWL,
                    net.minecraft.sounds.SoundSource.HOSTILE, 0.8F, 1.5F);
        } else {
            this.getNavigation().moveTo(target, 1.2);
        }
    }

    /** COSMIC_OWL: 夜间给 owner NIGHT_VISION + SLOW_FALLING（duration 由 balance 控制） */
    private void behaviorCosmicOwl(ServerLevel sl, Player owner) {
        BalanceConfig.FamiliarBalance f = BalanceConfig.get().familiar;
        long time = sl.getDayTime() % 24000;
        boolean isNight = time > 13000 && time < 23000;
        if (isNight || sl.dimensionType().hasFixedTime()) {
            owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.NIGHT_VISION, f.cosmic_night_vision_duration, 0, true, false));
        }
        // 跌落保护（owner 下落速度 < -0.5 时给 SLOW_FALLING）
        if (owner.getDeltaMovement().y < -0.5) {
            owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.SLOW_FALLING, f.cosmic_slow_falling_duration, 0, true, false));
        }
    }

    /** TAINTED_IMP: 敌怪每 sec 损失 HP, owner 得 mana（radius/damage/gain 由 balance 控制） */
    private void behaviorTaintedImp(ServerLevel sl, Player owner) {
        BalanceConfig.FamiliarBalance f = BalanceConfig.get().familiar;
        AABB search = this.getBoundingBox().inflate(f.tainted_search_radius);
        var enemies = sl.getEntitiesOfClass(LivingEntity.class, search,
                e -> e != owner && e != this && (e instanceof Enemy ||
                        (e instanceof Mob m && !m.isAlliedTo(owner))) && e.isAlive());
        for (LivingEntity e : enemies) {
            if (e.getHealth() <= 1.5F) continue; // 留 1 HP 不杀死
            e.hurt(this.damageSources().magic(), f.tainted_damage);
            // 给 owner +N innate mana
            int current = MagicCrystalHelper.getInnateMana(owner);
            int max = MagicCrystalHelper.getInnateMaxMana(owner);
            MagicCrystalHelper.setInnateMana(owner, Math.min(max, current + f.tainted_mana_gain));
        }
    }

    /** 客户端粒子：每帧生成 type 色 dust */
    private void spawnTypeParticles() {
        FamiliarType type = getFamiliarType();
        if (this.random.nextFloat() < 0.7F) {
            this.level().addParticle(
                    new DustParticleOptions(new Vector3f(type.r, type.g, type.b), 1.0F),
                    this.getX() + (this.random.nextDouble() - 0.5) * 0.5,
                    this.getY() + 0.3 + this.random.nextDouble() * 0.5,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.5,
                    0.0, 0.05, 0.0);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // 不允许玩家直接攻击 familiar
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (source.getEntity() instanceof Player p &&
                this.getOwnerUUID().map(u -> u.equals(p.getUUID())).orElse(false)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    // helper —— 直接用 java.util.Optional, 删除内部别名类
}
