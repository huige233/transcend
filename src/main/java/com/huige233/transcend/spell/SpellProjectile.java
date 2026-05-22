package com.huige233.transcend.spell;

import com.huige233.transcend.entity.RainbowLightning;
import com.huige233.transcend.client.renderer.ShaderSpellRenderer;
import com.huige233.transcend.init.ModEntities;
import com.huige233.transcend.util.EntityCompatUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class SpellProjectile extends ThrowableProjectile {

    private static final UUID ACID_ARMOR_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID ARMOR_BREAK_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");
    private static final UUID DEVOUR_HEALTH_UUID = UUID.fromString("c3d4e5f6-a7b8-9012-cdef-234567890123");

    private static final EntityDataAccessor<String> DATA_CARRIER =
            SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_ELEMENT =
            SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_EFFECT =
            SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_BASE_POWER =
            SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.FLOAT);

    private boolean isSplit = false;
    private int bounceCount = 0;
    private int age = 0;

    // Round 46: Augment glyph stacks (server-side, NBT persisted, not synced — affect hit logic only)
    private int augPierceStacks = 0;
    private int augChainStacks = 0;
    private int augExtendStacks = 0;
    private int augHomingStacks = 0;

    // Default constructor for entity type registration
    public SpellProjectile(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    // Full constructor for casting
    public SpellProjectile(Level level, LivingEntity owner, SpellCarrier carrier,
                           SpellElement element, SpellEffect effect, float basePower) {
        super(ModEntities.SPELL_PROJECTILE.get(), level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.entityData.set(DATA_CARRIER, carrier.id);
        this.entityData.set(DATA_ELEMENT, element.id);
        this.entityData.set(DATA_EFFECT, effect != null ? effect.id : "");
        this.entityData.set(DATA_BASE_POWER, basePower);
    }

    // Constructor for split projectiles
    public SpellProjectile(Level level, double x, double y, double z,
                           SpellCarrier carrier, SpellElement element, float basePower,
                           Entity owner) {
        super(ModEntities.SPELL_PROJECTILE.get(), level);
        this.setOwner(owner);
        this.setPos(x, y, z);
        this.entityData.set(DATA_CARRIER, carrier.id);
        this.entityData.set(DATA_ELEMENT, element.id);
        this.entityData.set(DATA_EFFECT, "");
        this.entityData.set(DATA_BASE_POWER, basePower);
        this.isSplit = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_CARRIER, "orb");
        this.entityData.define(DATA_ELEMENT, "fire");
        this.entityData.define(DATA_EFFECT, "");
        this.entityData.define(DATA_BASE_POWER, 1.0F);
    }

    // ═══════════════════════════════════════════
    //  NBT persistence
    // ═══════════════════════════════════════════

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SpellCarrier")) {
            this.entityData.set(DATA_CARRIER, tag.getString("SpellCarrier"));
        }
        if (tag.contains("SpellElement")) {
            this.entityData.set(DATA_ELEMENT, tag.getString("SpellElement"));
        }
        if (tag.contains("SpellEffect")) {
            this.entityData.set(DATA_EFFECT, tag.getString("SpellEffect"));
        }
        if (tag.contains("BasePower")) {
            this.entityData.set(DATA_BASE_POWER, tag.getFloat("BasePower"));
        }
        this.isSplit = tag.getBoolean("IsSplit");
        this.age = tag.getInt("Age");
        // Round 46: augment stacks
        this.augPierceStacks = tag.getInt("AugPierce");
        this.augChainStacks = tag.getInt("AugChain");
        this.augExtendStacks = tag.getInt("AugExtend");
        this.augHomingStacks = tag.getInt("AugHoming");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("SpellCarrier", this.entityData.get(DATA_CARRIER));
        tag.putString("SpellElement", this.entityData.get(DATA_ELEMENT));
        tag.putString("SpellEffect", this.entityData.get(DATA_EFFECT));
        tag.putFloat("BasePower", this.entityData.get(DATA_BASE_POWER));
        tag.putBoolean("IsSplit", this.isSplit);
        tag.putInt("Age", this.age);
        // Round 46: augment stacks
        tag.putInt("AugPierce", this.augPierceStacks);
        tag.putInt("AugChain", this.augChainStacks);
        tag.putInt("AugExtend", this.augExtendStacks);
        tag.putInt("AugHoming", this.augHomingStacks);
    }

    /** Round 46: 由 SpellBookItem.castActiveSlot 在 spawn 后调用 — 装备 augment glyph 计数 */
    public void setAugments(int pierceStacks, int chainStacks, int extendStacks, int homingStacks) {
        this.augPierceStacks = Math.max(0, pierceStacks);
        this.augChainStacks = Math.max(0, chainStacks);
        this.augExtendStacks = Math.max(0, extendStacks);
        this.augHomingStacks = Math.max(0, homingStacks);
    }

    // ═══════════════════════════════════════════
    //  Accessors
    // ═══════════════════════════════════════════

    public SpellCarrier getCarrier() {
        return SpellCarrier.getById(this.entityData.get(DATA_CARRIER));
    }

    public SpellElement getElement() {
        return SpellElement.getById(this.entityData.get(DATA_ELEMENT));
    }

    public SpellEffect getEffect() {
        return SpellEffect.getById(this.entityData.get(DATA_EFFECT));
    }

    public float getBasePower() {
        return this.entityData.get(DATA_BASE_POWER);
    }

    private static boolean canHitTarget(Entity entity, Entity owner) {
        return entity != null
                && entity != owner
                && entity.isAlive()
                && !EntityCompatUtil.isProtectedPlayer(entity);
    }

    // ═══════════════════════════════════════════
    //  Gravity
    // ═══════════════════════════════════════════

    @Override
    protected float getGravity() {
        return getCarrier().gravity * 0.03F;
    }

    // ═══════════════════════════════════════════
    //  tick() — particles, homing, timeout
    // ═══════════════════════════════════════════

    @Override
    public void tick() {
        super.tick();
        this.age++;

        // Round 46: Extend augment 延长投射存活时长（每层 +50%）
        int maxAge = 100 + 50 * augExtendStacks;
        if (this.age > maxAge) {
            this.discard();
            return;
        }

        // Client-side: minimal ambient particles (main visuals handled by shader renderer)
        if (this.level().isClientSide && this.age % 3 == 0) {
            SpellElement element = getElement();
            if (element != null) {
                spawnElementFlair(element, this.getX(), this.getY(), this.getZ());
            }
        }

        // Server-side: homing logic + EnderDragon collision
        if (!this.level().isClientSide) {
            SpellEffect effect = getEffect();
            // Round 46: Homing augment 同时触发追踪（与 SpellEffect.HOMING 并存）
            if (effect == SpellEffect.HOMING || augHomingStacks > 0) {
                applyHoming();
            }

            // EnderDragonPart 碰撞检测：用距离判定，弹射物太快可能穿过碰撞箱
            for (net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon :
                    this.level().getEntitiesOfClass(net.minecraft.world.entity.boss.enderdragon.EnderDragon.class,
                            this.getBoundingBox().inflate(8.0), e -> e.isAlive())) {
                for (net.minecraft.world.entity.boss.EnderDragonPart part : dragon.getSubEntities()) {
                    double dist = this.distanceToSqr(part.getX(), part.getY() + part.getBbHeight() * 0.5, part.getZ());
                    double hitRange = (part.getBbWidth() * 0.5 + 0.5);
                    if (dist < hitRange * hitRange) {
                        this.onHitEntity(new net.minecraft.world.phys.EntityHitResult(part));
                        return;
                    }
                }
            }
        }
    }

    private void applyHoming() {
        AABB searchArea = this.getBoundingBox().inflate(8.0);
        List<Entity> candidates = this.level().getEntities(this, searchArea,
                e -> canHitTarget(e, this.getOwner()) && (e instanceof LivingEntity || EntityCompatUtil.isGoetyObsidianMonolith(e)));

        if (candidates.isEmpty()) return;

        Entity nearest = candidates.stream()
                .min(Comparator
                        .comparingInt((Entity e) -> EntityCompatUtil.isGoetyObsidianMonolith(e) ? 0 : 1)
                        .thenComparingDouble(e -> e.distanceToSqr(this)))
                .orElse(null);

        if (nearest == null) return;

        Vec3 targetPos = nearest.position();
        if (nearest instanceof LivingEntity living) {
            targetPos = targetPos.add(0, living.getBbHeight() * 0.5, 0);
        }
        Vec3 toTarget = targetPos
                .subtract(this.position()).normalize();
        Vec3 currentVel = this.getDeltaMovement();
        double speed = currentVel.length();

        // Adjust velocity toward target by 5% per tick
        Vec3 newVel = currentVel.normalize().scale(0.95).add(toTarget.scale(0.05)).normalize().scale(speed);
        this.setDeltaMovement(newVel);
    }

    // ═══════════════════════════════════════════
    //  onHitEntity — damage + element effects + spell effect modifiers
    // ═══════════════════════════════════════════

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) return;

        Entity hitEntity = result.getEntity();
        // EnderDragonPart — 必须对部件调用 hurt()，且 DamageSource 的 directEntity 必须是 Player
        if (hitEntity instanceof net.minecraft.world.entity.boss.EnderDragonPart part) {
            if (this.level().isClientSide) return;
            Entity ownerEntity = this.getOwner();
            if (ownerEntity == part.parentMob) return;
            SpellElement element = getElement();
            float damage = element.baseDamage * getBasePower();
            Player ownerPlayer = (ownerEntity instanceof Player p) ? p : null;
            if (ownerPlayer != null) {
                com.huige233.transcend.ascension.PlayerAscensionData ascData =
                        com.huige233.transcend.ascension.AscensionCapability.get(ownerPlayer);
                damage *= ascData.getSpellDamageMultiplier(element, ownerPlayer);
            }
            SpellEffect effect = getEffect();
            if (effect == SpellEffect.AMPLIFY) damage *= 1.5F;
            net.minecraft.world.damagesource.DamageSource src = getElementDamageSource(element, null);
            part.hurt(src, damage);
            if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                spawnImpactBurst(sl, part.getX(), part.getY() + part.getBbHeight() * 0.5, part.getZ(), element);
            }
            if (effect != SpellEffect.PIERCING) {
                // Round 46: dragon part hit — also respect pierce augment
                if (augPierceStacks > 0) {
                    augPierceStacks--;
                } else {
                    this.discard();
                }
            }
            return;
        }
        if (!(hitEntity instanceof LivingEntity target)) return;

        Entity ownerEntity = this.getOwner();
        if (ownerEntity == target) return; // Don't hit caster
        if (EntityCompatUtil.isProtectedPlayer(target)) return;

        SpellElement element = getElement();
        SpellEffect effect = getEffect();
        float damage = element.baseDamage * getBasePower();

        // ── 飞升：法术强度 + 专精加成 + 暴击 ──────────────────────────────
        Player ownerPlayer = (ownerEntity instanceof Player p) ? p : null;
        if (ownerPlayer != null) {
            com.huige233.transcend.ascension.PlayerAscensionData ascData =
                    com.huige233.transcend.ascension.AscensionCapability.get(ownerPlayer);
            damage *= ascData.getSpellDamageMultiplier(element, ownerPlayer);

            // 暴击判定 — v3 改读 attribute（包含装备 / curio / 3rd-party modifier）
            com.huige233.transcend.ascension.AscensionStatBlock stats = ascData.buildTotalStats();
            float critChance = com.huige233.transcend.ascension.AscensionHandler.getCritChance(ownerPlayer);
            if (ownerPlayer.getRandom().nextFloat() < critChance) {
                damage *= stats.critMultiplier;
                // 暴击粒子提示
                if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    sl.broadcastEntityEvent(target, (byte) 60);
                }
            }

            // Armor penetration
            float armorPen = stats.getEffectiveArmorPen();
            if (armorPen > 0) {
                damage *= (1.0f + armorPen * 0.5f);
            }
            // Execute threshold: bonus damage on low HP targets
            {
                float hpRatio = target.getHealth() / target.getMaxHealth();
                java.util.List<com.huige233.transcend.ascension.tree.PassiveEffect> passives =
                        com.huige233.transcend.ascension.tree.TreeRegistry.getInstance()
                        .getActivePassives(ascData.getUnlockedNodes());
                for (var p : passives) {
                    if (p instanceof com.huige233.transcend.ascension.tree.PassiveEffect.ExecuteThreshold et) {
                        if (hpRatio < et.threshold()) damage *= (1.0f + et.bonusDamage());
                    }
                }
            }
            // Spell vamp: heal caster
            float vampRate = stats.getEffectiveSpellVamp();
            if (vampRate > 0) {
                ownerPlayer.heal(damage * vampRate);
            }
        }

        // AMPLIFY effect: +50% damage
        if (effect == SpellEffect.AMPLIFY) damage *= 1.5F;

        // MARK check: targets with mark take +25% damage
        if (target.getPersistentData().getInt("transcend_mark") > 0) {
            damage *= 1.25F;
        }

        // HEALING effect: if target is player, heal instead of damage
        if (effect == SpellEffect.HEALING && target instanceof Player targetPlayer) {
            targetPlayer.heal(damage);
            applyEffectModifiers(effect, target, ownerPlayer);
            if (effect != SpellEffect.PIERCING) {
                this.discard();
            }
            return;
        }

        // Apply element damage + secondary effects
        applyElementDamage(target, element, damage, ownerPlayer);
        applySoulShockFromBoss(target, ownerEntity);

        // 超越化身命中施加禁疗+湮灭+禁传送（不影响创造/观察者）
        if (ownerEntity instanceof com.huige233.transcend.entity.boss.TranscendenceAvatar avatar) {
            boolean immune = target instanceof Player p && (p.isCreative() || p.isSpectator());
            if (!immune) {
                int phase = avatar.getCurrentPhase().ordinal();
                int debuffLevel = phase;
                int debuffDuration = 60 + phase * 40;
                target.addEffect(new MobEffectInstance(
                        com.huige233.transcend.init.ModEffects.ANTI_HEAL.get(),
                        debuffDuration, debuffLevel, false, true));
                target.addEffect(new MobEffectInstance(
                        com.huige233.transcend.init.ModEffects.ANNIHILATION.get(),
                        debuffDuration, debuffLevel, false, true));
                target.getPersistentData().putBoolean("transcend_tp_lock", true);
                target.getPersistentData().putInt("transcend_tp_lock_time", debuffDuration);
            }
        }

        // Hit flash particles
        if (this.level() instanceof ServerLevel sl) {
            ElementReaction.spawnHitFlash(sl, target, element);
        }

        ElementReaction.tryReaction(target, element, damage, ownerEntity instanceof LivingEntity le ? le : null);

        grantXpToOwner(1); // +1 XP for hit

        // +3 XP if this kills the target
        if (target.getHealth() <= 0 || target.isDeadOrDying()) {
            grantXpToOwner(3);
        }

        // Apply effect modifiers
        if (effect != null) {
            applyEffectModifiers(effect, target, ownerPlayer);
        }

        // Impact burst particles
        if (this.level() instanceof ServerLevel sl) {
            spawnImpactBurst(sl, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), element);
        }

        // Discard unless PIERCING (Round 46: 也不 discard 如果 augPierceStacks > 0；Chain augment 改向最近敌人)
        if (effect != SpellEffect.PIERCING) {
            // Pierce augment：消耗一层，继续穿透
            if (augPierceStacks > 0) {
                augPierceStacks--;
                return;
            }
            // Chain augment：弹向另一敌人
            if (augChainStacks > 0) {
                augChainStacks--;
                if (redirectToNearestNonTarget(target)) {
                    return;
                }
            }
            this.discard();
        }
    }

    /**
     * Round 46: Chain augment — 弹向 8 格内最近的非当前 target 敌人。
     * 改写当前投射的速度方向，保持速度模长。
     */
    private boolean redirectToNearestNonTarget(LivingEntity excluded) {
        AABB area = this.getBoundingBox().inflate(8.0);
        Entity owner = this.getOwner();
        List<Entity> candidates = this.level().getEntities(this, area,
                e -> e != excluded && canHitTarget(e, owner) && e instanceof LivingEntity);
        if (candidates.isEmpty()) return false;
        Entity nearest = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(this)))
                .orElse(null);
        if (nearest == null) return false;
        Vec3 targetPos = nearest.position();
        if (nearest instanceof LivingEntity le) {
            targetPos = targetPos.add(0, le.getBbHeight() * 0.5, 0);
        }
        Vec3 dir = targetPos.subtract(this.position()).normalize();
        double speed = this.getDeltaMovement().length();
        this.setDeltaMovement(dir.scale(Math.max(0.5, speed)));
        // 重置 age 给一点时间到达新目标
        this.age = Math.max(0, this.age - 30);
        return true;
    }

    private void applySoulShockFromBoss(LivingEntity target, Entity ownerEntity) {
        if (!(ownerEntity instanceof com.huige233.transcend.entity.boss.AbstractTranscendBoss boss)) return;
        if (EntityCompatUtil.isProtectedPlayer(target)) return;
        int debuffLevel = Math.max(0, boss.getCurrentPhase().ordinal());
        int debuffDuration = 120 + debuffLevel * 40;
        target.addEffect(new MobEffectInstance(
                com.huige233.transcend.init.ModEffects.SOUL_SHOCK.get(),
                debuffDuration, debuffLevel, false, true));
    }

    private void applyElementDamage(LivingEntity target, SpellElement element,
                                    float damage, Player ownerPlayer) {
        if (EntityCompatUtil.isProtectedPlayer(target)) return;
        net.minecraft.world.damagesource.DamageSource src = getElementDamageSource(element, target);
        switch (element) {
            case FIRE -> {
                target.hurt(src, damage);
                target.setSecondsOnFire(3);
            }
            case ICE -> {
                target.hurt(src, damage);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, true));
                target.setTicksFrozen(target.getTicksFrozen() + 40);
            }
            case THUNDER -> {
                target.hurt(src, damage);
                if (this.level() instanceof ServerLevel serverLevel) {
                    RainbowLightning bolt = new RainbowLightning(serverLevel,
                            target.getX(), target.getY(), target.getZ());
                    serverLevel.addFreshEntity(bolt);
                }
            }
            case WIND -> {
                target.hurt(src, damage);
                Vec3 knockDir = target.position().subtract(this.position()).normalize();
                target.knockback(2.0F, -knockDir.x, -knockDir.z);
                target.hurtMarked = true;
            }
            case EARTH -> {
                target.hurt(src, damage);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1, false, true));
            }
            case VOID -> {
                target.hurt(src, damage);
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1, false, true));
            }
            case HOLY -> {
                if (target instanceof Monster) {
                    target.hurt(src, damage * 2.0F);
                } else if (target instanceof Player targetPlayer) {
                    targetPlayer.heal(damage);
                } else {
                    target.hurt(src, damage);
                }
            }
            case BLOOD -> {
                target.hurt(src, damage);
                target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 1, false, true));
                if (this.getOwner() instanceof LivingEntity caster) {
                    caster.heal(1.0F);
                }
            }
            case DARK -> {
                target.hurt(src, damage);
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, true));
            }
            case LIGHT -> {
                float lightDmg = (target instanceof Monster) ? damage * 1.5F : damage;
                target.hurt(src, lightDmg);
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, true));
            }
            case POISON -> {
                target.hurt(src, damage);
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1, false, true));
            }
            case TIME -> {
                target.hurt(src, damage);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3, false, true));
            }
            case SPACE -> {
                target.hurt(src, damage);
                double angle = level().getRandom().nextDouble() * Math.PI * 2;
                double dx = Math.cos(angle) * 5.0;
                double dz = Math.sin(angle) * 5.0;
                target.teleportTo(target.getX() + dx, target.getY(), target.getZ() + dz);
            }
            case NATURE -> {
                target.hurt(src, damage);
                if (this.getOwner() instanceof LivingEntity caster) {
                    caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, true));
                }
            }
            case CHAOS -> {
                target.hurt(src, damage);
                int roll = level().getRandom().nextInt(7);
                switch (roll) {
                    case 0 -> target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 1, false, true));
                    case 1 -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, true));
                    case 2 -> target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, true));
                    case 3 -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1, false, true));
                    case 4 -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3, false, true));
                    case 5 -> {
                        double a = level().getRandom().nextDouble() * Math.PI * 2;
                        target.teleportTo(target.getX() + Math.cos(a) * 5.0, target.getY(), target.getZ() + Math.sin(a) * 5.0);
                    }
                    case 6 -> {
                        if (this.getOwner() instanceof LivingEntity caster) {
                            caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, true));
                        }
                    }
                }
            }
            case ACID -> {
                target.hurt(src, damage);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, false, true));
                var armorAttr = target.getAttribute(Attributes.ARMOR);
                if (armorAttr != null) {
                    armorAttr.removeModifier(ACID_ARMOR_UUID);
                    armorAttr.addTransientModifier(new AttributeModifier(
                            ACID_ARMOR_UUID,
                            "spell_acid", -4.0, AttributeModifier.Operation.ADDITION));
                }
            }
            case SONIC -> {
                target.hurt(src, damage);
                if (level().getRandom().nextFloat() < 0.5F) {
                    target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 1, false, true));
                }
            }
            case ELDRITCH -> {
                target.hurt(src, damage);
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 2, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 1, false, true));
            }
        }
    }

    private net.minecraft.world.damagesource.DamageSource getElementDamageSource(SpellElement element, LivingEntity target) {
        Entity owner = this.getOwner();
        if (target != null && EntityCompatUtil.isBotaniaGaiaGuardian(target)) {
            if (owner instanceof Player player && !EntityCompatUtil.isProtectedPlayer(player)) {
                return player.damageSources().playerAttack(player);
            }
            Player compatPlayer = EntityCompatUtil.findNearestValidPlayer(this.level(), target, 64.0);
            if (compatPlayer == null) {
                compatPlayer = EntityCompatUtil.findNearestValidPlayer(this.level(), this, 64.0);
            }
            if (compatPlayer != null) {
                return compatPlayer.damageSources().playerAttack(compatPlayer);
            }
        }
        // 所有元素都使用带有entity引用的DamageSource，确保boss能识别伤害来源
        LivingEntity ownerLiving = owner instanceof LivingEntity le ? le : null;
        return switch (element) {
            case FIRE -> this.damageSources().mobProjectile(this, ownerLiving);
            case SONIC -> this.damageSources().sonicBoom(this);
            default -> this.damageSources().indirectMagic(this, ownerLiving);
        };
    }

    private void applyEffectModifiers(SpellEffect effect, LivingEntity target, Player ownerPlayer) {
        if (effect == null) return;

        switch (effect) {
            case EXPLOSION -> {
                this.level().explode(this, this.getX(), this.getY(), this.getZ(),
                        2.0F, Level.ExplosionInteraction.NONE);
            }
            case PIERCING -> {
                // Don't discard — handled in onHitEntity
            }
            case SPLIT -> {
                if (!this.isSplit) {
                    SpellCarrier carrier = getCarrier();
                    SpellElement element = getElement();
                    float basePower = getBasePower();

                    for (int i = 0; i < 3; i++) {
                        SpellProjectile splitProj = new SpellProjectile(
                                this.level(), this.getX(), this.getY(), this.getZ(),
                                carrier, element, basePower * 0.5F, this.getOwner());
                        // Random direction
                        double angle = this.random.nextDouble() * Math.PI * 2;
                        double pitch = (this.random.nextDouble() - 0.5) * Math.PI * 0.5;
                        double speed = 0.4 + this.random.nextDouble() * 0.3;
                        double vx = Math.cos(angle) * Math.cos(pitch) * speed;
                        double vy = Math.sin(pitch) * speed;
                        double vz = Math.sin(angle) * Math.cos(pitch) * speed;
                        splitProj.setDeltaMovement(vx, vy, vz);
                        this.level().addFreshEntity(splitProj);
                    }
                }
            }
            case HOMING -> {
                // Handled in tick()
            }
            case HEALING -> {
                // Healing is handled in onHitEntity before damage
            }
            case SHIELD -> {
                if (ownerPlayer != null) {
                    ownerPlayer.addEffect(new MobEffectInstance(
                            MobEffects.ABSORPTION, 200, 1, false, true));
                }
            }
            case CHAIN_LIGHTNING, BOUNCE, DELAYED, AMPLIFY, LIFESTEAL,
                 QUICKCAST, MULTISHOT, SLOWFIELD, GRAVITY_WELL, MARK -> {
                switch (effect) {
                    case CHAIN_LIGHTNING -> {
                        AABB chainArea = target.getBoundingBox().inflate(4.0);
                        Entity ownerEntity = this.getOwner();
                        List<LivingEntity> chainTargets = this.level().getEntitiesOfClass(LivingEntity.class, chainArea,
                                e -> canHitTarget(e, ownerEntity) && e != target);
                        chainTargets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(target)));
                        int chains = Math.min(3, chainTargets.size());
                        for (int i = 0; i < chains; i++) {
                            LivingEntity chainTarget = chainTargets.get(i);
                            chainTarget.hurt(getElementDamageSource(getElement(), chainTarget),
                                    getElement().baseDamage * getBasePower() * 0.6F);
                        }
                    }
                    case AMPLIFY -> {
                        target.hurt(getElementDamageSource(getElement(), target), getElement().baseDamage * getBasePower() * 0.5F);
                    }
                    case LIFESTEAL -> {
                        if (ownerPlayer != null) {
                            float healed = getElement().baseDamage * getBasePower() * 0.3F;
                            ownerPlayer.heal(healed);
                        }
                    }
                    case SLOWFIELD -> {
                        Vec3 impactPos = this.position();
                        AABB slowBox = new AABB(impactPos.x - 3, impactPos.y - 1, impactPos.z - 3,
                                impactPos.x + 3, impactPos.y + 3, impactPos.z + 3);
                        Entity ownerEntity = this.getOwner();
                        this.level().getEntitiesOfClass(LivingEntity.class, slowBox,
                                        e -> canHitTarget(e, ownerEntity))
                                .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, true)));
                    }
                    case GRAVITY_WELL -> {
                        Vec3 impactPos = this.position();
                        AABB pullBox = new AABB(impactPos.x - 4, impactPos.y - 2, impactPos.z - 4,
                                impactPos.x + 4, impactPos.y + 4, impactPos.z + 4);
                        Entity ownerEntity = this.getOwner();
                        this.level().getEntitiesOfClass(LivingEntity.class, pullBox,
                                        e -> canHitTarget(e, ownerEntity))
                                .forEach(e -> {
                                    Vec3 pull = impactPos.subtract(e.position()).normalize().scale(0.5);
                                    e.setDeltaMovement(e.getDeltaMovement().add(pull));
                                    e.hurtMarked = true;
                                });
                    }
                    case MARK -> {
                        target.getPersistentData().putInt("transcend_mark", 100);
                    }
                    default -> {}
                }
            }
            case ECHO -> {
                // Echo: immediate 50% damage to all entities within 2 blocks of impact (visual echo flavor)
                Vec3 impactPos = this.position();
                AABB echoBox = new AABB(impactPos.x - 2, impactPos.y - 2, impactPos.z - 2,
                        impactPos.x + 2, impactPos.y + 2, impactPos.z + 2);
                Entity ownerEntity = this.getOwner();
                float echoDamage = target.getMaxHealth() > 0
                        ? getElement().baseDamage * getBasePower() * 0.5F
                        : 1.0F;
                this.level().getEntitiesOfClass(LivingEntity.class, echoBox,
                                e -> canHitTarget(e, ownerEntity))
                        .forEach(e -> e.hurt(getElementDamageSource(getElement(), e), echoDamage));
            }
            case ARMOR_BREAK -> {
                var armorAttr = target.getAttribute(Attributes.ARMOR);
                if (armorAttr != null) {
                    armorAttr.removeModifier(ARMOR_BREAK_UUID);
                    armorAttr.addTransientModifier(new AttributeModifier(
                            ARMOR_BREAK_UUID,
                            "spell_armor_break", -8.0, AttributeModifier.Operation.ADDITION));
                    // 标记持续时间 120 ticks (6秒)，由 ShieldEventHandler tick 清理
                    target.getPersistentData().putInt("transcend_armor_break", 120);
                }
            }
            case ROOT -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4, false, true));
            }
            case BLIGHT -> {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 2, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 2, false, true));
            }
            case LINGERING -> {
                // Deal 1.5 extra damage to all entities in 2-block radius of impact
                Vec3 lingPos = this.position();
                AABB lingBox = new AABB(lingPos.x - 2, lingPos.y - 2, lingPos.z - 2,
                        lingPos.x + 2, lingPos.y + 2, lingPos.z + 2);
                Entity ownerEntity = this.getOwner();
                this.level().getEntitiesOfClass(LivingEntity.class, lingBox,
                                e -> canHitTarget(e, ownerEntity))
                        .forEach(e -> e.hurt(getElementDamageSource(getElement(), e), 1.5F));
            }
            case DEVOUR -> {
                float dmg = getElement().baseDamage * getBasePower();
                if (target.getHealth() <= dmg && ownerPlayer != null) {
                    var maxHealthAttr = ownerPlayer.getAttribute(Attributes.MAX_HEALTH);
                    if (maxHealthAttr != null) {
                        maxHealthAttr.removeModifier(DEVOUR_HEALTH_UUID);
                        maxHealthAttr.addTransientModifier(new AttributeModifier(
                                DEVOUR_HEALTH_UUID,
                                "spell_devour_temp", 2.0, AttributeModifier.Operation.ADDITION));
                        ownerPlayer.heal(2.0F);
                    }
                }
            }
            case ABSORB -> {
                // 30% of damage dealt becomes absorption hearts for caster
                if (ownerPlayer != null) {
                    float absorb = Math.min(target.getHealth(), getElement().baseDamage * getBasePower()) * 0.3F;
                    ownerPlayer.setAbsorptionAmount(ownerPlayer.getAbsorptionAmount() + absorb);
                }
            }
            case REFLECT -> {
                // Target's next attack within 5 seconds is reflected back
                if (target instanceof Mob mob) {
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 4));
                }
            }
            case CURSE -> {
                // Amplify all damage target receives by 50% for 8 seconds via Unluck
                target.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 160, 4));
                // Also prevent healing
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            }
            case OVERLOAD -> {
                // Deal 2x damage but also hurt caster for 25%
                float bonusDmg = getElement().baseDamage * getBasePower();
                target.hurt(getElementDamageSource(getElement(), target), bonusDmg);
                if (ownerPlayer != null) {
                    ownerPlayer.hurt(ownerPlayer.damageSources().magic(), bonusDmg * 0.25F);
                }
            }
            case WEAKEN -> {
                // Reduce target's attack damage
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 1));
            }
            case UNSTABLE -> {
                float roll = this.level().getRandom().nextFloat();
                if (roll < 0.3F) {
                    target.hurt(getElementDamageSource(getElement(), target), getElement().baseDamage * getBasePower() * 2.0F);
                } else if (roll > 0.8F && ownerPlayer != null) {
                    ownerPlayer.hurt(ownerPlayer.damageSources().magic(), getElement().baseDamage * 0.5F);
                }
            }
            case SHATTER -> {
                // Remove ALL armor from target for 6 seconds (via attribute modifier)
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 3));
                // Also break shields
                if (target instanceof Player targetPlayer) {
                    targetPlayer.getCooldowns().addCooldown(net.minecraft.world.item.Items.SHIELD, 120);
                }
            }
            case SUMMON_WISP -> {
                // Spawn a SpellWisp at impact point
                if (this.level() instanceof ServerLevel serverLevel) {
                    com.huige233.transcend.entity.SpellWisp wisp = new com.huige233.transcend.entity.SpellWisp(
                            com.huige233.transcend.init.ModEntities.SPELL_WISP.get(), serverLevel);
                    wisp.setPos(this.getX(), this.getY(), this.getZ());
                    if (this.getOwner() instanceof LivingEntity livingOwner) {
                        wisp.setOwner(livingOwner);
                    }
                    serverLevel.addFreshEntity(wisp);
                }
            }
            case SUMMON_GUARDIAN -> {
                // Spawn a SpellGuardian at impact point
                if (this.level() instanceof ServerLevel serverLevel) {
                    com.huige233.transcend.entity.SpellGuardian guardian = new com.huige233.transcend.entity.SpellGuardian(
                            com.huige233.transcend.init.ModEntities.SPELL_GUARDIAN.get(), serverLevel);
                    guardian.setPos(this.getX(), this.getY(), this.getZ());
                    if (this.getOwner() instanceof LivingEntity livingOwner) {
                        guardian.setOwner(livingOwner);
                    }
                    serverLevel.addFreshEntity(guardian);
                }
            }
        }
    }

    private void grantXpToOwner(int amount) {
        Entity owner = this.getOwner();
        if (!(owner instanceof Player player)) return;

        // Find wand in player's hands
        for (InteractionHand hand : InteractionHand.values()) {
            net.minecraft.world.item.ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof com.huige233.transcend.items.TranscendWand) {
                int selected = held.getOrCreateTag().getInt("selected_slot");
                com.huige233.transcend.items.TranscendWand.addSpellXp(held, selected, amount);
                return;
            }
        }
    }

    // ═══════════════════════════════════════════
    //  onHitBlock — AOE effects if carrier has aoeRadius > 0
    // ═══════════════════════════════════════════

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level().isClientSide) return;

        SpellCarrier carrier = getCarrier();
        SpellEffect effect = getEffect();
        float damage = getElement().baseDamage * getBasePower();

        if (carrier.aoeRadius > 0) {
            Vec3 hitPos = result.getLocation();
            double r = carrier.aoeRadius;
            AABB area = new AABB(hitPos.x - r, hitPos.y - r, hitPos.z - r,
                    hitPos.x + r, hitPos.y + r, hitPos.z + r);

            Entity ownerEntity = this.getOwner();
            List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, area,
                    e -> canHitTarget(e, ownerEntity));

            for (LivingEntity target : targets) {
                applyElementDamage(target, getElement(), damage, ownerEntity instanceof Player p ? p : null);
            }
        }

        // Apply explosion effect on block hit too
        if (effect == SpellEffect.EXPLOSION) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(),
                    2.0F, Level.ExplosionInteraction.NONE);
        }

        // DELAYED effect: create a delayed explosion at impact
        if (effect == SpellEffect.DELAYED) {
            Vec3 hitPos = result.getLocation();
            Entity ownerEntity = this.getOwner();
            // Schedule detonation by spawning a new projectile that stays in place
            SpellProjectile delayed = new SpellProjectile(
                    this.level(), hitPos.x, hitPos.y, hitPos.z,
                    getCarrier(), getElement(), getBasePower() * 1.5F, ownerEntity);
            delayed.setDeltaMovement(0, 0, 0);
            delayed.setNoGravity(true);
            this.level().addFreshEntity(delayed);
        }

        if (this.level() instanceof ServerLevel sl) {
            spawnImpactBurst(sl, this.getX(), this.getY(), this.getZ(), getElement());
        }

        // BOUNCE effect: reflect off blocks up to 3 times
        if (effect == SpellEffect.BOUNCE && this.bounceCount < 3) {
            this.bounceCount++;
            Vec3 motion = this.getDeltaMovement();
            net.minecraft.core.Direction face = result.getDirection();
            Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
            Vec3 reflected = motion.subtract(normal.scale(2.0 * motion.dot(normal)));
            this.setDeltaMovement(reflected);
            this.setPos(result.getLocation().add(normal.scale(0.1)));
            return;
        }

        this.discard();
    }

    private void spawnCarrierParticles(SpellElement element, SpellCarrier carrier) {
        Vec3 vel = this.getDeltaMovement();
        double px = this.getX(), py = this.getY(), pz = this.getZ();
        spawnCarrierShaderFx(element, carrier, px, py, pz, vel);
    }

    private void spawnElementFlair(SpellElement element, double px, double py, double pz) {
        spawnElementShaderFlair(element, px, py, pz);
    }

    private double rOff() {
        return (this.random.nextDouble() - 0.5) * 0.5;
    }

    private void spawnCarrierShaderFx(SpellElement element, SpellCarrier carrier,
                                      double px, double py, double pz, Vec3 vel) {
        float r = element.particleR;
        float g = element.particleG;
        float b = element.particleB;
        Vec3 center = new Vec3(px, py + 0.05, pz);
        Vec3 dir = vel.lengthSqr() > 1.0E-4 ? vel.normalize() : new Vec3(0.0, 0.1, 0.0);
        Vec3 tail = center.subtract(dir.scale(1.2));

        switch (carrier) {
            case ORB -> {
                ShaderSpellRenderer.addShieldRipple(center, 0.9F, r, g, b, 12);
            }
            case ARROW -> {
                ShaderSpellRenderer.addSpellEffect(tail, center, r, g, b, 10, "beam");
                if ((this.age % 3) == 0) {
                    ShaderSpellRenderer.addShieldRipple(center, 0.45F, r, g, b, 8);
                }
            }
            case VORTEX -> {
                ShaderSpellRenderer.addShieldRipple(center, 0.95F, r, g, b, 12);
                ShaderSpellRenderer.addSpellEffect(center.add(0.8, 0.15, 0.0), center, r, g, b, 10, "slash");
            }
            case TRAP -> {
                ShaderSpellRenderer.addShieldRipple(center, 0.6F, r, g, b, 12);
            }
            case RAIN -> {
                ShaderSpellRenderer.addSpellEffect(center.add(0.0, 0.9, 0.0), center, r, g, b, 10, "beam");
            }
            default -> {
                ShaderSpellRenderer.addShieldRipple(center, 0.55F, r, g, b, 10);
            }
        }

        spawnElementShaderFlair(element, px, py, pz);
    }

    private void spawnElementShaderFlair(SpellElement element, double px, double py, double pz) {
        float r = element.particleR;
        float g = element.particleG;
        float b = element.particleB;
        Vec3 center = new Vec3(px, py + 0.02, pz);
        int gate = switch (element) {
            case THUNDER, SPACE, TIME, SONIC -> 3;
            case CHAOS, VOID, ELDRITCH -> 2;
            default -> 4;
        };
        if ((this.age % gate) != 0) return;

        // 只保留spell效果光束，不画地面法阵
        String spellType = switch (element) {
            case THUNDER, SONIC -> "beam";
            case VOID, DARK, ELDRITCH, CHAOS -> "slash";
            case SPACE, TIME -> "nova";
            default -> "beam";
        };
        ShaderSpellRenderer.addSpellEffect(center.add(rOff() * 0.35, 0.25 + rOff() * 0.2, rOff() * 0.35),
                center, r, g, b, 8, spellType);
    }

    private void spawnImpactBurst(ServerLevel level, double x, double y, double z, SpellElement element) {
        float r = element.particleR;
        float g = element.particleG;
        float b = element.particleB;
        float radius = switch (element) {
            case CHAOS, VOID, ELDRITCH -> 3.2F;
            case THUNDER, FIRE -> 2.9F;
            case HOLY, LIGHT -> 2.6F;
            default -> 2.4F;
        };
        Vec3 center = new Vec3(x, y + 0.08, z);
        // 只保留冲击波，不画地面法阵
        ShaderSpellRenderer.addShockwave(center, radius, r, g, b, 18);
        if (element == SpellElement.THUNDER || element == SpellElement.SONIC) {
            ShaderSpellRenderer.addSpellEffect(
                    new Vec3(x, y + 1.8, z),
                    new Vec3(x, y + 0.1, z),
                    1.0F, 1.0F, 0.55F, 12, "beam");
        }
    }
}
