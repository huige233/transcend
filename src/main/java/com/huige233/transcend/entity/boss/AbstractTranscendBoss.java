
package com.huige233.transcend.entity.boss;

import com.huige233.transcend.entity.SpellGuardian;
import com.huige233.transcend.entity.SpellWisp;
import com.huige233.transcend.entity.boss.BossFaction;
import com.huige233.transcend.entity.boss.BossPhase;
import com.huige233.transcend.init.ModEffects;
import com.huige233.transcend.mixinitf.ITranscendMarked;
import com.huige233.transcend.spell.SpellCarrier;
import com.huige233.transcend.spell.SpellElement;
import com.huige233.transcend.spell.SpellProjectile;
import com.huige233.transcend.util.EntityCompatUtil;
import com.huige233.transcend.visual.ServerVisualBroadcaster;
import com.huige233.transcend.world.TranscendDimensions;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractTranscendBoss
extends Monster {
    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(AbstractTranscendBoss.class, (EntityDataSerializer)EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TRANSITION_TICK = SynchedEntityData.defineId(AbstractTranscendBoss.class, (EntityDataSerializer)EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_ELEMENT = SynchedEntityData.defineId(AbstractTranscendBoss.class, (EntityDataSerializer)EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_ANIM_TICK = SynchedEntityData.defineId(AbstractTranscendBoss.class, (EntityDataSerializer)EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ANIM_TYPE = SynchedEntityData.defineId(AbstractTranscendBoss.class, (EntityDataSerializer)EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_FAKE_BAR_PROGRESS = SynchedEntityData.defineId(AbstractTranscendBoss.class, (EntityDataSerializer)EntityDataSerializers.FLOAT);
    protected final ServerBossEvent bossBar;
    protected BossPhase currentPhase = BossPhase.PHASE_1;
    protected int phaseTimer = 0;
    protected int combatTicks = 0;
    protected SpellElement currentElement;
    protected SpellElement secondaryElement;
    protected int spellCooldown = 0;
    private LivingEntity lastAttacker = null;
    private boolean selfTeleport = false;
    private boolean combatStarted = false;
    private int omniIFrames = 0;
    private int omniHealTimer = 0;
    private int healthMutationGraceTicks = 0;
    private float lockedHealth = -1.0f;

    public BossPhase getCurrentPhase() {
        return BossPhase.values()[Math.min((Integer)this.entityData.get(DATA_PHASE), BossPhase.values().length - 1)];
    }

    public SpellElement getCurrentElement() {
        return java.util.Objects.requireNonNull(
                SpellElement.getById((String)this.entityData.get(DATA_ELEMENT)),
                "Boss has an invalid current element");
    }

    public int getPhaseTransitionTick() {
        return (Integer)this.entityData.get(DATA_TRANSITION_TICK);
    }

    protected void clearTransitionTick() {
        this.entityData.set(DATA_TRANSITION_TICK, 0);
    }

    public int getAttackAnimTick() {
        return (Integer)this.entityData.get(DATA_ANIM_TICK);
    }

    public int getAttackAnimType() {
        return (Integer)this.entityData.get(DATA_ANIM_TYPE);
    }

    public float getDisplayedBossBarProgress() {
        if (!this.useFakeBossBar()) {
            return Mth.clamp(this.getHealth() / Math.max(1.0f, this.getMaxHealth()), 0.0f, 1.0f);
        }
        return Mth.clamp(((Float)this.entityData.get(DATA_FAKE_BAR_PROGRESS)).floatValue(), 0.0f, 1.0f);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, 0);
        this.entityData.define(DATA_TRANSITION_TICK, 0);
        this.entityData.define(DATA_ELEMENT, "fire");
        this.entityData.define(DATA_ANIM_TICK, 0);
        this.entityData.define(DATA_ANIM_TYPE, 0);
        this.entityData.define(DATA_FAKE_BAR_PROGRESS, Float.valueOf(1.0f));
    }

    private void syncData() {
        this.entityData.set(DATA_PHASE, this.currentPhase.ordinal());
        this.entityData.set(DATA_ELEMENT, this.currentElement != null ? this.currentElement.id : "fire");
    }

    protected AbstractTranscendBoss(EntityType<? extends Monster> type, Level level, Component displayName, BossEvent.BossBarColor barColor) {
        super(type, level);
        this.bossBar = new ServerBossEvent(displayName, barColor, BossEvent.BossBarOverlay.NOTCHED_10);
        this.bossBar.setVisible(false);
        this.setPersistenceRequired();
        this.xpReward = 500;
    }

    public abstract BossFaction getFaction();

    public void tick() {
        if (!this.level().isClientSide && this.getY() < (double)(this.level().getMinBuildHeight() - 16)) {
            BlockPos spawn = this.level().getSharedSpawnPos();
            this.bossTeleportTo((double)spawn.getX() + 0.5, spawn.getY() + 1, (double)spawn.getZ() + 0.5);
            this.setDeltaMovement(0.0, 0.0, 0.0);
            this.fallDistance = 0.0f;
        }
        super.tick();
        if (!this.level().isClientSide) {
            this.tickOmniBossGuard();
        }
        ++this.phaseTimer;
        int ptt = (Integer)this.entityData.get(DATA_TRANSITION_TICK);
        if (ptt > 0) {
            this.entityData.set(DATA_TRANSITION_TICK, ptt - 1);
            int at = (Integer)this.entityData.get(DATA_ANIM_TICK);
            if (at > 0) {
                this.entityData.set(DATA_ANIM_TICK, at - 1);
            }
            if (!this.level().isClientSide) {
                this.setDeltaMovement(0.0, 0.05, 0.0);
                this.getNavigation().stop();
                this.setTarget(null);
                Level level = this.level();
                if (level instanceof ServerLevel) {
                    ServerLevel sl = (ServerLevel)level;
                    if (this.currentElement != null) {
                        float er = this.currentElement.getParticleR();
                        float eg = this.currentElement.getParticleG();
                        float eb = this.currentElement.getParticleB();
                        float ring = 1.0F + (60 - ptt) * 0.04F;
                        ServerVisualBroadcaster.circle(sl,
                                new Vec3(this.getX(), this.getY() + 1.0 + (60 - ptt) * 0.04, this.getZ()),
                                ring, er, eg, eb, 12, 20, "pentagram");
                        if ((ptt % 8) == 0) {
                            ServerVisualBroadcaster.shieldRipple(sl,
                                    new Vec3(this.getX(), this.getY() + 1.5, this.getZ()),
                                    1.2F + (60 - ptt) * 0.02F, er, eg, eb, 10);
                        }
                    }
                }
            }
            return;
        }
        int at2 = (Integer)this.entityData.get(DATA_ANIM_TICK);
        if (at2 > 0) {
            this.entityData.set(DATA_ANIM_TICK, at2 - 1);
        }
        if (!this.level().isClientSide) {
            List<LivingEntity> candidates;
            Player tp;
            Player p;
            LivingEntity livingEntity;
            this.bossBar.setProgress(this.getDisplayedBossBarProgress());
            this.checkPhaseTransition();
            this.syncData();
            if (this.combatStarted || this.getTarget() != null || this.lastAttacker != null) {
                this.combatStarted = true;
                ++this.combatTicks;
            }
            if (this.spellCooldown > 0) {
                --this.spellCooldown;
            }
            this.tickArenaAura();
            double trackRange = this.getTrackingRange();
            double trackRangeSq = trackRange * trackRange;
            AABB search = this.getBoundingBox().inflate(trackRange);
            if (this.lastAttacker != null && this.lastAttacker.isAlive() && this.lastAttacker.distanceToSqr((Entity)this) < trackRangeSq && (!((livingEntity = this.lastAttacker) instanceof Player) || !(p = (Player)livingEntity).isCreative() && !p.isSpectator())) {
                this.setTarget(this.lastAttacker);
            } else if ((this.getTarget() == null || !this.getTarget().isAlive() || this.getTarget().distanceToSqr((Entity)this) > trackRangeSq || (livingEntity = this.getTarget()) instanceof Player && ((tp = (Player)livingEntity).isCreative() || tp.isSpectator())) && !(candidates = this.level().getEntitiesOfClass(LivingEntity.class, search, e -> {
                if (!e.isAlive()) return false;
                if (e == this) return false;
                if (e instanceof SpellGuardian) return false;
                if (e instanceof SpellWisp) return false;
                if (e instanceof Player) {
                    Player candidatePlayer = (Player)e;
                    if (candidatePlayer.isCreative()) return false;
                    if (candidatePlayer.isSpectator()) return false;
                }
                if (!(e instanceof AbstractTranscendBoss)) return true;
                AbstractTranscendBoss ob = (AbstractTranscendBoss)((Object)e);
                if (!this.getFaction().isHostileTo(ob.getFaction())) return false;
                return true;
            })).isEmpty()) {
                candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(this)));
                this.setTarget((LivingEntity)candidates.get(0));
            }
        }
        if (!this.level().isClientSide) {
            this.spawnAmbientParticles();
        }
    }

    protected abstract void checkPhaseTransition();

    protected abstract void onPhaseChange(BossPhase var1);

    protected abstract void spawnAmbientParticles();

    protected abstract float getElementDamageMultiplier(SpellElement var1);

    protected double getTrackingRange() {
        return 24.0;
    }

    protected boolean useFakeBossBar() {
        return false;
    }

    protected float getFakeBossBarDurabilityMultiplier() {
        return 3.0f;
    }

    protected boolean refillFakeBossBarOnPhaseChange() {
        return true;
    }

    private void setFakeBossBarProgress(float progress) {
        this.entityData.set(DATA_FAKE_BAR_PROGRESS, Float.valueOf(Mth.clamp(progress, 0.0f, 1.0f)));
    }

    private void syncFakeBossBarProgressToRealHealth() {
        this.setFakeBossBarProgress(this.getHealth() / Math.max(1.0f, this.getMaxHealth()));
    }

    private void consumeFakeBossBarByDamage(float damageApplied) {
        if (!this.useFakeBossBar()) {
            this.syncFakeBossBarProgressToRealHealth();
            return;
        }
        if (damageApplied <= 0.0f) {
            return;
        }
        float maxHealth = Math.max(1.0f, this.getMaxHealth());
        float durability = Math.max(0.01f, this.getFakeBossBarDurabilityMultiplier());
        float consume = damageApplied / (maxHealth * durability);
        float now = this.getDisplayedBossBarProgress();
        this.setFakeBossBarProgress(now - consume);
    }

    protected float getBossResistance() {
        return 1.0f;
    }

    protected float getBossWeakDamageLimit() {
        return 0.0f;
    }

    protected int getBossIFrames() {
        return 10;
    }

    protected int getBossHealTime() {
        return 20;
    }

    protected float getBossHealAmount() {
        return this.getMaxHealth() / 200.0f;
    }

    protected boolean enableBossHealthLock() {
        return true;
    }

    protected boolean enableBossAutoHeal() {
        return true;
    }

    protected final void bossSetHealthGuarded(float health) {
        this.healthMutationGraceTicks = Math.max(this.healthMutationGraceTicks, 2);
        super.setHealth(Math.max(0.0f, Math.min(this.getMaxHealth(), health)));
        this.lockedHealth = super.getHealth();
    }

    protected final void bossHealGuarded(float amount) {
        if (amount <= 0.0f) {
            return;
        }
        this.bossSetHealthGuarded(super.getHealth() + amount);
    }

    private void tickOmniBossGuard() {
        if (Float.isNaN(this.lockedHealth) || this.lockedHealth < 0.0f) {
            this.lockedHealth = super.getHealth();
        }
        if (this.omniIFrames > 0) {
            --this.omniIFrames;
        }
        if (this.healthMutationGraceTicks > 0) {
            --this.healthMutationGraceTicks;
            this.lockedHealth = super.getHealth();
        } else if (this.enableBossHealthLock()) {
            float liveHealth = super.getHealth();
            if (Math.abs(liveHealth - this.lockedHealth) > 0.01f) {
                this.bossSetHealthGuarded(this.lockedHealth);
            }
        }

        if (this.enableBossHealthLock() && this.deathTime > 0 && this.lockedHealth > 0.0f) {
            this.deathTime = 0;
        }
        if (this.enableBossAutoHeal()) {
            if (this.omniHealTimer > 0) {
                --this.omniHealTimer;
            } else if (this.isAlive() && super.getHealth() < this.getMaxHealth() && (Integer)this.entityData.get(DATA_TRANSITION_TICK) <= 0) {
                float healAmount = Math.max(0.0f, this.getBossHealAmount());
                if (healAmount > 0.0f) {
                    this.bossHealGuarded(healAmount);
                }
                this.omniHealTimer = Math.max(1, this.getBossHealTime());
            }
        }
        if (!this.useFakeBossBar()) {
            this.syncFakeBossBarProgressToRealHealth();
        }
    }

    protected float getSpellPowerMultiplier() {
        float base = 1.0f + 19.0f * Math.min(1.0f, (float)this.combatTicks / 48000.0f);
        if (this.isInArenaDimension()) {
            base *= 1.2f;
        }
        return base;
    }

    protected int getBaseSpellTier() {
        return 1;
    }

    protected final int getBossSpellTier() {
        return BossSpellTier.calculate(this.getBaseSpellTier(), this.currentPhase.ordinal());
    }

    protected boolean isInArenaDimension() {
        return this.level() != null && this.level().dimension() == TranscendDimensions.ARENA_LEVEL;
    }

    protected void tickArenaAura() {
        if (!this.isInArenaDimension() || !(this.level() instanceof ServerLevel sl)) {
            return;
        }
        if (this.tickCount % 80 != 0) {
            return;
        }
        float er = this.currentElement != null ? this.currentElement.getParticleR() : 1.0f;
        float eg = this.currentElement != null ? this.currentElement.getParticleG() : 1.0f;
        float eb = this.currentElement != null ? this.currentElement.getParticleB() : 1.0f;
        float auraRadius = 5.0f + this.currentPhase.ordinal() * 1.2f;
        ServerVisualBroadcaster.circle(sl, new Vec3(this.getX(), this.getY() + 0.08, this.getZ()),
                auraRadius, er, eg, eb, 50, 36, "hexagram");
        if (this.getTarget() != null && this.tickCount % 160 == 0) {
            ServerVisualBroadcaster.beam(sl,
                    new Vec3(this.getX(), this.getEyeY(), this.getZ()),
                    new Vec3(this.getTarget().getX(), this.getTarget().getEyeY(), this.getTarget().getZ()),
                    er, eg, eb, 20, "beam");
        }
    }

    public void forceSetPhase(BossPhase phase) {
        this.currentPhase = phase;
        this.phaseTimer = 0;
        this.spellCooldown = 40;
        this.entityData.set(DATA_PHASE, phase.ordinal());
        if (this.useFakeBossBar()) {
            this.entityData.set(DATA_FAKE_BAR_PROGRESS, Float.valueOf(1.0f));
        }
        this.onPhaseChange(phase);
    }

    protected boolean hasNextPhase() {
        return true;
    }

    protected void setPhase(BossPhase phase) {
        if (this.currentPhase != phase) {
            BossPhase oldPhase = this.currentPhase;
            this.currentPhase = phase;
            this.phaseTimer = 0;
            this.spellCooldown = 20;
            this.entityData.set(DATA_TRANSITION_TICK, 60);
            this.syncData();
            if (this.useFakeBossBar() && this.refillFakeBossBarOnPhaseChange()) {
                this.setFakeBossBarProgress(1.0f);
            }
            this.onPhaseChange(phase);
            Level level = this.level();
            if (level instanceof ServerLevel) {
                ServerLevel sl = (ServerLevel)level;
                int phaseNum = phase.ordinal() + 1;
                for (ServerPlayer sp : sl.getPlayers(p -> p.distanceTo((Entity)this) < 64.0f)) {
                    sp.connection.send((Packet)new ClientboundSetTitleTextPacket((Component)Component.translatable("title.transcend.boss_phase", phaseNum).withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD})));
                    sp.connection.send((Packet)new ClientboundSetSubtitleTextPacket((Component)this.getDisplayName().copy().withStyle(ChatFormatting.GOLD)));
                    sp.connection.send((Packet)new ClientboundSetTitlesAnimationPacket(10, 30, 10));
                }
                sl.playSound(null, this.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.0f, 0.6f + (float)phaseNum * 0.15f);
                sl.playSound(null, this.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5f, 1.0f + (float)phaseNum * 0.1f);
                float er = this.currentElement != null ? this.currentElement.getParticleR() : 1.0f;
                float eg = this.currentElement != null ? this.currentElement.getParticleG() : 1.0f;
                float eb = this.currentElement != null ? this.currentElement.getParticleB() : 1.0f;
                for (int ring = 0; ring < 3; ++ring) {
                    float radius = 2.0f + ring * 2.0f;
                    ServerVisualBroadcaster.circle(sl,
                            new Vec3(this.getX(), this.getY() + 0.5 + ring * 0.3, this.getZ()),
                            radius,
                            Math.min(1.0f, er + 0.3f), Math.min(1.0f, eg + 0.3f), Math.min(1.0f, eb + 0.3f),
                            18 + ring * 4, 30, ring % 2 == 0 ? "hexagram" : "pentagram");
                }
                ServerVisualBroadcaster.shockwave(sl, new Vec3(this.getX(), this.getY() + 0.1, this.getZ()),
                        12.0f + (float)(phaseNum * 2), er, eg, eb, 40);
                ServerVisualBroadcaster.beam(sl,
                        new Vec3(this.getX(), this.getY() + 4.5, this.getZ()),
                        new Vec3(this.getX(), this.getY() + 0.5, this.getZ()),
                        er, eg, eb, 24, "beam");
                for (ServerPlayer sp : sl.getPlayers(p -> p.distanceTo((Entity)this) < 24.0f)) {
                    double kz;
                    double kx = sp.getX() - this.getX();
                    double dist = Math.sqrt(kx * kx + (kz = sp.getZ() - this.getZ()) * kz);
                    if (!(dist > 0.1)) continue;
                    float strength = (float)(0.6 * (1.0 - Math.min(1.0, dist / 24.0)));
                    sp.knockback((double)strength, -kx / dist, -kz / dist);
                    sp.hurtMarked = true;
                }
            }
        }
    }

    public boolean hurt(DamageSource source, float amount) {
        if (this instanceof ITranscendMarked m && m.transcend$isMarked()) {
            return super.hurt(source, amount);
        }
        if (this.omniIFrames > 0) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }
        if (source == this.damageSources().fellOutOfWorld()) {
            return false;
        }
        if (source.getEntity() == this || source.getDirectEntity() == this) {
            return false;
        }
        if (source.getDirectEntity() instanceof SpellProjectile proj && proj.getOwner() == this) {
            return false;
        }
        if ((Integer)this.entityData.get(DATA_TRANSITION_TICK) > 0) {
            return false;
        }
        if (source.getEntity() == null && source.getDirectEntity() == null) {
            return false;
        }

        Entity src = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        if (src instanceof AbstractTranscendBoss otherBoss && !this.getFaction().isHostileTo(otherBoss.getFaction())) {
            return false;
        }
        if (src instanceof SpellProjectile p && p.getOwner() instanceof AbstractTranscendBoss ob
                && !this.getFaction().isHostileTo(ob.getFaction())) {
            return false;
        }
        if (src instanceof SpellGuardian || src instanceof SpellWisp) {
            return false;
        }

        if (source.getEntity() instanceof LivingEntity attacker && attacker != this) {
            this.lastAttacker = attacker;
            this.combatStarted = true;
        }

        if (source.getEntity() instanceof LivingEntity) {
            SpellElement hitElement = this.getIncomingSpellElement(source);
            if (hitElement != null) {
                float mult = this.getElementDamageMultiplier(hitElement);
                amount *= mult;
                Level level = this.level();
                if (mult > 1.5f && level instanceof ServerLevel sl) {
                    ServerVisualBroadcaster.shockwave(sl, new Vec3(this.getX(), this.getY() + 1.0, this.getZ()), 2.4f, 1.0f, 1.0f, 0.35f, 14);
                    sl.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.0f, 0.8f);
                } else if (mult < 0.5f && level instanceof ServerLevel sl) {
                    sl.playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.0f, 0.6f);
                }
            }
        }

        float resistance = Math.max(0.01f, this.getBossResistance());
        amount /= resistance;
        if (amount <= 0.0f || amount < this.getBossWeakDamageLimit()) {
            return false;
        }

        amount = Math.min(amount, this.getDamageCap());

        if (amount <= 0.0f) {
            return false;
        }

        float beforeHealth = this.getHealth();
        boolean result = super.hurt(source, amount);
        if (result) {
            float damageApplied = Math.max(0.0f, beforeHealth - this.getHealth());
            this.consumeFakeBossBarByDamage(damageApplied);
            this.omniIFrames = Math.max(this.omniIFrames, Math.max(0, this.getBossIFrames()));
            this.omniHealTimer = Math.max(this.omniHealTimer, Math.max(1, this.getBossHealTime()));
            this.healthMutationGraceTicks = Math.max(this.healthMutationGraceTicks, 2);
            this.lockedHealth = super.getHealth();

            if (this.isAlive() && this.hasNextPhase() && this.getHealth() <= 1.0f) {
                this.checkPhaseTransition();
            }
        }
        return result;
    }

    protected void actuallyHurt(DamageSource source, float amount) {
        if (amount <= 0.0f) return;
        if (this.isInvulnerableTo(source)) return;

        float afterArmor = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(
                amount, (float)this.getArmorValue(), (float)this.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS));
        float afterMagic = afterArmor;

        if (this.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE) && !source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            int resistLevel = this.getEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1;
            afterMagic = Math.max(0.0f, afterArmor * (1.0f - resistLevel * 0.2f));
        }

        int protLevel = net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageProtection(this.getArmorSlots(), source);
        if (protLevel > 0) {
            afterMagic = net.minecraft.world.damagesource.CombatRules.getDamageAfterMagicAbsorb(afterMagic, (float)protLevel);
        }
        float finalDamage = Math.max(0.0f, afterMagic);

        if (this.hasNextPhase() && this.currentPhase != BossPhase.PHASE_4) {
            float currentHealth = this.getHealth();
            if (currentHealth <= 1.0f) {
                this.checkPhaseTransition();
                return;
            }
            if (finalDamage >= currentHealth - 1.0f) {

                this.bossSetHealthGuarded(1.0f);
                this.checkPhaseTransition();
                return;
            }
        }
        super.actuallyHurt(source, amount);
    }

    protected float getDamageCap() {
        return this.getMaxHealth() * 0.05f;
    }

    private SpellElement getIncomingSpellElement(DamageSource source) {
        Entity entity = source.getDirectEntity();
        if (entity instanceof SpellProjectile) {
            SpellProjectile proj = (SpellProjectile)entity;
            return proj.getElement();
        }
        return null;
    }

    protected void fireSpellAtTarget(SpellElement element, float power) {
        double dz;
        double dy;
        Level level = this.level();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel sl = (ServerLevel)level;
        LivingEntity target = this.getTarget();
        if (target == null) {
            return;
        }
        float scaledPower = power * this.getSpellPowerMultiplier();
        SpellProjectile proj = new SpellProjectile(this.level(), (LivingEntity)this, SpellCarrier.ORB, element, java.util.List.of(), scaledPower, this.getBossSpellTier());
        double dx = target.getX() - this.getX();
        double dist = Math.sqrt(dx * dx + (dy = target.getEyeY() - this.getEyeY()) * dy + (dz = target.getZ() - this.getZ()) * dz);
        if (dist > 0.0) {
            float speed = 0.6f;
            proj.setDeltaMovement(dx / dist * (double)speed, dy / dist * (double)speed, dz / dist * (double)speed);
        }
        proj.setPos(this.getX(), this.getEyeY(), this.getZ());
        sl.addFreshEntity((Entity)proj);
    }

    protected void fireSpellRain(SpellElement element, float power, double targetX, double targetZ) {
        Level level = this.level();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel sl = (ServerLevel)level;
        float scaledPower = power * this.getSpellPowerMultiplier();
        for (int i = 0; i < 8; ++i) {
            double ox = (sl.getRandom().nextDouble() - 0.5) * 6.0;
            double oz = (sl.getRandom().nextDouble() - 0.5) * 6.0;
            SpellProjectile proj = new SpellProjectile(this.level(), (LivingEntity)this, SpellCarrier.ORB, element, java.util.List.of(), scaledPower, this.getBossSpellTier());
            proj.setPos(targetX + ox, this.getY() + 12.0, targetZ + oz);
            proj.setDeltaMovement(0.0, -0.8, 0.0);
            sl.addFreshEntity((Entity)proj);
        }
    }

    protected void fireRuneVolley(SpellElement element, float power, int count) {
        Level level = this.level();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel sl = (ServerLevel)level;
        LivingEntity target = this.getTarget();
        if (target == null) {
            return;
        }
        float scaledPower = power * this.getSpellPowerMultiplier();
        for (int i = 0; i < count; ++i) {
            double angle = Math.PI * 2 * (double)i / (double)count;
            double runeX = this.getX() + Math.cos(angle) * 3.0;
            double runeY = this.getY() + 2.5 + Math.sin((double)i * 1.5) * 0.5;
            double runeZ = this.getZ() + Math.sin(angle) * 3.0;
            ServerVisualBroadcaster.circle(sl, new Vec3(runeX, runeY, runeZ), 0.7f, element.getParticleR(), element.getParticleG(), element.getParticleB(), 10, 16, "pentagram");
            SpellProjectile proj = new SpellProjectile(this.level(), (LivingEntity)this, SpellCarrier.ARROW, element, java.util.List.of(), scaledPower, this.getBossSpellTier());
            proj.setPos(runeX, runeY, runeZ);
            double dx = target.getX() - runeX;
            double dy = target.getEyeY() - runeY;
            double dz = target.getZ() - runeZ;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > 0.0) {
                float spd = 0.7f;
                proj.setDeltaMovement(dx / dist * (double)spd, dy / dist * (double)spd, dz / dist * (double)spd);
            }
            sl.addFreshEntity((Entity)proj);
        }
        sl.playSound(null, this.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 1.5f, 0.8f);
    }

    protected void fireSpiralBarrage(SpellElement element, float power, int arms, float speed) {
        Level level = this.level();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel sl = (ServerLevel)level;
        float scaledPower = power * this.getSpellPowerMultiplier();
        double baseAngle = (double)this.phaseTimer * 0.15;
        for (int arm = 0; arm < arms; ++arm) {
            double angle = baseAngle + (double)arm * Math.PI * 2.0 / (double)arms;
            SpellProjectile proj = new SpellProjectile(this.level(), (LivingEntity)this, SpellCarrier.ORB, element, java.util.List.of(), scaledPower, this.getBossSpellTier());
            proj.setPos(this.getX(), this.getEyeY(), this.getZ());
            proj.setDeltaMovement(Math.cos(angle) * (double)speed, 0.05, Math.sin(angle) * (double)speed);
            sl.addFreshEntity((Entity)proj);
        }
    }

    protected void fireBeamSweep(SpellElement element, float damage) {
        Level level = this.level();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel sl = (ServerLevel)level;
        LivingEntity target = this.getTarget();
        if (target == null) {
            return;
        }
        float scaledDamage = damage * this.getSpellPowerMultiplier();
        this.entityData.set(DATA_ANIM_TICK, 15);
        this.entityData.set(DATA_ANIM_TYPE, 1);
        ServerVisualBroadcaster.beam(sl, new Vec3(this.getX(), this.getEyeY(), this.getZ()), new Vec3(target.getX(), target.getEyeY(), target.getZ()), element.getParticleR(), element.getParticleG(), element.getParticleB(), 30, "beam");
        Vec3 look = target.position().subtract(this.position()).normalize();
        double sweepAngle = Math.atan2(look.z, look.x);
        for (int i = -3; i <= 3; ++i) {
            double angle = sweepAngle + (double)i * Math.PI / 18.0;
            Vec3 start = new Vec3(this.getX(), this.getEyeY(), this.getZ());
            for (double d = 1.0; d <= 15.0; d += 1.5) {
                double bx = start.x + Math.cos(angle) * d;
                double bz = start.z + Math.sin(angle) * d;
                AABB hitBox = new AABB(bx - 0.5, start.y - 0.5, bz - 0.5, bx + 0.5, start.y + 0.5, bz + 0.5);
                for (LivingEntity hit : sl.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != this && e.isAlive() && !(e instanceof AbstractTranscendBoss) && !EntityCompatUtil.isProtectedPlayer((Entity)e))) {
                    if (!hit.hurt(this.getBossAttackDamageSource(hit), scaledDamage)) continue;
                    this.applySoulShockDebuff(hit);
                }
            }
            ServerVisualBroadcaster.beam(sl, new Vec3(this.getX() + Math.cos(angle) * 7.5, this.getEyeY(), this.getZ() + Math.sin(angle) * 7.5), new Vec3(this.getX(), this.getEyeY(), this.getZ()), element.getParticleR(), element.getParticleG(), element.getParticleB(), 12, "slash");
        }
    }

    protected void groundSlam(SpellElement element, float damage, double radius) {
        Level level = this.level();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel sl = (ServerLevel)level;
        float scaledDamage = damage * this.getSpellPowerMultiplier();
        this.entityData.set(DATA_ANIM_TICK, 12);
        this.entityData.set(DATA_ANIM_TYPE, 2);
        ServerVisualBroadcaster.circle(sl, new Vec3(this.getX(), this.getY() + 0.1, this.getZ()), (float)radius, element.getParticleR(), element.getParticleG(), element.getParticleB(), 40, 32, "hexagram");
        AABB area = this.getBoundingBox().inflate(radius);
        for (LivingEntity hit : sl.getEntitiesOfClass(LivingEntity.class, area, e -> e != this && e.isAlive() && !(e instanceof AbstractTranscendBoss) && !EntityCompatUtil.isProtectedPlayer((Entity)e))) {
            if (hit.hurt(this.getBossAttackDamageSource(hit), scaledDamage)) {
                this.applySoulShockDebuff(hit);
            }
            Vec3 kb = hit.position().subtract(this.position()).normalize();
            hit.knockback(1.5, -kb.x, -kb.z);
            hit.hurtMarked = true;
        }
        ServerVisualBroadcaster.shockwave(sl, new Vec3(this.getX(), this.getY() + 0.2, this.getZ()), (float)(radius * 1.15), element.getParticleR(), element.getParticleG(), element.getParticleB(), 20);
    }

    protected DamageSource getBossAttackDamageSource(LivingEntity target) {
        return this.damageSources().indirectMagic((Entity)this, (Entity)this);
    }

    protected void applySoulShockDebuff(LivingEntity target) {
        if (target == null || !target.isAlive() || EntityCompatUtil.isProtectedPlayer((Entity)target)) {
            return;
        }
        int debuffLevel = Math.max(0, this.getCurrentPhase().ordinal());
        int debuffDuration = 120 + debuffLevel * 40;
        target.addEffect(new MobEffectInstance((MobEffect)ModEffects.SOUL_SHOCK.get(), debuffDuration, debuffLevel, false, true));
    }

    protected List<Player> getNearbyPlayers(double range) {
        AABB area = this.getBoundingBox().inflate(range);
        return this.level().getEntitiesOfClass(Player.class, area, p -> p.isAlive() && !p.isSpectator() && !p.isCreative());
    }

    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossBar.addPlayer(player);
    }

    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossBar.removePlayer(player);
    }

    public boolean canChangeDimensions() {
        return false;
    }

    public void die(DamageSource source) {
        this.setFakeBossBarProgress(0.0f);
        super.die(source);
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel sl = (ServerLevel)level;
            for (ItemStack drop : this.getBossDrops()) {
                ItemEntity itemEntity = new ItemEntity((Level)sl, this.getX(), this.getY() + 0.5, this.getZ(), drop);
                itemEntity.setDefaultPickUpDelay();
                sl.addFreshEntity((Entity)itemEntity);
            }
        ServerVisualBroadcaster.shockwave(sl, new Vec3(this.getX(), this.getY() + 1.0, this.getZ()), 8.5f, 1.0f, 0.9f, 0.3f, 28);
            sl.playSound(null, this.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 2.0f, 1.0f);
        }
    }

    protected abstract List<ItemStack> getBossDrops();

    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    protected void bossTeleportTo(double x, double y, double z) {
        this.selfTeleport = true;
        this.teleportTo(x, y, z);
        this.selfTeleport = false;
    }

    public void teleportTo(double x, double y, double z) {
        if (!this.selfTeleport && !this.level().isClientSide) {
            return;
        }
        super.teleportTo(x, y, z);
    }
}
