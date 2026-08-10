package com.huige233.transcend.entity.boss;

import com.huige233.transcend.entity.SpellGuardian;
import com.huige233.transcend.entity.SpellWisp;
import com.huige233.transcend.init.ModEntities;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.spell.SpellElement;
import com.huige233.transcend.visual.ServerVisualBroadcaster;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import com.huige233.transcend.world.nexus.NexusBossModifier;

/** 超越化身首领。 */
public class TranscendenceAvatar extends PhaseDrivenBossBase {

    private static final EntityDataAccessor<Float> DATA_SHIELD =
            SynchedEntityData.defineId(TranscendenceAvatar.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MAX_SHIELD =
            SynchedEntityData.defineId(TranscendenceAvatar.class, EntityDataSerializers.FLOAT);

    private static final SpellElement[] ALL_ELEMENTS =
            java.util.Arrays.stream(SpellElement.values())
                    .filter(e -> e != SpellElement.CHAOS)
                    .toArray(SpellElement[]::new);
    private int elementIndex = 0;
    private int elementTimer = 0;
    private SpellElement[] activeElements = { SpellElement.FIRE };
    private int shieldRefreshTimer = 0;
    private boolean phaseTransitioning = false;
    private int absorbAnimTick = 0;
    private BossPhase pendingPhase = null;
    private final java.util.List<com.huige233.transcend.entity.SpellPillar> activePillars = new java.util.ArrayList<>();
    private boolean pillarShieldActive = false;

    private boolean isChasing = false;
    private int chaseTimer = 0;
    private double flightTargetHeight = 1.0;
    private int flightHeightChangeTimer = 0;
    private int chaseCooldown = 0;
    private int teleportTimer = 0;
    private int attributeLockTimer = 0;

    private com.huige233.transcend.spell.SpellCarrier activeCarrier = com.huige233.transcend.spell.SpellCarrier.ORB;
    private com.huige233.transcend.spell.SpellEffect activeEffect = null;
    private int comboTimer = 0;

    private boolean nexusModifiersApplied = false;
    private boolean nexusCanRapidTeleport = false;
    private boolean nexusCanSiphonAura = false;
    private boolean nexusIsReactionImmune = false;
    private boolean nexusHasExecuteSlash = false;
    private boolean nexusHasBurstCycle = false;
    private float nexusTeleportCdMult = 1.0F;

    private float nexusSpellIntervalMult = 1.0F;
    private float nexusSpellPowerMult = 1.0F;
    private int nexusResistOffset = 0;
    private boolean nexusStartPhase3 = false;
    private boolean nexusHasResurrection = false;
    private boolean nexusResurrectionUsed = false;
    private int resurrectionAnimTick = 0;
    private float nexusMinSpellPowerMult = 1.0F;

    private static final com.huige233.transcend.spell.SpellCarrier[] BOSS_CARRIERS = {
            com.huige233.transcend.spell.SpellCarrier.BEAM,
            com.huige233.transcend.spell.SpellCarrier.ARROW,
            com.huige233.transcend.spell.SpellCarrier.CHAIN,
            com.huige233.transcend.spell.SpellCarrier.DASH,
            com.huige233.transcend.spell.SpellCarrier.VORTEX,
            com.huige233.transcend.spell.SpellCarrier.TRAP,
    };
    private static final com.huige233.transcend.spell.SpellEffect[] ALL_EFFECTS;
    static {
        var effs = com.huige233.transcend.spell.SpellEffect.values();
        ALL_EFFECTS = new com.huige233.transcend.spell.SpellEffect[effs.length + 1];
        ALL_EFFECTS[0] = null;
        System.arraycopy(effs, 0, ALL_EFFECTS, 1, effs.length);
    }

    private void rollNewCombo() {
        activeCarrier = BOSS_CARRIERS[this.random.nextInt(BOSS_CARRIERS.length)];
        activeEffect = ALL_EFFECTS[this.random.nextInt(ALL_EFFECTS.length)];
        if (this.level() instanceof ServerLevel sl) {
            int phase = currentPhase.ordinal();
            sl.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.5F, 0.6F + phase * 0.2F);

            SpellElement el = currentElement != null ? currentElement : SpellElement.FIRE;
            ServerVisualBroadcaster.shieldRipple(sl,
                    new net.minecraft.world.phys.Vec3(this.getX(), this.getY() + 2.5, this.getZ()),
                    1.1F, el.getParticleR(), el.getParticleG(), el.getParticleB(), 14);
        }
    }

    public float getShieldHealth() { return this.entityData.get(DATA_SHIELD); }
    public float getMaxShield() { return this.entityData.get(DATA_MAX_SHIELD); }
    private void setShieldHealth(float v) { this.entityData.set(DATA_SHIELD, v); }
    private void setMaxShield(float v) { this.entityData.set(DATA_MAX_SHIELD, v); }
    private BossPhase getNextPhase() {
        return switch (currentPhase) {
            case PHASE_1 -> BossPhase.PHASE_2;
            case PHASE_2 -> BossPhase.PHASE_3;
            case PHASE_3 -> BossPhase.PHASE_4;
            case PHASE_4 -> null;
        };
    }

    private void beginPhaseTransition(BossPhase nextPhase) {
        if (nextPhase == null || phaseTransitioning || !this.isAlive()) return;
        this.bossSetHealthGuarded(Math.max(1.0F, this.getHealth()));
        this.phaseTransitioning = true;
        this.pendingPhase = nextPhase;
        this.absorbAnimTick = 80;
        this.getNavigation().stop();
        this.setDeltaMovement(0, 0, 0);
        this.setTarget(null);
        if (this.level() instanceof ServerLevel sl) {
            sl.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
                    net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.5F);
        }
    }

    private void ensurePhaseTransition() {
        if (phaseTransitioning || currentPhase == BossPhase.PHASE_4 || !this.isAlive()) return;
        if (this.getHealth() <= 1.0F) {
            beginPhaseTransition(getNextPhase());
        }
    }

    public TranscendenceAvatar(EntityType<? extends TranscendenceAvatar> type, Level level) {
        super(type, level, Component.translatable("entity.transcend.transcendence_avatar"), BossEvent.BossBarColor.WHITE);
        this.currentElement = SpellElement.FIRE;
        this.secondaryElement = SpellElement.WATER;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SHIELD, 375.0F);
        this.entityData.define(DATA_MAX_SHIELD, 375.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1500.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 15.0)
                .add(Attributes.ARMOR, 20.0)
                .add(Attributes.ARMOR_TOUGHNESS, 20.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public BossFaction getFaction() { return BossFaction.TRANSCEND; }

    @Override
    protected int getBaseSpellTier() { return 9; }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 32.0F));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                e -> !(e instanceof com.huige233.transcend.entity.SpellGuardian)
                        && !(e instanceof com.huige233.transcend.entity.SpellWisp)
                        && !(e instanceof AbstractTranscendBoss ob && !this.getFaction().isHostileTo(ob.getFaction()))));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        if (!nexusModifiersApplied && this.level() instanceof ServerLevel sl) {
            nexusModifiersApplied = true;
            var server = sl.getServer();
            nexusCanRapidTeleport  = NexusBossModifier.canBossRapidTeleport(server);
            nexusTeleportCdMult    = NexusBossModifier.getBossTeleportCooldownMult(server);
            nexusCanSiphonAura     = NexusBossModifier.canBossSiphonAura(server);
            nexusIsReactionImmune  = NexusBossModifier.isBossReactionImmune(server);
            nexusHasExecuteSlash   = NexusBossModifier.hasExecuteSlash(server);
            nexusHasBurstCycle     = NexusBossModifier.hasBossBurstCycle(server);
            nexusStartPhase3       = NexusBossModifier.shouldStartAtPhase3(server);
            nexusHasResurrection   = NexusBossModifier.shouldBossHaveResurrection(server);
            nexusMinSpellPowerMult = NexusBossModifier.getBossMinSpellPowerMultiplier(server);

            nexusSpellIntervalMult = nexusHasBurstCycle ? 0.7F : 1.0F;
            nexusSpellPowerMult    = nexusMinSpellPowerMult;

            if (nexusStartPhase3 && currentPhase == BossPhase.PHASE_1) {

                setPhase(BossPhase.PHASE_3);
            }
        }

        attributeLockTimer++;
        if (attributeLockTimer >= 20) {
            attributeLockTimer = 0;
            lockAttribute(Attributes.MAX_HEALTH, 1500.0);
            lockAttribute(Attributes.MOVEMENT_SPEED, 0.22);
            lockAttribute(Attributes.ARMOR, 20.0);
            lockAttribute(Attributes.ARMOR_TOUGHNESS, 20.0);
            lockAttribute(Attributes.KNOCKBACK_RESISTANCE, 1.0);
            lockAttribute(Attributes.ATTACK_DAMAGE, 15.0);
        }

        if (pillarShieldActive) {
            activePillars.removeIf(p -> !p.isAlive());
            if (activePillars.isEmpty()) {
                pillarShieldActive = false;
                if (this.level() instanceof ServerLevel sl) {
                    sl.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE,
                            net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 1.0F);
                    ServerVisualBroadcaster.shockwave(sl,
                            new net.minecraft.world.phys.Vec3(this.getX(), this.getY() + 0.1, this.getZ()),
                            10.0F, 1.0F, 0.3F, 0.3F, 30);
                }
            }
        }

        if (!phaseTransitioning && currentPhase != BossPhase.PHASE_1) {
            teleportTimer++;
            int teleportInterval = switch (currentPhase) {
                case PHASE_2 -> 200;
                case PHASE_3 -> 120;
                case PHASE_4 -> 80;
                default -> Integer.MAX_VALUE;
            };
            if (teleportTimer >= teleportInterval && getTarget() != null) {
                teleportTimer = 0;
                teleportNearTarget(getTarget());
            }
        }

        if (!phaseTransitioning && currentPhase != BossPhase.PHASE_4
                && this.isAlive() && this.getHealth() <= 1.0F) {
            BossPhase next = switch (currentPhase) {
                case PHASE_1 -> BossPhase.PHASE_2;
                case PHASE_2 -> BossPhase.PHASE_3;
                case PHASE_3 -> BossPhase.PHASE_4;
                case PHASE_4 -> null;
            };
            if (next != null) {
                this.bossSetHealthGuarded(1.0F);
                this.phaseTransitioning = true;
                this.pendingPhase = next;
                this.absorbAnimTick = 80;
                if (this.level() instanceof ServerLevel sl)
                    sl.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
                            net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.5F);
            }
        }

        if (!phaseTransitioning && currentPhase != BossPhase.PHASE_4
                && this.isAlive() && this.getHealth() < 1.0F) {
            this.bossSetHealthGuarded(1.0F);
        }

        if (!phaseTransitioning) {
            forceHeal(0.4F);
        }

        if (!phaseTransitioning && this.tickCount % 40 == 0) {
            double crystalRange = 48.0;
            net.minecraft.world.phys.AABB crystalSearch = this.getBoundingBox().inflate(crystalRange);
            for (net.minecraft.world.entity.boss.enderdragon.EndCrystal crystal :
                    this.level().getEntitiesOfClass(net.minecraft.world.entity.boss.enderdragon.EndCrystal.class,
                            crystalSearch, net.minecraft.world.entity.Entity::isAlive)) {

                crystal.hurt(this.damageSources().mobAttack(this), 1.0F);
            }
        }

        if (!phaseTransitioning) {
            int resistLevel = Math.max(0, currentPhase.ordinal() + nexusResistOffset);
            if (!this.hasEffect(MobEffects.DAMAGE_RESISTANCE)
                    || this.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() != resistLevel) {
                this.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, resistLevel, false, false));
            }
        }

        if (!phaseTransitioning && this.tickCount % 20 == 0) {
            double range = getTrackingRange();
            net.minecraft.world.phys.AABB search = this.getBoundingBox().inflate(range);
            LivingEntity bestBoss = null;
            double bestDist = Double.MAX_VALUE;
            for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, search, e -> e.isAlive() && e != this)) {
                boolean isBoss = e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                        || e instanceof net.minecraft.world.entity.boss.wither.WitherBoss
                        || (e instanceof AbstractTranscendBoss ob && this.getFaction().isHostileTo(ob.getFaction()));
                if (isBoss) {
                    double d = e.distanceToSqr(this);
                    if (d < bestDist) {
                        bestDist = d;
                        bestBoss = e;
                    }
                }
            }
            if (bestBoss != null) {
                this.setTarget(bestBoss);
            }
        }

        if (!phaseTransitioning) {
            LivingEntity tgt = this.getTarget();

            if (tgt != null && chaseCooldown <= 0) {
                double dx = tgt.getX() - this.getX();
                double dz = tgt.getZ() - this.getZ();
                double dist2D = Math.sqrt(dx * dx + dz * dz);
                if (!isChasing && dist2D > 14.0) {
                    isChasing = true;
                    chaseTimer = 0;
                    double groundY = findGroundY();
                    flightTargetHeight = (tgt.getY() - groundY) + 2.0 + this.random.nextDouble() * 3.0;
                    flightTargetHeight = Math.max(2.0, flightTargetHeight);
                    flightHeightChangeTimer = 60 + this.random.nextInt(60);
                }
            }

            if (isChasing) {
                chaseTimer++;
                flightHeightChangeTimer--;

                if (flightHeightChangeTimer <= 0) {
                    double groundY = findGroundY();
                    double baseH = tgt != null ? (tgt.getY() - groundY) : 0;
                    flightTargetHeight = baseH + 1.5 + this.random.nextDouble() * 5.0;
                    flightTargetHeight = Math.max(2.0, flightTargetHeight);
                    flightHeightChangeTimer = 60 + this.random.nextInt(80);
                }

                if (tgt != null) {
                    double dx = tgt.getX() - this.getX();
                    double dz = tgt.getZ() - this.getZ();
                    double dist2D = Math.sqrt(dx * dx + dz * dz);

                    if (dist2D > 0.5) {
                        double speed = Math.min(0.12, dist2D * 0.015);
                        this.setDeltaMovement(
                                this.getDeltaMovement().x + dx / dist2D * speed,
                                this.getDeltaMovement().y,
                                this.getDeltaMovement().z + dz / dist2D * speed);
                    }

                    if (dist2D <= 6.0) {
                        isChasing = false;
                        chaseCooldown = 100;
                        flightTargetHeight = 1.0;
                    }
                }

                if (chaseTimer > 400) {
                    isChasing = false;
                    chaseCooldown = 60;
                    flightTargetHeight = 1.0;
                }

                this.setDeltaMovement(
                        this.getDeltaMovement().x * 0.90,
                        this.getDeltaMovement().y,
                        this.getDeltaMovement().z * 0.90);

            } else {
                if (chaseCooldown > 0) chaseCooldown--;

                if (tgt != null) {
                    double dx = tgt.getX() - this.getX();
                    double dz = tgt.getZ() - this.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    double preferredDist = 7.0;
                    if (dist > 0.1) {
                        double speed = dist > preferredDist + 1 ? 0.04 : (dist < preferredDist - 1 ? -0.03 : 0);
                        this.setDeltaMovement(
                                this.getDeltaMovement().x + dx / dist * speed,
                                this.getDeltaMovement().y,
                                this.getDeltaMovement().z + dz / dist * speed);
                    }
                }
                this.setDeltaMovement(
                        this.getDeltaMovement().x * 0.85,
                        this.getDeltaMovement().y,
                        this.getDeltaMovement().z * 0.85);
            }

            double groundY = findGroundY();
            double targetAbsY = groundY + flightTargetHeight;
            double dy = targetAbsY - this.getY();
            double ySpeed = isChasing ? dy * 0.12 : dy * 0.1;
            this.setDeltaMovement(this.getDeltaMovement().x, ySpeed, this.getDeltaMovement().z);
        }

        if (phaseTransitioning && absorbAnimTick > 0) {
            absorbAnimTick--;
            this.getNavigation().stop();
            this.setTarget(null);
            this.setDeltaMovement(0, absorbAnimTick > 40 ? 0.06 : -0.04, 0);

            if (absorbAnimTick <= 40) {
                forceHeal(this.getMaxHealth() / 40.0F);
            }

            if (this.level() instanceof ServerLevel sl) {
                int elementIdx = (80 - absorbAnimTick) % ALL_ELEMENTS.length;
                SpellElement absorbEl = ALL_ELEMENTS[elementIdx];
                float t = (80 - absorbAnimTick) / 80.0F;
                float spiralRadius = Math.max(0.8F, 4.0F * (1.0F - t));
                ServerVisualBroadcaster.circle(sl,
                        new net.minecraft.world.phys.Vec3(this.getX(), this.getY() + 1.2 + t * 0.9, this.getZ()),
                        spiralRadius,
                        absorbEl.getParticleR(), absorbEl.getParticleG(), absorbEl.getParticleB(),
                        12, 24, "pentagram");
                ServerVisualBroadcaster.beam(sl,
                        new net.minecraft.world.phys.Vec3(this.getX() + Math.cos(absorbAnimTick * 0.22) * spiralRadius,
                                this.getY() + 1.6 + Math.sin(absorbAnimTick * 0.18) * 0.4,
                                this.getZ() + Math.sin(absorbAnimTick * 0.22) * spiralRadius),
                        new net.minecraft.world.phys.Vec3(this.getX(), this.getY() + 1.5, this.getZ()),
                        absorbEl.getParticleR(), absorbEl.getParticleG(), absorbEl.getParticleB(),
                        10, "beam");

                if (absorbAnimTick % 10 == 0) {
                    ServerVisualBroadcaster.shieldRipple(sl,
                            new net.minecraft.world.phys.Vec3(this.getX(), this.getY() + 1.5, this.getZ()),
                            1.2F + t * 1.2F, 1.0F, 0.95F, 0.75F, 12);
                }
            }

            if (absorbAnimTick <= 0) {
                phaseTransitioning = false;
                if (pendingPhase != null) {
                    setPhase(pendingPhase);
                    pendingPhase = null;
                }
            }
            return;
        }

        if (resurrectionAnimTick > 0) {
            resurrectionAnimTick--;
            this.getNavigation().stop();
            this.setTarget(null);
            this.setDeltaMovement(0, 0.08, 0);

            forceHeal(this.getMaxHealth() / 60.0F);

            if (this.level() instanceof ServerLevel sl) {
                float t = (60 - resurrectionAnimTick) / 60.0F;

                float r1 = 0.8F + 0.2F * (float) Math.sin(resurrectionAnimTick * 0.3);
                float g1 = 0.1F;
                float b1 = 0.15F + 0.2F * (float) Math.cos(resurrectionAnimTick * 0.25);

                ServerVisualBroadcaster.circle(sl,
                        new net.minecraft.world.phys.Vec3(this.getX(), this.getY() + 0.5 + t * 2.0, this.getZ()),
                        3.0F - t * 1.5F, r1, g1, b1, 16, 28, "pentagram");

                if (resurrectionAnimTick % 4 == 0) {
                    ServerVisualBroadcaster.shieldRipple(sl,
                            new net.minecraft.world.phys.Vec3(this.getX(), this.getY() + 1.5, this.getZ()),
                            1.6F + t * 1.4F, r1, g1, b1, 18);
                }

                if (resurrectionAnimTick % 8 == 0) {
                    double angle = resurrectionAnimTick * 0.5;
                    ServerVisualBroadcaster.beam(sl,
                            new net.minecraft.world.phys.Vec3(
                                    this.getX() + Math.cos(angle) * (3.0 - t * 2.0),
                                    this.getY() + 3.0,
                                    this.getZ() + Math.sin(angle) * (3.0 - t * 2.0)),
                            new net.minecraft.world.phys.Vec3(this.getX(), this.getY() + 1.5, this.getZ()),
                            1.0F, 0.2F, 0.2F, 14, "beam");
                }
            }

            if (resurrectionAnimTick <= 0) {

                this.setHealth(this.getMaxHealth());
                setShieldHealth(getMaxShield());
                this.phaseTimer = 0;
                this.spellCooldown = 40;

                if (this.level() instanceof ServerLevel sl) {

                    ServerVisualBroadcaster.shockwave(sl,
                            new net.minecraft.world.phys.Vec3(this.getX(), this.getY() + 0.1, this.getZ()),
                            18.0F, 1.0F, 0.15F, 0.15F, 50);

                    for (net.minecraft.server.level.ServerPlayer sp :
                            sl.getPlayers(p -> p.distanceTo(this) < 32.0F)) {
                        double kx = sp.getX() - this.getX();
                        double kz = sp.getZ() - this.getZ();
                        double dist = Math.sqrt(kx * kx + kz * kz);
                        if (dist > 0.1) {
                            float strength = (float) (1.2 * (1.0 - Math.min(1.0, dist / 32.0)));
                            sp.knockback(strength, -kx / dist, -kz / dist);
                            sp.hurtMarked = true;
                        }
                        sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                                Component.translatable("title.transcend.resurrection")
                                        .withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD)));
                        sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(5, 40, 10));
                    }

                    sl.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.WITHER_SPAWN,
                            net.minecraft.sounds.SoundSource.HOSTILE, 2.5F, 0.5F);
                    sl.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL,
                            net.minecraft.sounds.SoundSource.HOSTILE, 2.5F, 0.4F);
                }
            }
            return;
        }

        elementTimer++;

        comboTimer++;
        int comboInterval = switch (currentPhase) {
            case PHASE_1 -> 400;
            case PHASE_2 -> 300;
            case PHASE_3 -> 200;
            case PHASE_4 -> 100;
        };
        if (comboTimer >= comboInterval) {
            comboTimer = 0;
            rollNewCombo();
        }

        boolean lowHp = this.getHealth() < this.getMaxHealth() * 0.35F;

        boolean useLifesteal = lowHp && this.random.nextFloat() < 0.4F;

        switch (currentPhase) {
            case PHASE_1 -> {
                if (elementTimer >= 600) {
                    rotateElement();
                    elementTimer = 0;
                }
                if (spellCooldown <= 0 && getTarget() != null) {
                    fireSpellAtTarget(useLifesteal ? SpellElement.WOOD : activeElements[0], 2.0F);

                    if (phaseTimer % 3 == 0) {
                        fireSpellAtTarget(activeElements[0], 1.5F);
                    }
                    spellCooldown = Math.max(1, (int)(4 * nexusSpellIntervalMult));
                    if (useLifesteal) forceHeal(3.0F);
                }
                if (phaseTimer % 60 == 0 && phaseTimer > 0 && getTarget() != null) {
                    fireSpellRain(activeElements[0], 1.5F, getTarget().getX(), getTarget().getZ());
                }

                if (phaseTimer % 150 == 0 && phaseTimer > 0) {
                    fireBeamSweep(activeElements[0], 6.0F);
                }
                if (phaseTimer % 400 == 0) {
                    deployBossWarningEffect();
                }
            }
            case PHASE_2 -> {
                if (elementTimer >= 400) {
                    rotateElement();
                    elementTimer = 0;
                }
                if (spellCooldown <= 0 && getTarget() != null) {

                    for (int i = 0; i < 3; i++) {
                        SpellElement el = useLifesteal && i == 0 ? SpellElement.WOOD : activeElements[i % activeElements.length];
                        fireSpellAtTarget(el, 2.5F - i * 0.3F);
                    }
                    spellCooldown = Math.max(1, (int)(3 * nexusSpellIntervalMult));
                    if (useLifesteal) forceHeal(5.0F);
                }

                if (phaseTimer % 80 == 0 && phaseTimer > 0) {
                    fireBeamSweep(activeElements[0], 8.0F);
                }

                if (phaseTimer % 60 == 0 && phaseTimer > 0) {
                    fireRuneVolley(activeElements[this.random.nextInt(activeElements.length)], 1.8F, 4);
                }
                if (phaseTimer % 150 == 0 && phaseTimer > 0) {
                    fireSpellRain(activeElements[this.random.nextInt(activeElements.length)], 2.0F,
                            getTarget() != null ? getTarget().getX() : this.getX(),
                            getTarget() != null ? getTarget().getZ() : this.getZ());
                }
                if (phaseTimer % 100 == 0 && phaseTimer > 0) {
                    groundSlam(activeElements[0], 10.0F, 6.0);
                }
                if (phaseTimer % 400 == 0) {
                    deployBossWarningEffect();
                }
                if (phaseTimer % 250 == 0 && phaseTimer > 0) {
                    elementalStorm();
                }
            }
            case PHASE_3 -> {
                if (elementTimer >= 60) {
                    rotateElement();
                    elementTimer = 0;
                }
                if (spellCooldown <= 0 && getTarget() != null) {

                    for (int i = 0; i < 5; i++) {
                        SpellElement el = useLifesteal && i == 0 ? SpellElement.WOOD : activeElements[i % activeElements.length];
                        fireSpellAtTarget(el, 3.0F - i * 0.2F);
                    }
                    spellCooldown = Math.max(1, (int)(2 * nexusSpellIntervalMult));
                    if (useLifesteal) forceHeal(8.0F);
                }

                if (phaseTimer % 50 == 0 && phaseTimer > 0) {
                    fireBeamSweep(activeElements[0], 12.0F);
                }

                if (phaseTimer % 35 == 0 && phaseTimer > 0) {
                    fireRuneVolley(activeElements[this.random.nextInt(activeElements.length)], 2.5F, 6);
                }
                if (phaseTimer % 30 == 0 && phaseTimer > 0) {
                    fireSpiralBarrage(activeElements[this.random.nextInt(activeElements.length)], 2.5F, 8, 0.65F);
                }
                if (phaseTimer % 300 == 0 && phaseTimer > 0 && getTarget() != null) {
                    voidPrison(getTarget());
                }
                if (phaseTimer % 200 == 0 && phaseTimer > 0) {
                    elementalStorm();
                }
                if (phaseTimer % 220 == 0 && phaseTimer > 0) {
                    summonMeteorCataclysm(false);
                }
            }
            case PHASE_4 -> {
                if (spellCooldown <= 0 && getTarget() != null) {

                    for (int i = 0; i < 8; i++) {
                        SpellElement el = useLifesteal && i == 0 ? SpellElement.WOOD
                                : activeElements[i % activeElements.length];
                        fireSpellAtTarget(el, 4.0F - i * 0.15F);
                    }
                    spellCooldown = Math.max(1, (int)(2 * nexusSpellIntervalMult));
                    if (useLifesteal) forceHeal(12.0F);
                }

                if (phaseTimer % 35 == 0 && phaseTimer > 0) {
                    fireBeamSweep(activeElements[this.random.nextInt(activeElements.length)], 15.0F);
                }

                if (phaseTimer % 25 == 0 && phaseTimer > 0) {
                    fireRuneVolley(activeElements[this.random.nextInt(activeElements.length)], 3.5F, 10);
                }
                if (phaseTimer % 20 == 0 && phaseTimer > 0) {
                    fireSpiralBarrage(activeElements[this.random.nextInt(activeElements.length)], 3.0F, 12, 0.8F);
                }
                if (phaseTimer % 60 == 0 && phaseTimer > 0) {
                    groundSlam(activeElements[this.random.nextInt(activeElements.length)], 15.0F, 8.0);
                }
                if (phaseTimer % 80 == 0) {
                    novaBlast(activeElements[this.random.nextInt(activeElements.length)], 3.5F);
                }
                if (phaseTimer % 150 == 0 && phaseTimer > 0 && getTarget() != null) {
                    voidPrison(getTarget());
                }
                if (phaseTimer % 120 == 0 && phaseTimer > 0) {
                    elementalStorm();
                }
                if (phaseTimer % 140 == 0 && phaseTimer > 0) {
                    summonMeteorCataclysm(true);
                }
                getNearbyPlayers(16).forEach(p -> {
                    p.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 1, false, true));
                    p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, true));
                });
            }
        }
    }

    @Override
    protected void checkPhaseTransition() {
        ensurePhaseTransition();
    }

    @Override
    protected void setPhase(BossPhase phase) {
        super.setPhase(phase);

        clearTransitionTick();
    }

    @Override
    protected void tickArenaAura() {

    }

    @Override
    protected void onPhaseChange(BossPhase newPhase) {
        setMaxShield(getMaxShield() + this.getMaxHealth() * 0.25F);
        setShieldHealth(getMaxShield());
        shieldRefreshTimer = 0;

        if (!(this.level() instanceof ServerLevel sl)) return;

        int guardians = switch (newPhase) {
            case PHASE_1 -> 0;
            case PHASE_2 -> 3;
            case PHASE_3 -> 4;
            case PHASE_4 -> 6;
        };
        int wisps = switch (newPhase) {
            case PHASE_1 -> 0;
            case PHASE_2 -> 3;
            case PHASE_3 -> 4;
            case PHASE_4 -> 5;
        };

        for (int i = 0; i < guardians; i++) {
            SpellGuardian g = new SpellGuardian(ModEntities.SPELL_GUARDIAN.get(), sl);
            double a = Math.PI * 2 * i / guardians;
            g.setPos(this.getX() + Math.cos(a) * 4, this.getY(), this.getZ() + Math.sin(a) * 4);
            g.setOwner(this);
            sl.addFreshEntity(g);
        }
        for (int i = 0; i < wisps; i++) {
            SpellWisp w = new SpellWisp(ModEntities.SPELL_WISP.get(), sl);
            double a = Math.PI * 2 * i / wisps + Math.PI / wisps;
            w.setPos(this.getX() + Math.cos(a) * 3, this.getY() + 2, this.getZ() + Math.sin(a) * 3);
            w.setOwner(this);
            sl.addFreshEntity(w);
        }

        if (newPhase == BossPhase.PHASE_4) {
            activePillars.clear();
            java.util.List<SpellElement> pool = new java.util.ArrayList<>(java.util.Arrays.asList(ALL_ELEMENTS));
            java.util.Collections.shuffle(pool, new java.util.Random(this.random.nextLong()));
            for (int i = 0; i < 6; i++) {
                SpellElement pillarElement = pool.get(i % pool.size());
                com.huige233.transcend.entity.SpellPillar pillar =
                        new com.huige233.transcend.entity.SpellPillar(ModEntities.SPELL_PILLAR.get(), sl);
                double angle = Math.PI * 2 * i / 6;
                pillar.setPos(this.getX() + Math.cos(angle) * 6, this.getY(), this.getZ() + Math.sin(angle) * 6);
                pillar.setRequiredElement(pillarElement);
                pillar.setOwner(this);
                sl.addFreshEntity(pillar);
                activePillars.add(pillar);
            }
            pillarShieldActive = true;
            sl.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
                    net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.4F);
        }
    }

    @Override
    protected float getDamageCap() {
        return switch (currentPhase) {
            case PHASE_1 -> this.getMaxHealth() * 0.20F;
            case PHASE_2 -> this.getMaxHealth() * 0.10F;
            case PHASE_3, PHASE_4 -> this.getMaxHealth() * 0.05F;
        };
    }

    @Override
    protected float getSpellPowerMultiplier() {
        float base = super.getSpellPowerMultiplier();

        base = Math.max(base, nexusMinSpellPowerMult);

        base *= nexusSpellPowerMult;
        return base;
    }

    @Override
    protected float getElementDamageMultiplier(SpellElement element) {
        if (currentPhase == BossPhase.PHASE_3 || currentPhase == BossPhase.PHASE_4) {
            return 1.0F;
        }
        if (currentPhase == BossPhase.PHASE_1) {
            if (element == currentElement) return 0.0F;
            SpellElement counter = getCounterForAvatar(currentElement);
            if (element == counter) return 3.0F;
        }
        return 1.0F;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (this instanceof com.huige233.transcend.mixinitf.ITranscendMarked m && m.transcend$isMarked()) {
            return super.hurt(source, amount);
        }
        if (phaseTransitioning) return false;
        if (resurrectionAnimTick > 0) return false;
        if (pillarShieldActive) return false;
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) return false;

        boolean isExplosion = source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION);
        boolean isMagic = source.getDirectEntity() instanceof com.huige233.transcend.spell.SpellProjectile
                || source.getMsgId().contains("indirectMagic") || source.getMsgId().contains("magic");
        boolean isPhysical = !isMagic && !isExplosion
                && (source.getDirectEntity() instanceof LivingEntity || source.getMsgId().contains("player")
                || source.getMsgId().contains("mob") || source.getMsgId().contains("attack"));
        boolean isCoreDamage = isExplosion || isMagic || isPhysical;

        switch (currentPhase) {
            case PHASE_2 -> {
                if (!isCoreDamage) {
                    amount *= 0.20F;
                }
            }
            case PHASE_3, PHASE_4 -> {
                if (!isCoreDamage) return false;
            }
            default -> {}
        }

        float reduction = isMagic
                ? switch (currentPhase) { case PHASE_1->0.15F; case PHASE_2->0.25F; case PHASE_3->0.35F; case PHASE_4->0.75F; }
                : switch (currentPhase) { case PHASE_1->0.10F; case PHASE_2->0.15F; case PHASE_3->0.20F; case PHASE_4->0.25F; };
        amount *= (1.0F - reduction);

        if (getShieldHealth() > 0) {
            amount *= 0.65F;
        }

        boolean result = super.hurt(source, amount);
        if (this.isAlive()) {
            ensurePhaseTransition();
        }
        return result;
    }

    protected void actuallyHurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (this instanceof com.huige233.transcend.mixinitf.ITranscendMarked m && m.transcend$isMarked()) {
            super.actuallyHurt(source, amount);
            return;
        }

        float sh = getShieldHealth();
        if (sh > 0 && amount > 0) {
            float absorbed = Math.min(sh, amount);
            setShieldHealth(sh - absorbed);
            amount -= absorbed;
            if (this.level() instanceof ServerLevel sl) {
                float intensity = Math.min(1.0F, absorbed / 20.0F);
                ServerVisualBroadcaster.shieldRipple(sl,
                        new net.minecraft.world.phys.Vec3(this.getX(), this.getY() + 1.5, this.getZ()),
                        1.4F + intensity * 0.9F, 0.3F, 0.6F, 1.0F, 18);
                ServerVisualBroadcaster.shockwave(sl,
                        new net.minecraft.world.phys.Vec3(this.getX(), this.getY() + 0.2, this.getZ()),
                        1.8F + intensity * 2.2F, 0.35F, 0.7F, 1.0F, 16);
                if (sh - absorbed <= 0)
                    sl.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.SHIELD_BREAK,
                            net.minecraft.sounds.SoundSource.HOSTILE, 1.5F, 0.8F);
            }
            if (amount <= 0) return;
        }
        super.actuallyHurt(source, amount);

        if (!level().isClientSide && currentPhase == BossPhase.PHASE_4
                && nexusHasResurrection && !nexusResurrectionUsed
                && this.getHealth() <= 0.0F) {
            nexusResurrectionUsed = true;
            this.bossSetHealthGuarded(1.0F);
            this.setHealth(1.0F);
            resurrectionAnimTick = 60;
            this.getActiveEffects().stream()
                    .filter(e -> !e.getEffect().isBeneficial())
                    .map(MobEffectInstance::getEffect)
                    .toList()
                    .forEach(this::removeEffect);
            if (this.level() instanceof ServerLevel sl) {
                sl.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.TOTEM_USE,
                        net.minecraft.sounds.SoundSource.HOSTILE, 2.5F, 0.6F);
                sl.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL,
                        net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.3F);
                ServerVisualBroadcaster.shockwave(sl,
                        new net.minecraft.world.phys.Vec3(this.getX(), this.getY() + 0.1, this.getZ()),
                        12.0F, 1.0F, 0.1F, 0.1F, 35);
            }
        }

    }

    @Override
    protected void fireSpellAtTarget(SpellElement element, float power) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        LivingEntity target = this.getTarget();
        if (target == null) return;

        power *= nexusSpellPowerMult;

        boolean targetIsBoss = target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                || target instanceof net.minecraft.world.entity.boss.wither.WitherBoss
                || (target instanceof AbstractTranscendBoss ob && this.getFaction().isHostileTo(ob.getFaction()));
        com.huige233.transcend.spell.SpellCarrier carrier = targetIsBoss && this.random.nextFloat() < 0.35F
                ? com.huige233.transcend.spell.SpellCarrier.BEAM
                : activeCarrier;

        com.huige233.transcend.spell.SpellProjectile proj =
                new com.huige233.transcend.spell.SpellProjectile(
                        this.level(), this, carrier, element, activeEffect, power,
                        currentPhase.ordinal() + 1);

        double dx = target.getX() - this.getX();
        double dy = target.getEyeY() - this.getEyeY();
        double dz = target.getZ() - this.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > 0) {

            float speed = 0.8F + currentPhase.ordinal() * 0.15F;
            proj.setDeltaMovement(dx / dist * speed, dy / dist * speed, dz / dist * speed);
        }
        proj.setPos(this.getX(), this.getEyeY(), this.getZ());
        sl.addFreshEntity(proj);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect().isBeneficial();
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {

        if (currentPhase == BossPhase.PHASE_4
                && nexusHasResurrection && !nexusResurrectionUsed) {
            nexusResurrectionUsed = true;
            this.bossSetHealthGuarded(1.0F);
            this.setHealth(1.0F);
            resurrectionAnimTick = 60;

            if (this.level() instanceof ServerLevel sl) {
                sl.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.TOTEM_USE,
                        net.minecraft.sounds.SoundSource.HOSTILE, 2.5F, 0.6F);
                ServerVisualBroadcaster.shockwave(sl,
                        new net.minecraft.world.phys.Vec3(this.getX(), this.getY() + 0.1, this.getZ()),
                        12.0F, 1.0F, 0.1F, 0.1F, 35);
            }
            return;
        }
        super.die(source);
    }

    @Override
    protected void spawnAmbientParticles() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        SpellElement el = currentElement;
        if (el == null) return;
        BossParticleModel.renderAvatarBody(
                sl,
                this.getX(), this.getY(), this.getZ(),
                el.getParticleR(), el.getParticleG(), el.getParticleB(),
                secondaryElement != null ? secondaryElement.getParticleR() : el.getParticleR() * 0.7F,
                secondaryElement != null ? secondaryElement.getParticleG() : el.getParticleG() * 0.7F,
                secondaryElement != null ? secondaryElement.getParticleB() : el.getParticleB() * 0.7F,
                this.tickCount,
                this.currentPhase == BossPhase.PHASE_4
        );
    }

    private void rotateElement() {
        elementIndex = (elementIndex + 1) % ALL_ELEMENTS.length;
        currentElement = ALL_ELEMENTS[elementIndex];
        int count = switch (currentPhase) {
            case PHASE_1 -> 1;
            case PHASE_2 -> 2;
            case PHASE_3, PHASE_4 -> 3;
        };
        activeElements = new SpellElement[count];
        for (int i = 0; i < count; i++) {
            activeElements[i] = ALL_ELEMENTS[(elementIndex + i) % ALL_ELEMENTS.length];
        }
        secondaryElement = ALL_ELEMENTS[(elementIndex + 1) % ALL_ELEMENTS.length];
    }

    private void forceHeal(float amount) {
        float currentHp = this.getHealth();
        float maxHp = this.getMaxHealth();
        float actualHeal = Math.min(amount, maxHp - currentHp);
        if (actualHeal > 0) {
            this.bossSetHealthGuarded(currentHp + actualHeal);
        }
        float overflow = amount - actualHeal;
        if (overflow > 0) {

            float sh = getShieldHealth();
            float maxSh = getMaxShield();
            if (sh < maxSh) {
                setShieldHealth(Math.min(maxSh, sh + overflow * 2.0F));
            }
        }
    }

    private void lockAttribute(net.minecraft.world.entity.ai.attributes.Attribute attr, double baseValue) {
        AttributeInstance inst = this.getAttribute(attr);
        if (inst == null) return;
        inst.setBaseValue(baseValue);
        inst.getModifiers().forEach(mod -> inst.removeModifier(mod.getId()));
    }

    private void teleportNearTarget(LivingEntity target) {
        double angle = this.random.nextDouble() * Math.PI * 2;
        double dist = 3.0 + this.random.nextDouble() * 2.0;
        double tx = target.getX() + Math.cos(angle) * dist;
        double tz = target.getZ() + Math.sin(angle) * dist;
        if (this.level() instanceof ServerLevel sl) {
            SpellElement el = currentElement != null ? currentElement : SpellElement.FIRE;

            ServerVisualBroadcaster.circle(sl,
                    new net.minecraft.world.phys.Vec3(this.getX(), this.getY() + 0.1, this.getZ()),
                    1.8F, el.getParticleR(), el.getParticleG(), el.getParticleB(), 60, 22, "pentagram");
            ServerVisualBroadcaster.shieldRipple(sl,
                    new net.minecraft.world.phys.Vec3(this.getX(), this.getY() + 1.0, this.getZ()),
                    1.4F, el.getParticleR(), el.getParticleG(), el.getParticleB(), 20);
            sl.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.5F, 0.7F);
        }
        this.bossTeleportTo(tx, target.getY(), tz);
        if (this.level() instanceof ServerLevel sl) {
            SpellElement el = currentElement != null ? currentElement : SpellElement.FIRE;

            ServerVisualBroadcaster.circle(sl,
                    new net.minecraft.world.phys.Vec3(this.getX(), this.getY() + 0.1, this.getZ()),
                    2.0F, el.getParticleR(), el.getParticleG(), el.getParticleB(), 80, 24, "hexagram");
            ServerVisualBroadcaster.shockwave(sl,
                    new net.minecraft.world.phys.Vec3(this.getX(), this.getY() + 0.2, this.getZ()),
                    2.4F, el.getParticleR(), el.getParticleG(), el.getParticleB(), 16);
        }
    }

    @Override
    protected double getTrackingRange() {
        return 48.0;
    }

    @Override
    protected boolean enableBossAutoHeal() {
        return false;
    }

    @Override
    protected boolean enableBossHealthLock() {
        return true;
    }

    @Override
    protected boolean hasNextPhase() {
        return currentPhase != BossPhase.PHASE_4;
    }

    private SpellElement getCounterForAvatar(SpellElement element) {
        return switch (element) {
            case METAL -> SpellElement.FIRE;
            case WOOD -> SpellElement.METAL;
            case WATER -> SpellElement.EARTH;
            case FIRE -> SpellElement.WATER;
            case EARTH -> SpellElement.WOOD;
            case CHAOS -> SpellElement.CHAOS;
        };
    }

    private double findGroundY() {
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos(
                (int) Math.floor(this.getX()), (int) Math.floor(this.getY()), (int) Math.floor(this.getZ()));
        for (int i = 0; i < 64; i++) {
            if (probe.getY() <= this.level().getMinBuildHeight()) break;
            if (!this.level().getBlockState(probe).isAir()) {
                return probe.getY() + 1;
            }
            probe.setY(probe.getY() - 1);
        }
        return this.getY();
    }

    private void novaBlast(SpellElement element, float power) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        for (int i = 0; i < 16; i++) {
            double angle = Math.PI * 2 * i / 16;
            com.huige233.transcend.spell.SpellProjectile proj =
                    new com.huige233.transcend.spell.SpellProjectile(
                            this.level(), this, activeCarrier,
                            element, activeEffect, power, currentPhase.ordinal() + 1);
            proj.setPos(this.getX(), this.getEyeY(), this.getZ());
            proj.setDeltaMovement(Math.cos(angle) * 0.6, 0.05, Math.sin(angle) * 0.6);
            sl.addFreshEntity(proj);
        }
    }

    private void elementalStorm() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        LivingEntity target = this.getTarget();
        double cx = target != null ? target.getX() : this.getX();
        double cz = target != null ? target.getZ() : this.getZ();
        ServerVisualBroadcaster.circle(sl,
                new net.minecraft.world.phys.Vec3(cx, this.getY(), cz),
                10.0F, currentElement.getParticleR(), currentElement.getParticleG(), currentElement.getParticleB(),
                60, 32, "hexagram");
        for (int i = 0; i < 12; i++) {
            SpellElement el = ALL_ELEMENTS[this.random.nextInt(ALL_ELEMENTS.length)];
            double ox = (this.random.nextDouble() - 0.5) * 20;
            double oz = (this.random.nextDouble() - 0.5) * 20;
            com.huige233.transcend.spell.SpellProjectile proj =
                    new com.huige233.transcend.spell.SpellProjectile(
                            this.level(), this, activeCarrier, el, activeEffect, 2.5F,
                            currentPhase.ordinal() + 1);
            proj.setPos(cx + ox, this.getY() + 15, cz + oz);
            proj.setDeltaMovement((this.random.nextDouble() - 0.5) * 0.2, -0.9, (this.random.nextDouble() - 0.5) * 0.2);
            sl.addFreshEntity(proj);
        }
        sl.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.5F, 1.2F);
    }

    private void summonMeteorCataclysm(boolean empowered) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        LivingEntity target = this.getTarget();
        if (target == null) return;

        net.minecraft.world.phys.Vec3 center = new net.minecraft.world.phys.Vec3(
                target.getX(), target.getY(), target.getZ());
        float spellPower = Math.min(4.0F, getSpellPowerMultiplier());
        float powerMult = (empowered ? 2.2F : 1.8F) * spellPower;
        float radiusMult = empowered ? 1.45F : 1.25F;
        int specialLevel = empowered ? 4 : 3;
        int lifetime = empowered ? 135 : 120;

        AvatarMeteorEffectManager.add(new AvatarMeteorEffect(
                sl, center, powerMult, radiusMult, specialLevel, lifetime, this.getUUID()));
        sl.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.WITHER_SPAWN,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.5F, empowered ? 0.85F : 0.75F);
    }

    private void voidPrison(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        double tx = target.getX(), ty = target.getY(), tz = target.getZ();
        ServerVisualBroadcaster.circle(sl,
                new net.minecraft.world.phys.Vec3(tx, ty, tz),
                6.0F, 0.15F, 0.0F, 0.2F, 80, 32, "pentagram");
        target.getPersistentData().putInt("transcend_void_prison", 60);
        target.getPersistentData().putDouble("transcend_void_prison_x", tx);
        target.getPersistentData().putDouble("transcend_void_prison_z", tz);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3, false, true));
        sl.playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.PORTAL_TRIGGER,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.5F);
    }

    private void deployBossWarningEffect() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        SpellElement el = currentElement != null ? currentElement : SpellElement.FIRE;

        ServerVisualBroadcaster.shockwave(sl,
                new net.minecraft.world.phys.Vec3(this.getX(), this.getY() + 0.1, this.getZ()),
                3.5F, el.getParticleR(), el.getParticleG(), el.getParticleB(), 25);
        ServerVisualBroadcaster.shieldRipple(sl,
                new net.minecraft.world.phys.Vec3(this.getX(), this.getY() + 0.5, this.getZ()),
                2.0F, el.getParticleR(), el.getParticleG(), el.getParticleB(), 20);
    }

    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getBossDrops() {
        java.util.List<net.minecraft.world.item.ItemStack> drops = new java.util.ArrayList<>();
        drops.add(new net.minecraft.world.item.ItemStack(ModItems.epic_ingot.get(), 4 + this.random.nextInt(4)));
        drops.add(new net.minecraft.world.item.ItemStack(ModItems.transcend_ingot.get(), 16 + this.random.nextInt(16)));
        drops.add(new net.minecraft.world.item.ItemStack(ModItems.refined_magic_crystal.get(), 32 + this.random.nextInt(32)));
        drops.add(new net.minecraft.world.item.ItemStack(ModItems.enhance_special.get(), 4 + this.random.nextInt(4)));
        drops.add(new net.minecraft.world.item.ItemStack(ModItems.enhance_power.get(), 3));
        drops.add(new net.minecraft.world.item.ItemStack(ModItems.wand_master.get()));

        drops.add(new net.minecraft.world.item.ItemStack(ModItems.avatar_essence.get(), 1));

        drops.add(new net.minecraft.world.item.ItemStack(ModItems.manuscript_ascension_lore.get(), 1));
        if (this.random.nextFloat() < 0.3F) {
            drops.add(new net.minecraft.world.item.ItemStack(ModItems.transcend_curio.get()));
        }
        return drops;
    }
}
