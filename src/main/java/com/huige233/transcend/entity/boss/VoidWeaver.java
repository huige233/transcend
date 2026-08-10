package com.huige233.transcend.entity.boss;

import com.huige233.transcend.entity.SpellGuardian;
import com.huige233.transcend.init.ModEntities;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.spell.SpellElement;
import com.huige233.transcend.visual.ServerVisualBroadcaster;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 虚空织者首领。 */
public class VoidWeaver extends PhaseDrivenBossBase {

    private int teleportTimer = 0;

    public VoidWeaver(EntityType<? extends VoidWeaver> type, Level level) {
        super(type, level, Component.translatable("entity.transcend.void_weaver"), BossEvent.BossBarColor.PURPLE);
        this.currentElement = SpellElement.CHAOS;
        this.secondaryElement = SpellElement.METAL;
        addPhaseRule(BossPhase.PHASE_1, 0.6F, BossPhase.PHASE_2);
        addPhaseRule(BossPhase.PHASE_2, 0.3F, BossPhase.PHASE_3);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public BossFaction getFaction() { return BossFaction.VOID; }

    @Override
    protected int getBaseSpellTier() { return 6; }

    @Override
    protected boolean useFakeBossBar() {
        return false;
    }

    @Override
    protected boolean hasNextPhase() {

        return currentPhase != BossPhase.PHASE_3;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 20.0F));
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

        teleportTimer++;
        Player target = getNearbyPlayers(48).stream().findFirst().orElse(null);

        switch (currentPhase) {
            case PHASE_1 -> {
                if (spellCooldown <= 0 && getTarget() != null) {
                    fireSpellAtTarget(SpellElement.CHAOS, 1.5F);
                    if (phaseTimer % 20 == 0) fireSpellAtTarget(SpellElement.METAL, 1.2F);
                    spellCooldown = 6;
                }
                if (teleportTimer >= 300 && target != null) {
                    teleportNearTarget(target);
                    novaBlast(SpellElement.CHAOS, 2.0F);
                    teleportTimer = 0;
                }
            }
            case PHASE_2 -> {
                if (spellCooldown <= 0 && getTarget() != null) {
                    fireSpellAtTarget(SpellElement.CHAOS, 2.0F);
                    fireSpellAtTarget(SpellElement.METAL, 1.5F);
                    spellCooldown = 5;
                }
                if (teleportTimer >= 200) {
                    if (target != null) teleportNearTarget(target);
                    novaBlast(SpellElement.CHAOS, 2.5F);
                    teleportTimer = 0;
                }
                if (phaseTimer % 120 == 0 && phaseTimer > 0) {
                    fireBeamSweep(SpellElement.CHAOS, 6.0F);
                }
                getNearbyPlayers(20).forEach(p ->
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, true)));
            }
            case PHASE_3 -> {
                if (spellCooldown <= 0 && getTarget() != null) {
                    fireSpellAtTarget(SpellElement.CHAOS, 2.5F);
                    if (phaseTimer % 6 == 0) fireSpellAtTarget(SpellElement.CHAOS, 2.0F);
                    spellCooldown = 4;
                }
                if (phaseTimer % 80 == 0 && phaseTimer > 0) {
                    fireSpiralBarrage(SpellElement.CHAOS, 2.0F, 8, 0.6F);
                }
                if (phaseTimer % 150 == 0 && phaseTimer > 0) {
                    fireBeamSweep(SpellElement.CHAOS, 8.0F);
                }
                if (teleportTimer >= 200) {
                    dimensionalRift();
                    teleportTimer = 0;
                }
            }
        }
    }

    @Override
    protected void checkPhaseTransition() {
        runPhaseStateMachine();
    }

    @Override
    protected void onPhaseChange(BossPhase newPhase) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        if (newPhase == BossPhase.PHASE_2) {
            for (int i = 0; i < 4; i++) {
                SpellGuardian guard = new SpellGuardian(ModEntities.SPELL_GUARDIAN.get(), sl);
                double angle = Math.PI * 2 * i / 4;
                guard.setPos(this.getX() + Math.cos(angle) * 3, this.getY(), this.getZ() + Math.sin(angle) * 3);
                guard.setOwner(this);
                sl.addFreshEntity(guard);
            }

            deployBossCircleEffect(0.15F, 0.0F, 0.2F, 4.0F);
            deployBossCircleEffect(0.9F, 0.7F, 0.1F, 3.0F);
        } else if (newPhase == BossPhase.PHASE_3) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.48);
        }
    }

    @Override
    protected void tickArenaAura() {

    }

    @Override
    protected void spawnAmbientParticles() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        BossParticleModel.renderWeaverBody(sl,
                this.getX(), this.getY(), this.getZ(), this.tickCount);

        Vec3 center = new Vec3(this.getX(), this.getY(), this.getZ());
        if (this.tickCount % 3 == 0) {
            ServerVisualBroadcaster.shieldRipple(sl, center.add(0.0, 1.1, 0.0),
                    1.35F + 0.2F * (float) Math.sin(this.tickCount * 0.04),
                    0.25F, 0.05F, 0.35F, 16);
        }
        if (this.tickCount % 5 == 0) {
            ServerVisualBroadcaster.beam(sl, center.add(-1.0, 1.2, 0.0),
                    center.add(1.0, 1.2, 0.0), 0.30F, 0.04F, 0.42F, 14, "slash");
        }
    }

    @Override
    protected float getElementDamageMultiplier(SpellElement element) {
        if (element == SpellElement.METAL) return 2.0F;
        if (element == SpellElement.CHAOS) return 0.2F;
        return 1.0F;
    }

    private void teleportNearTarget(Player target) {

        if (this.level() instanceof ServerLevel sl) {
            ServerVisualBroadcaster.circle(sl, new Vec3(this.getX(), this.getY() + 0.1, this.getZ()),
                    2.0F, 0.15F, 0.0F, 0.2F, 60, 22, "pentagram");
        }
        double angle = this.random.nextDouble() * Math.PI * 2;
        double dist = 3.0 + this.random.nextDouble() * 2.0;
        this.bossTeleportTo(target.getX() + Math.cos(angle) * dist, target.getY(), target.getZ() + Math.sin(angle) * dist);
        if (this.level() instanceof ServerLevel sl) {

            ServerVisualBroadcaster.circle(sl, new Vec3(this.getX(), this.getY() + 0.1, this.getZ()),
                    2.4F, 0.25F, 0.03F, 0.35F, 80, 24, "hexagram");
            ServerVisualBroadcaster.shieldRipple(sl, new Vec3(this.getX(), this.getY() + 1.0, this.getZ()),
                    2.2F, 0.25F, 0.03F, 0.35F, 18);
            ServerVisualBroadcaster.shockwave(sl, new Vec3(this.getX(), this.getY() + 0.2, this.getZ()),
                    2.8F, 0.25F, 0.03F, 0.35F, 16);
        }
    }

    private void novaBlast(SpellElement element, float power) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2 * i / 12;
            com.huige233.transcend.spell.SpellProjectile proj =
                    new com.huige233.transcend.spell.SpellProjectile(
                            this.level(), this, com.huige233.transcend.spell.SpellCarrier.ORB,
                            element, java.util.List.of(), power, 1);
            proj.setPos(this.getX(), this.getEyeY(), this.getZ());
            proj.setDeltaMovement(Math.cos(angle) * 0.5, 0.1, Math.sin(angle) * 0.5);
            sl.addFreshEntity(proj);
        }
    }

    private void dimensionalRift() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        getNearbyPlayers(20).forEach(p -> {
            double angle = sl.getRandom().nextDouble() * Math.PI * 2;
            double dist = 5 + sl.getRandom().nextDouble() * 10;
            p.teleportTo(p.getX() + Math.cos(angle) * dist, p.getY(), p.getZ() + Math.sin(angle) * dist);
        });
        ServerVisualBroadcaster.shockwave(sl, new Vec3(this.getX(), this.getY() + 0.2, this.getZ()),
                8.0F, 0.22F, 0.02F, 0.32F, 24);
        ServerVisualBroadcaster.circle(sl, new Vec3(this.getX(), this.getY() + 0.1, this.getZ()),
                5.2F, 0.24F, 0.04F, 0.34F, 18, 30, "pentagram");
    }

    private void deployBossCircleEffect(float r, float g, float b, float radius) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        ServerVisualBroadcaster.circle(sl,
                new Vec3(this.getX(), this.getY() + 0.1, this.getZ()),
                radius, r, g, b, 80, 28, "pentagram");
        ServerVisualBroadcaster.shockwave(sl,
                new Vec3(this.getX(), this.getY() + 0.2, this.getZ()),
                radius * 1.2F, r, g, b, 22);
    }

    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getBossDrops() {
        java.util.List<net.minecraft.world.item.ItemStack> drops = new java.util.ArrayList<>();
        drops.add(new net.minecraft.world.item.ItemStack(ModItems.transcend_ingot.get(), 8 + this.random.nextInt(8)));
        drops.add(new net.minecraft.world.item.ItemStack(ModItems.refined_magic_crystal.get(), 16 + this.random.nextInt(16)));
        drops.add(new net.minecraft.world.item.ItemStack(ModItems.enhance_special.get(), 2 + this.random.nextInt(3)));

        drops.add(new net.minecraft.world.item.ItemStack(ModItems.weaver_essence.get(), 1));

        drops.add(new net.minecraft.world.item.ItemStack(ModItems.manuscript_boss_lore.get(), 1));
        if (this.random.nextFloat() < 0.5F) {
            drops.add(new net.minecraft.world.item.ItemStack(ModItems.transcendence_core.get()));
        }
        if (this.random.nextFloat() < 0.2F) {
            drops.add(new net.minecraft.world.item.ItemStack(ModItems.wand_advanced.get()));
        }
        return drops;
    }
}
