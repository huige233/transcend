package com.huige233.transcend.entity.boss;

import com.huige233.transcend.client.renderer.ShaderSpellRenderer;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.entity.SpellWisp;
import com.huige233.transcend.init.ModEntities;
import com.huige233.transcend.spell.SpellElement;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
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

public class ElementalWarden extends PhaseDrivenBossBase {

    private static final SpellElement[] ELEMENTS = {SpellElement.FIRE, SpellElement.ICE, SpellElement.THUNDER};
    private int circleTimer = 0;

    public ElementalWarden(EntityType<? extends ElementalWarden> type, Level level) {
        super(type, level, Component.translatable("entity.transcend.elemental_warden"), BossEvent.BossBarColor.YELLOW);
        this.currentElement = ELEMENTS[level.getRandom().nextInt(ELEMENTS.length)];
        this.secondaryElement = getCounterElement(this.currentElement);
        addPhaseRule(BossPhase.PHASE_1, 0.5F, BossPhase.PHASE_2);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    public BossFaction getFaction() { return BossFaction.LIGHT; }

    // 假血条与真实血量同步：不使用fake bar，血条直接显示真实HP
    @Override
    protected boolean useFakeBossBar() {
        return false;
    }

    @Override
    protected boolean hasNextPhase() {
        // Warden只有2阶段：PHASE_1→PHASE_2，PHASE_2是最终阶段
        return currentPhase == BossPhase.PHASE_1;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 16.0F));
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

        circleTimer++;

        if (currentPhase == BossPhase.PHASE_1) {
            if (spellCooldown <= 0 && getTarget() != null) {
                fireSpellAtTarget(currentElement, 1.5F);
                spellCooldown = 8;
            }
            if (circleTimer >= 400) {
                deployMagicCircle(currentElement);
                circleTimer = 0;
            }
            if (phaseTimer % 100 == 0 && phaseTimer > 0) {
                fireSpiralBarrage(currentElement, 1.2F, 4, 0.4F);
            }
        } else if (currentPhase == BossPhase.PHASE_2) {
            if (spellCooldown <= 0 && getTarget() != null) {
                fireSpellAtTarget(currentElement, 1.8F);
                if (phaseTimer % 10 == 0) {
                    fireSpellAtTarget(secondaryElement, 1.5F);
                }
                spellCooldown = 5;
            }
            if (phaseTimer % 60 == 0 && phaseTimer > 0) {
                fireSpiralBarrage(currentElement, 1.5F, 6, 0.5F);
            }
            if (phaseTimer % 150 == 0 && phaseTimer > 0) {
                groundSlam(currentElement, 8.0F, 5.0);
            }
            if (circleTimer >= 300) {
                deployMagicCircle(currentElement);
                circleTimer = 0;
            }
        }
    }

    @Override
    protected void checkPhaseTransition() {
        runPhaseStateMachine();
    }

    @Override
    protected void onPhaseChange(BossPhase newPhase) {
        if (newPhase == BossPhase.PHASE_2 && this.level() instanceof ServerLevel sl) {
            secondaryElement = getCounterElement(currentElement);
            for (int i = 0; i < 2; i++) {
                SpellWisp wisp = new SpellWisp(ModEntities.SPELL_WISP.get(), sl);
                wisp.setPos(this.getX() + (i - 0.5) * 2, this.getY(), this.getZ());
                wisp.setOwner(this);
                sl.addFreshEntity(wisp);
            }
        }
    }

    // Boss不使用持续性地面法阵光环，只在特殊技能(deployMagicCircle)时显示
    @Override
    protected void tickArenaAura() {
        // Intentionally empty — Warden does not use persistent floor circles.
    }

    @Override
    protected void spawnAmbientParticles() {
        float r = currentElement.particleR, g = currentElement.particleG, b = currentElement.particleB;

        BossParticleModel.renderWardenBody(this.level(),
                this.getX(), this.getY(), this.getZ(), r, g, b, this.tickCount);

        Vec3 center = new Vec3(this.getX(), this.getY(), this.getZ());
        if (this.tickCount % 3 == 0) {
            ShaderSpellRenderer.addShieldRipple(center.add(0.0, 1.15, 0.0),
                    1.25F + 0.18F * (float) Math.sin(this.tickCount * 0.05),
                    r, g, b, 14);
            ShaderSpellRenderer.addSpellEffect(center.add(-1.2, 1.0, 0.0),
                    center.add(1.2, 1.0, 0.0), r, g, b, 12, "slash");
        }
    }

    @Override
    protected float getElementDamageMultiplier(SpellElement element) {
        SpellElement weakness = getCounterElement(currentElement);
        if (element == weakness) return 2.0F;
        if (element == currentElement) return 0.3F;
        return 1.0F;
    }

    private static SpellElement getCounterElement(SpellElement element) {
        return switch (element) {
            case FIRE -> SpellElement.ICE;
            case ICE -> SpellElement.THUNDER;
            case THUNDER -> SpellElement.FIRE;
            default -> SpellElement.FIRE;
        };
    }

    /**
     * Boss施展特殊技能时的视觉法阵警告（纯ShaderSpellRenderer特效，非玩家法阵系统）。
     */
    private void deployMagicCircle(SpellElement element) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        float r = element.particleR, g = element.particleG, b = element.particleB;
        // 地面法阵预警（lifetime=80帧，消退自然）
        ShaderSpellRenderer.addCircle(
                new Vec3(this.getX(), this.getY() + 0.1, this.getZ()),
                3.5F, r, g, b, 80, 28, "hexagram");
        // 内圈辅助五芒星
        ShaderSpellRenderer.addCircle(
                new Vec3(this.getX(), this.getY() + 0.15, this.getZ()),
                1.8F, r * 0.8F, g * 0.8F, b * 0.8F, 60, 20, "pentagram");
        // 能量冲击波
        ShaderSpellRenderer.addShockwave(
                new Vec3(this.getX(), this.getY() + 0.2, this.getZ()),
                4.0F, r, g, b, 20);
        sl.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.5F, 0.7F + element.ordinal() * 0.1F);
    }

    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getBossDrops() {
        java.util.List<net.minecraft.world.item.ItemStack> drops = new java.util.ArrayList<>();
        drops.add(new net.minecraft.world.item.ItemStack(ModItems.transcend_ingot.get(), 4 + this.random.nextInt(5)));
        drops.add(new net.minecraft.world.item.ItemStack(ModItems.refined_magic_crystal.get(), 8 + this.random.nextInt(8)));
        // Round 26: 100% 掉 1 件 warden_essence — boss 进度货币
        drops.add(new net.minecraft.world.item.ItemStack(ModItems.warden_essence.get(), 1));
        // Round 31: 100% 掉「四相起源录」— Warden 与四元素相关，对应 aspect_lore
        drops.add(new net.minecraft.world.item.ItemStack(ModItems.manuscript_aspect_lore.get(), 1));
        // Round 38: 60% 掉 Sealed Scroll（1-2 张）— Warden 是古卷 progression 的入门 boss
        if (this.random.nextFloat() < 0.6F) {
            drops.add(new net.minecraft.world.item.ItemStack(ModItems.sealed_scroll.get(),
                    1 + this.random.nextInt(2)));
        }
        if (this.random.nextFloat() < 0.3F) {
            drops.add(new net.minecraft.world.item.ItemStack(ModItems.rift_fragment.get()));
        }
        SpellElement dropEl = ELEMENTS[this.random.nextInt(ELEMENTS.length)];
        net.minecraft.world.item.ItemStack elementItem = switch (dropEl) {
            case FIRE -> new net.minecraft.world.item.ItemStack(ModItems.enhance_power.get(), 2);
            case ICE -> new net.minecraft.world.item.ItemStack(ModItems.enhance_duration.get(), 2);
            default -> new net.minecraft.world.item.ItemStack(ModItems.enhance_efficiency.get(), 2);
        };
        drops.add(elementItem);
        return drops;
    }
}
