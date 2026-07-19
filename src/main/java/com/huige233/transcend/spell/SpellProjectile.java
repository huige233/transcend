package com.huige233.transcend.spell;

import com.huige233.transcend.ascension.resource.ClassResourceHandler;
import com.huige233.transcend.init.ModEntities;
import com.huige233.transcend.spell.config.ConfiguredSpell;
import com.huige233.transcend.spell.config.SpellConfigurationRules;
import com.huige233.transcend.util.EntityCompatUtil;
import com.huige233.transcend.visual.ServerVisualBroadcaster;
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
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SpellProjectile extends ThrowableProjectile {

    private static final UUID ARMOR_BREAK_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");

    private static final EntityDataAccessor<String> DATA_CARRIER =
            SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_ELEMENT =
            SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_EFFECT =
            SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_EFFECTS =
            SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_BASE_POWER =
            SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_SPELL_TIER =
            SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.INT);

    private boolean isSplit = false;
    private int bounceCount = 0;
    private int age = 0;
    private int remainingEffectPierces = 0;

    private int augPierceStacks = 0;
    private int augChainStacks = 0;
    private int augExtendStacks = 0;
    private int augHomingStacks = 0;

    public SpellProjectile(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    public SpellProjectile(Level level, LivingEntity owner, SpellCarrier carrier,
                           SpellElement element, SpellEffect effect, float basePower, int spellTier) {
        this(level, owner, carrier, element, effect == null ? List.of() : List.of(effect), basePower, spellTier);
    }

    public SpellProjectile(Level level, LivingEntity owner, SpellCarrier carrier,
                           SpellElement element, List<SpellEffect> effects, float basePower, int spellTier) {
        super(ModEntities.SPELL_PROJECTILE.get(), level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.entityData.set(DATA_CARRIER, carrier.id);
        this.entityData.set(DATA_ELEMENT, element.canonical().id);
        setEffects(effects);
        this.remainingEffectPierces = SpellConfigurationRules.piercingExtraPenetrations(effects);
        this.entityData.set(DATA_BASE_POWER, boundedBasePower(basePower));
        this.entityData.set(DATA_SPELL_TIER, Math.max(1, Math.min(12, spellTier)));
    }

    public SpellProjectile(Level level, double x, double y, double z,
                           SpellCarrier carrier, SpellElement element, List<SpellEffect> effects,
                           float basePower, int spellTier, Entity owner) {
        super(ModEntities.SPELL_PROJECTILE.get(), level);
        this.setOwner(owner);
        this.setPos(x, y, z);
        this.entityData.set(DATA_CARRIER, carrier.id);
        this.entityData.set(DATA_ELEMENT, element.id);
        setEffects(effects);
        this.remainingEffectPierces = SpellConfigurationRules.piercingExtraPenetrations(effects);
        this.entityData.set(DATA_BASE_POWER, boundedBasePower(basePower));
        this.entityData.set(DATA_SPELL_TIER, Math.max(1, Math.min(12, spellTier)));
        this.isSplit = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_CARRIER, "orb");
        this.entityData.define(DATA_ELEMENT, "fire");
        this.entityData.define(DATA_EFFECT, "");
        this.entityData.define(DATA_EFFECTS, "");
        this.entityData.define(DATA_BASE_POWER, 1.0F);
        this.entityData.define(DATA_SPELL_TIER, 1);
    }

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
        if (tag.contains("SpellEffects")) {
            this.entityData.set(DATA_EFFECTS, tag.getString("SpellEffects"));
        } else if (tag.contains("SpellEffect")) {
            setEffects(getEffect() == null ? List.of() : List.of(getEffect()));
        }
        if (tag.contains("BasePower")) {
            this.entityData.set(DATA_BASE_POWER, boundedBasePower(tag.getFloat("BasePower")));
        }
        if (tag.contains("SpellTier")) {
            this.entityData.set(DATA_SPELL_TIER, Math.max(1, Math.min(12, tag.getInt("SpellTier"))));
        }
        this.isSplit = tag.getBoolean("IsSplit");
        this.age = Math.max(0, Math.min(1200, tag.getInt("Age")));
        this.remainingEffectPierces = tag.contains("EffectPierces")
                ? Math.max(0, tag.getInt("EffectPierces"))
                : SpellConfigurationRules.piercingExtraPenetrations(getEffects());

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
        tag.putString("SpellEffects", this.entityData.get(DATA_EFFECTS));
        tag.putFloat("BasePower", this.entityData.get(DATA_BASE_POWER));
        tag.putInt("SpellTier", this.entityData.get(DATA_SPELL_TIER));
        tag.putBoolean("IsSplit", this.isSplit);
        tag.putInt("Age", this.age);
        tag.putInt("EffectPierces", this.remainingEffectPierces);

        tag.putInt("AugPierce", this.augPierceStacks);
        tag.putInt("AugChain", this.augChainStacks);
        tag.putInt("AugExtend", this.augExtendStacks);
        tag.putInt("AugHoming", this.augHomingStacks);
    }

    public void setAugments(int pierceStacks, int chainStacks, int extendStacks, int homingStacks) {
        this.augPierceStacks = Math.max(0, pierceStacks);
        this.augChainStacks = Math.max(0, chainStacks);
        this.augExtendStacks = Math.max(0, extendStacks);
        this.augHomingStacks = Math.max(0, homingStacks);
    }

    public SpellCarrier getCarrier() {
        return java.util.Objects.requireNonNull(
                SpellCarrier.getById(this.entityData.get(DATA_CARRIER)),
                "Spell projectile has an invalid carrier");
    }

    public SpellElement getElement() {
        return java.util.Objects.requireNonNull(
                SpellElement.getById(this.entityData.get(DATA_ELEMENT)),
                "Spell projectile has an invalid element");
    }

    public SpellEffect getEffect() {
        List<SpellEffect> effects = getEffects();
        return effects.isEmpty() ? null : effects.get(0);
    }

    public List<SpellEffect> getEffects() {
        String encoded = this.entityData.get(DATA_EFFECTS);
        if (encoded.isEmpty()) {
            String legacyId = this.entityData.get(DATA_EFFECT);
            SpellEffect legacy = SpellEffect.getById(legacyId);
            if (!legacyId.isEmpty() && legacy == null) {
                throw new IllegalStateException("Spell projectile has an invalid effect: " + legacyId);
            }
            return legacy == null ? List.of() : List.of(legacy);
        }
        List<SpellEffect> effects = new ArrayList<>();
        for (String id : encoded.split(",", ConfiguredSpell.MAX_EFFECTS + 1)) {
            if (effects.size() >= ConfiguredSpell.MAX_EFFECTS) break;
            SpellEffect effect = SpellEffect.getById(id);
            if (effect == null) throw new IllegalStateException("Spell projectile has an invalid effect: " + id);
            effects.add(effect);
        }
        return SpellConfigurationRules.canonicalEffects(effects);
    }

    private void setEffects(List<SpellEffect> effects) {
        List<SpellEffect> canonical = SpellConfigurationRules.canonicalEffects(effects);
        String encoded = canonical.stream().map(e -> e.id).collect(java.util.stream.Collectors.joining(","));
        this.entityData.set(DATA_EFFECTS, encoded);
        this.entityData.set(DATA_EFFECT, canonical.isEmpty() ? "" : canonical.get(0).id);
    }

    public float getBasePower() {
        return this.entityData.get(DATA_BASE_POWER);
    }

    private static float boundedBasePower(float power) {
        return Float.isFinite(power) ? Math.max(0.0F, Math.min(1000.0F, power)) : 1.0F;
    }

    public int getSpellTier() {
        return this.entityData.get(DATA_SPELL_TIER);
    }

    private static boolean canHitTarget(Entity entity, Entity owner) {
        return entity != null
                && entity != owner
                && entity.isAlive()
                && !EntityCompatUtil.isProtectedPlayer(entity);
    }

    @Override
    protected float getGravity() {
        return getCarrier().getGravity() * 0.03F;
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;

        int maxAge = 100 + 50 * augExtendStacks;
        if (this.age > maxAge) {
            this.discard();
            return;
        }

        if (this.level() instanceof ServerLevel serverLevel && this.age % 3 == 0) {
            SpellElement element = getElement();
            if (element != null) {
                spawnElementFlair(serverLevel, element, this.getX(), this.getY(), this.getZ());
            }
        }

        if (!this.level().isClientSide) {
            List<SpellEffect> effects = getEffects();

            if (effects.contains(SpellEffect.HOMING) || augHomingStacks > 0) {
                applyHoming();
            }

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
                e -> canHitTarget(e, this.getOwner()) && (e instanceof LivingEntity));

        if (candidates.isEmpty()) return;

        Entity nearest = candidates.stream()
                .min(Comparator
                        .comparingDouble(e -> e.distanceToSqr(this)))
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

        Vec3 newVel = currentVel.normalize().scale(0.95).add(toTarget.scale(0.05)).normalize().scale(speed);
        this.setDeltaMovement(newVel);
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) return;

        Entity hitEntity = result.getEntity();

        if (hitEntity instanceof net.minecraft.world.entity.boss.EnderDragonPart part) {
            if (this.level().isClientSide) return;
            Entity ownerEntity = this.getOwner();
            if (ownerEntity == part.parentMob) return;
            SpellElement element = getElement();
            List<SpellEffect> effects = getEffects();
            float damage = element.getBaseDamage() * getBasePower()
                    * SpellConfigurationRules.amplifyMultiplier(effects);
            Player ownerPlayer = (ownerEntity instanceof Player p) ? p : null;
            if (ownerPlayer != null) {
                com.huige233.transcend.ascension.PlayerAscensionData ascData =
                        com.huige233.transcend.ascension.AscensionCapability.get(ownerPlayer);
                damage *= ascData.getSpellDamageMultiplier(element, ownerPlayer);
                damage *= ClassResourceHandler.getSpellDamageMultiplier(ownerPlayer, element);
            }
            net.minecraft.world.damagesource.DamageSource src = getElementDamageSource(element, null);
            part.hurt(src, damage);
            if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                spawnImpactBurst(sl, part.getX(), part.getY() + part.getBbHeight() * 0.5, part.getZ(), element);
            }
            if (!consumeEntityPenetration()) this.discard();
            return;
        }
        if (!(hitEntity instanceof LivingEntity target)) return;

        Entity ownerEntity = this.getOwner();
        if (ownerEntity == target) return;
        if (EntityCompatUtil.isProtectedPlayer(target)) return;

        SpellElement element = getElement();
        List<SpellEffect> effects = getEffects();
        float damage = element.getBaseDamage() * getBasePower()
                * SpellConfigurationRules.amplifyMultiplier(effects);

        Player ownerPlayer = (ownerEntity instanceof Player p) ? p : null;
        if (ownerPlayer != null) {
            com.huige233.transcend.ascension.PlayerAscensionData ascData =
                    com.huige233.transcend.ascension.AscensionCapability.get(ownerPlayer);
            damage *= ascData.getSpellDamageMultiplier(element, ownerPlayer);

            com.huige233.transcend.ascension.AscensionStatBlock stats = ascData.buildTotalStats();

            com.huige233.transcend.ascension.AscensionVow devotionVow = ascData.getActiveTertiaryVow();
            boolean devotionCritBlocked = devotionVow != null
                    && "vow_of_devotion".equals(devotionVow.getId())
                    && !ascData.isVowLiberated(devotionVow.getId());
            float critChance = devotionCritBlocked ? 0f
                    : com.huige233.transcend.ascension.AscensionHandler.getCritChance(ownerPlayer);
            if (ownerPlayer.getRandom().nextFloat() < critChance) {
                damage *= stats.critMultiplier;

                if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    sl.broadcastEntityEvent(target, (byte) 60);
                }
            }

            float armorPen = stats.getEffectiveArmorPen();
            if (armorPen > 0) {
                damage *= (1.0f + armorPen * 0.5f);
            }
        }

        if (effects.contains(SpellEffect.HEALING) && target instanceof Player targetPlayer) {
            targetPlayer.heal(damage);
            for (SpellEffect effect : effects) applyEffectModifiers(effect, target, ownerPlayer, damage, 0.0F);
            if (!consumeEntityPenetration()) this.discard();
            return;
        }

        float actualHealthDamage = applyElementDamage(target, element, damage, ownerPlayer);
        applySoulShockFromBoss(target, ownerEntity);

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

        if (this.level() instanceof ServerLevel sl) {
            ElementReaction.spawnHitFlash(sl, target, element);
        }

        grantXpToOwner(1);

        if (target.getHealth() <= 0 || target.isDeadOrDying()) {
            grantXpToOwner(3);
        }

        spawnSplitChildren(effects);

        for (SpellEffect effect : effects) {
            applyEffectModifiers(effect, target, ownerPlayer, damage, actualHealthDamage);
        }

        if (this.level() instanceof ServerLevel sl) {
            spawnImpactBurst(sl, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), element);
        }

        if (!consumeEntityPenetration()) {

            if (augChainStacks > 0) {
                augChainStacks--;
                if (redirectToNearestNonTarget(target)) {
                    return;
                }
            }
            this.discard();
        }
    }

    private boolean consumeEntityPenetration() {
        if (remainingEffectPierces > 0) {
            remainingEffectPierces--;
            return true;
        }
        if (augPierceStacks > 0) {
            augPierceStacks--;
            return true;
        }
        return false;
    }

    private void spawnSplitChildren(List<SpellEffect> effects) {
        if (this.isSplit) return;
        int children = SpellConfigurationRules.splitPlan(effects).childCount();
        if (children == 0) return;
        List<SpellEffect> childEffects = effects.stream().filter(e -> e != SpellEffect.SPLIT).toList();
        for (int i = 0; i < children; i++) {
            SpellProjectile child = new SpellProjectile(this.level(), getX(), getY(), getZ(), getCarrier(),
                    getElement(), childEffects, getBasePower() * 0.5F, getSpellTier(), getOwner());
            double angle = this.random.nextDouble() * Math.PI * 2;
            double pitch = (this.random.nextDouble() - 0.5) * Math.PI * 0.5;
            double speed = 0.4 + this.random.nextDouble() * 0.3;
            child.setDeltaMovement(Math.cos(angle) * Math.cos(pitch) * speed, Math.sin(pitch) * speed,
                    Math.sin(angle) * Math.cos(pitch) * speed);
            this.level().addFreshEntity(child);
        }
    }

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

    private float applyElementDamage(LivingEntity target, SpellElement element,
                                    float damage, Player ownerPlayer) {
        if (ownerPlayer != null) {
            damage *= ClassResourceHandler.getSpellDamageMultiplier(ownerPlayer, element);
        }
        if (this.level() instanceof ServerLevel level) {
            LivingEntity caster = this.getOwner() instanceof LivingEntity living ? living : null;
            return SpellDamageService.deal(level, caster, this, target, element, getSpellTier(), damage, true)
                    .actualHealthDamage();
        }
        return 0.0F;
    }

    private net.minecraft.world.damagesource.DamageSource getElementDamageSource(SpellElement element, LivingEntity target) {
        Entity owner = this.getOwner();
        if (target != null && false) {
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

        LivingEntity ownerLiving = owner instanceof LivingEntity le ? le : null;
        return switch (element) {
            case FIRE -> this.damageSources().mobProjectile(this, ownerLiving);
            default -> this.damageSources().indirectMagic(this, ownerLiving);
        };
    }

    private void applyEffectModifiers(SpellEffect effect, LivingEntity target, Player ownerPlayer,
                                      float rawDamage, float actualHealthDamage) {
        if (effect == null) return;

        switch (effect) {
            case EXPLOSION -> {
                applyVisualExplosion(this.position(), rawDamage);
            }
            case PIERCING -> {

            }
            case SPLIT -> {

            }
            case HOMING -> {

            }
            case HEALING -> {

            }
            case SHIELD -> {
                if (ownerPlayer != null) {
                    ownerPlayer.addEffect(new MobEffectInstance(
                            MobEffects.ABSORPTION, 200, 1, false, true));
                }
            }
            case CHAIN_LIGHTNING, AMPLIFY, LIFESTEAL, MULTISHOT, SLOWFIELD, MARK -> {
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
                            dealSecondary(chainTarget, getElement().getBaseDamage() * getBasePower() * 0.6F);
                        }
                    }
                    case AMPLIFY -> { }
                    case LIFESTEAL -> {
                        if (ownerPlayer != null) {
                            ownerPlayer.heal(Math.max(0.0F, actualHealthDamage) * 0.15F);
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
                    case MARK -> {
                        target.getPersistentData().putInt(SpellDamageService.CANONICAL_MARK_TAG, 100);
                    }
                    default -> {}
                }
            }
            case ROOT -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4, false, true));
                Vec3 pull = this.position().subtract(target.position());
                if (pull.lengthSqr() > 0.0001) target.setDeltaMovement(target.getDeltaMovement().add(pull.normalize().scale(0.5)));
                target.hurtMarked = true;
            }
            case BLIGHT -> {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 2, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 2, false, true));

                Vec3 lingPos = this.position();
                AABB lingBox = new AABB(lingPos.x - 2, lingPos.y - 2, lingPos.z - 2,
                        lingPos.x + 2, lingPos.y + 2, lingPos.z + 2);
                Entity ownerEntity = this.getOwner();
                this.level().getEntitiesOfClass(LivingEntity.class, lingBox,
                                e -> canHitTarget(e, ownerEntity))
                        .forEach(e -> dealSecondary(e, 1.5F));
            }
            case CURSE -> {
                target.getPersistentData().putInt(SpellDamageService.CURSE_TAG, 160);
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            }
            case OVERLOAD -> {

                float bonusDmg = getElement().getBaseDamage() * getBasePower();
                dealSecondary(target, bonusDmg);
                if (ownerPlayer != null) {
                    ownerPlayer.hurt(ownerPlayer.damageSources().magic(), bonusDmg * 0.25F);
                }
            }
            case SHATTER -> {
                applyTimedArmorReduction(target);
                if (target instanceof Player targetPlayer) {
                    targetPlayer.getCooldowns().addCooldown(net.minecraft.world.item.Items.SHIELD, 120);
                }
            }
        }
    }

    private static void applyTimedArmorReduction(LivingEntity target) {
        var armor = target.getAttribute(Attributes.ARMOR);
        if (armor == null) return;
        armor.removeModifier(ARMOR_BREAK_UUID);
        armor.addTransientModifier(new AttributeModifier(ARMOR_BREAK_UUID, "spell_shatter", -8.0,
                AttributeModifier.Operation.ADDITION));
        target.getPersistentData().putInt("transcend_armor_break", 120);
    }

    private void dealSecondary(LivingEntity target, float rawDamage) {
        if (!(this.level() instanceof ServerLevel level)) return;
        LivingEntity caster = this.getOwner() instanceof LivingEntity living ? living : null;
        SpellDamageService.deal(level, caster, this, target, getElement(), getSpellTier(), rawDamage, true);
    }

    private void grantXpToOwner(int amount) {
        Entity owner = this.getOwner();
        if (!(owner instanceof Player player)) return;

        for (InteractionHand hand : InteractionHand.values()) {
            net.minecraft.world.item.ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof com.huige233.transcend.items.TranscendWand) {
                int selected = held.getOrCreateTag().getInt("selected_slot");
                com.huige233.transcend.items.TranscendWand.addSpellXp(held, selected, amount);
                return;
            }
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level().isClientSide) return;

        SpellCarrier carrier = getCarrier();
        List<SpellEffect> effects = getEffects();
        float damage = getElement().getBaseDamage() * getBasePower()
                * SpellConfigurationRules.amplifyMultiplier(effects);

        double radius = carrier.getAoeRadius();
        if (radius > 0) {
            Vec3 hitPos = result.getLocation();
            double r = radius;
            AABB area = new AABB(hitPos.x - r, hitPos.y - r, hitPos.z - r,
                    hitPos.x + r, hitPos.y + r, hitPos.z + r);

            Entity ownerEntity = this.getOwner();
            List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, area,
                    e -> canHitTarget(e, ownerEntity));

            for (LivingEntity target : targets) {
                applyElementDamage(target, getElement(), damage, ownerEntity instanceof Player p ? p : null);
            }
        }

        if (effects.contains(SpellEffect.EXPLOSION)) {
            applyVisualExplosion(result.getLocation(), damage);
        }

        if (this.level() instanceof ServerLevel sl) {
            spawnImpactBurst(sl, this.getX(), this.getY(), this.getZ(), getElement());
        }

        if (effects.contains(SpellEffect.SPLIT) && this.bounceCount < 3) {
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

    private void applyVisualExplosion(Vec3 pos, float rawDamage) {
        if (!(this.level() instanceof ServerLevel level)) return;
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z,
                8, 1.0, 0.6, 1.0, 0.05);
        level.playSound(null, net.minecraft.core.BlockPos.containing(pos),
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.9F, 1.1F);
        Entity owner = this.getOwner();
        level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(3.0),
                        e -> canHitTarget(e, owner))
                .stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(pos)))
                .limit(12)
                .forEach(e -> dealSecondary(e, rawDamage * 0.55F));
    }

    private void spawnElementFlair(ServerLevel level, SpellElement element, double px, double py, double pz) {
        spawnElementShaderFlair(level, element, px, py, pz);
    }

    private double rOff() {
        return (this.random.nextDouble() - 0.5) * 0.5;
    }

    private void spawnElementShaderFlair(ServerLevel level, SpellElement element, double px, double py, double pz) {
        float r = element.getParticleR();
        float g = element.getParticleG();
        float b = element.getParticleB();
        Vec3 center = new Vec3(px, py + 0.02, pz);
        int gate = switch (element) {
            case METAL, FIRE -> 3;
            case CHAOS -> 2;
            default -> 4;
        };
        if ((this.age % gate) != 0) return;

        String spellType = switch (element) {
            case METAL, FIRE -> "beam";
            case CHAOS -> "slash";
            default -> "beam";
        };
        ServerVisualBroadcaster.beam(level,
                center.add(rOff() * 0.35, 0.25 + rOff() * 0.2, rOff() * 0.35),
                center, r, g, b, 8, spellType);
    }

    private void spawnImpactBurst(ServerLevel level, double x, double y, double z, SpellElement element) {
        float r = element.getParticleR();
        float g = element.getParticleG();
        float b = element.getParticleB();
        float radius = switch (element) {
            case CHAOS -> 3.2F;
            case METAL, FIRE -> 2.9F;
            case WOOD -> 2.6F;
            default -> 2.4F;
        };
        Vec3 center = new Vec3(x, y + 0.08, z);

        ServerVisualBroadcaster.shockwave(level, center, radius, r, g, b, 18);
        if (element == SpellElement.METAL) {
            ServerVisualBroadcaster.beam(level,
                    new Vec3(x, y + 1.8, z),
                    new Vec3(x, y + 0.1, z),
                    1.0F, 1.0F, 0.55F, 12, "beam");
        }
    }
}
