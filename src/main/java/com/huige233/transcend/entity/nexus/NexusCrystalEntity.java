package com.huige233.transcend.entity.nexus;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.init.ModBlocks;
import com.huige233.transcend.world.nexus.NexusManager;
import com.huige233.transcend.world.nexus.NexusType;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;

/**
 * 法则水晶实体 — NexusCoreBlock 被破坏后生成。
 *
 * 机制：
 * - 拥有可再生的魔力护盾（shield）和生命值（HP）
 * - 护盾存在时伤害先扣护盾，护盾归零后才扣HP
 * - 护盾在未受伤2秒后开始自动回复
 * - 5分钟（6000 tick）内未被击杀 → 消失并还原 NexusCoreBlock，core 锁定5分钟
 * - 被击杀 → 触发枢纽摧毁效果
 * - 带有 BossBar 显示护盾/HP 状态
 */
public class NexusCrystalEntity extends Mob {

    // ─── Constants ───────────────────────────────────────────────────
    private static final float MAX_SHIELD = 100.0F;
    private static final float SHIELD_REGEN_RATE = 0.5F; // 每tick回复量
    private static final int SHIELD_REGEN_DELAY = 40;     // 受伤后延迟多少tick才开始回复
    private static final int REVERT_TICKS = 6000;          // 5分钟
    private static final int LOCK_TICKS = 6000;            // 锁定5分钟

    // ─── Synched Data ────────────────────────────────────────────────
    private static final EntityDataAccessor<Float> DATA_SHIELD =
            SynchedEntityData.defineId(NexusCrystalEntity.class, EntityDataSerializers.FLOAT);

    // ─── Fields ──────────────────────────────────────────────────────
    private String nexusTypeId = "";
    private BlockPos corePosition = BlockPos.ZERO;
    private int aliveTicks = 0;
    private int lastHurtTick = -999;

    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.literal("Nexus Crystal").withStyle(ChatFormatting.LIGHT_PURPLE),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS
    );

    // ─── Constructor ─────────────────────────────────────────────────

    public NexusCrystalEntity(EntityType<? extends NexusCrystalEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = false;
        bossBar.setVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    // ─── Setup ───────────────────────────────────────────────────────

    public void setNexusData(String typeId, BlockPos corePos) {
        this.nexusTypeId = typeId;
        this.corePosition = corePos;
        NexusType type = NexusType.getById(typeId);
        if (type != null) {
            bossBar.setName(Component.translatable(type.nameKey).withStyle(type.color)
                    .append(Component.literal(" Crystal").withStyle(ChatFormatting.WHITE)));
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SHIELD, MAX_SHIELD);
    }

    @Override
    protected void registerGoals() {
        // 水晶不移动，不攻击
    }

    // ─── Shield Accessors ────────────────────────────────────────────

    public float getShield() {
        return this.entityData.get(DATA_SHIELD);
    }

    public void setShield(float value) {
        this.entityData.set(DATA_SHIELD, Math.max(0, Math.min(MAX_SHIELD, value)));
    }

    // ─── Damage Handling ─────────────────────────────────────────────

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) return false;
        if (this.level().isClientSide) return false;

        this.lastHurtTick = this.tickCount;

        float shield = getShield();
        if (shield > 0) {
            // 护盾吸收伤害
            float absorbed = Math.min(shield, amount);
            setShield(shield - absorbed);
            amount -= absorbed;

            // 护盾碎裂音效
            if (getShield() <= 0) {
                this.level().playSound(null, this.blockPosition(),
                        SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5F, 0.5F);
            } else {
                this.level().playSound(null, this.blockPosition(),
                        SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.HOSTILE, 1.0F, 1.2F);
            }

            if (amount <= 0) {
                // 护盾完全吸收，不传递给 super
                return true;
            }
        }

        return super.hurt(source, amount);
    }

    // ─── Tick ────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        this.aliveTicks++;

        if (!this.level().isClientSide) {
            // 护盾回复
            int ticksSinceHurt = this.tickCount - this.lastHurtTick;
            if (ticksSinceHurt >= SHIELD_REGEN_DELAY && getShield() < MAX_SHIELD) {
                setShield(getShield() + SHIELD_REGEN_RATE);
            }

            // BossBar 更新
            float totalMax = this.getMaxHealth() + MAX_SHIELD;
            float totalCurrent = this.getHealth() + getShield();
            bossBar.setProgress(Math.max(0, Math.min(1.0F, totalCurrent / totalMax)));

            // Boss bar: 根据护盾状态改变颜色
            if (getShield() > 0) {
                bossBar.setColor(BossEvent.BossBarColor.PURPLE);
            } else {
                bossBar.setColor(BossEvent.BossBarColor.RED);
            }

            // 5分钟超时 → 还原为 NexusCoreBlock + 锁定
            if (this.aliveTicks >= REVERT_TICKS) {
                revertToCore();
                return;
            }
        }

        // 客户端粒子
        if (this.level().isClientSide) {
            double cx = this.getX();
            double cy = this.getY() + 1.0;
            double cz = this.getZ();

            // 护盾粒子
            if (getShield() > 0 && this.tickCount % 2 == 0) {
                double angle = (this.tickCount % 40) * Math.PI * 2.0 / 40.0;
                double radius = 1.2;
                this.level().addParticle(ParticleTypes.END_ROD,
                        cx + Math.cos(angle) * radius, cy, cz + Math.sin(angle) * radius,
                        0, 0.02, 0);
            }

            // 核心粒子
            if (this.tickCount % 4 == 0) {
                this.level().addParticle(ParticleTypes.ENCHANT,
                        cx + (this.random.nextDouble() - 0.5) * 2,
                        cy + this.random.nextDouble(),
                        cz + (this.random.nextDouble() - 0.5) * 2,
                        0, -0.1, 0);
            }
        }
    }

    // ─── Revert / Death ──────────────────────────────────────────────

    /**
     * 还原为 NexusCoreBlock 并锁定。
     */
    private void revertToCore() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        NexusType type = NexusType.getById(nexusTypeId);
        if (type != null) {
            // 放回 NexusCoreBlock
            serverLevel.setBlock(corePosition, ModBlocks.NEXUS_CORE.get().defaultBlockState(), 3);
            BlockEntity be = serverLevel.getBlockEntity(corePosition);
            if (be instanceof com.huige233.transcend.block.NexusCoreBlockEntity coreBE) {
                coreBE.setNexusType(nexusTypeId);
                coreBE.setLockTicks(LOCK_TICKS);
            }

            // 广播消息
            for (ServerPlayer p : serverLevel.getServer().getPlayerList().getPlayers()) {
                p.sendSystemMessage(Component.translatable("msg.transcend.nexus_crystal_reverted")
                        .withStyle(ChatFormatting.GRAY));
            }
        }

        bossBar.removeAllPlayers();
        this.discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);

        if (this.level() instanceof ServerLevel serverLevel) {
            NexusType type = NexusType.getById(nexusTypeId);
            if (type != null) {
                // 触发枢纽摧毁效果
                ServerPlayer killer = null;
                if (source.getEntity() instanceof ServerPlayer sp) {
                    killer = sp;
                } else {
                    // 找最近的玩家
                    net.minecraft.world.entity.player.Player nearest = serverLevel.getNearestPlayer(this, 64);
                    if (nearest instanceof ServerPlayer sp2) killer = sp2;
                }
                if (killer != null) {
                    NexusManager.onNexusDestroyed(serverLevel, corePosition, killer, type);
                }
            }
            bossBar.removeAllPlayers();
        }
    }

    // ─── Persistence ─────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("NexusType", nexusTypeId);
        tag.putInt("CoreX", corePosition.getX());
        tag.putInt("CoreY", corePosition.getY());
        tag.putInt("CoreZ", corePosition.getZ());
        tag.putInt("AliveTicks", aliveTicks);
        tag.putFloat("Shield", getShield());
        tag.putInt("LastHurtTick", lastHurtTick);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        nexusTypeId = tag.getString("NexusType");
        corePosition = new BlockPos(tag.getInt("CoreX"), tag.getInt("CoreY"), tag.getInt("CoreZ"));
        aliveTicks = tag.getInt("AliveTicks");
        setShield(tag.getFloat("Shield"));
        lastHurtTick = tag.getInt("LastHurtTick");
        // 重新设置 boss bar 名字
        NexusType type = NexusType.getById(nexusTypeId);
        if (type != null) {
            bossBar.setName(Component.translatable(type.nameKey).withStyle(type.color)
                    .append(Component.literal(" Crystal").withStyle(ChatFormatting.WHITE)));
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        bossBar.removeAllPlayers();
        bossBar.setVisible(false);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }
}
