package com.huige233.transcend.entity.familiar;

import com.huige233.transcend.balance.BalanceConfig;
import com.huige233.transcend.client.magic.MagicCrystalHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 召唤式持久魔法助手:绑定到玩家 UUID 的小型灵体。
 *
 * <p>支持两种工作模式:
 * <ul>
 *   <li>{@code FOLLOW} (默认,Round 20 行为):跟随主人,执行被动 buff/拾取/警戒。</li>
 *   <li>{@code ASSIST} (Round 54 任务模式):锚定 work_center,在 work_radius 内自主完成
 *       搬运/守卫/采集/收割任务,主人离开依然工作。</li>
 * </ul>
 *
 * <p>4 种 type × 2 种模式 = 8 种行为分支,详见 {@link #executeBehavior(Player)}。
 *
 * <p>所有数值通过 {@link BalanceConfig#familiar} 数据驱动。
 */
public class TranscendFamiliar extends PathfinderMob {

    public enum FamiliarType {
        AETHER_WISP(0.85F, 0.95F, 0.6F),
        BLOOD_HOUND(0.85F, 0.10F, 0.10F),
        COSMIC_OWL(0.45F, 0.50F, 0.95F),
        TAINTED_IMP(0.50F, 0.10F, 0.55F);

        public final float r, g, b;

        FamiliarType(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    /** 工作模式:FOLLOW=R20 跟随,ASSIST=R54 任务模式。 */
    public enum TaskMode {
        FOLLOW, ASSIST;

        public TaskMode next() {
            return this == FOLLOW ? ASSIST : FOLLOW;
        }
    }

    private static final EntityDataAccessor<Integer> DATA_TYPE =
            SynchedEntityData.defineId(TranscendFamiliar.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(TranscendFamiliar.class,
                    EntityDataSerializers.OPTIONAL_UUID);
    /** Round 54: 任务模式 ordinal (0=FOLLOW, 1=ASSIST). */
    private static final EntityDataAccessor<Integer> DATA_TASK_MODE =
            SynchedEntityData.defineId(TranscendFamiliar.class, EntityDataSerializers.INT);
    /** Round 54: 工作锚点;Optional.empty 表示未设置(等同 FOLLOW)。 */
    private static final EntityDataAccessor<Optional<BlockPos>> DATA_WORK_CENTER =
            SynchedEntityData.defineId(TranscendFamiliar.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    /** Round 54: 工作半径(方块),受 BalanceConfig 上限约束。 */
    private static final EntityDataAccessor<Integer> DATA_WORK_RADIUS =
            SynchedEntityData.defineId(TranscendFamiliar.class, EntityDataSerializers.INT);

    private int despawnTimer = 0;
    private int behaviorCooldown = 0;

    public TranscendFamiliar(EntityType<? extends TranscendFamiliar> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
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
        this.entityData.define(DATA_TASK_MODE, TaskMode.FOLLOW.ordinal());
        this.entityData.define(DATA_WORK_CENTER, Optional.empty());
        this.entityData.define(DATA_WORK_RADIUS, BalanceConfig.get().familiar.task_radius_default);
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
        tag.putInt("task_mode", this.entityData.get(DATA_TASK_MODE));
        this.entityData.get(DATA_WORK_CENTER).ifPresent(p -> {
            tag.putInt("work_center_x", p.getX());
            tag.putInt("work_center_y", p.getY());
            tag.putInt("work_center_z", p.getZ());
        });
        tag.putInt("work_radius", this.entityData.get(DATA_WORK_RADIUS));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_TYPE, tag.getInt("familiar_type"));
        if (tag.hasUUID("owner_uuid")) {
            this.entityData.set(DATA_OWNER, Optional.of(tag.getUUID("owner_uuid")));
        }
        // Round 54 字段 — 缺失时保持默认(向后兼容旧存档)
        if (tag.contains("task_mode")) {
            this.entityData.set(DATA_TASK_MODE, tag.getInt("task_mode"));
        }
        if (tag.contains("work_center_x")) {
            this.entityData.set(DATA_WORK_CENTER, Optional.of(new BlockPos(
                    tag.getInt("work_center_x"),
                    tag.getInt("work_center_y"),
                    tag.getInt("work_center_z"))));
        }
        if (tag.contains("work_radius")) {
            this.entityData.set(DATA_WORK_RADIUS, tag.getInt("work_radius"));
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

    public Optional<UUID> getOwnerUUID() {
        return this.entityData.get(DATA_OWNER);
    }

    public Player findOwner() {
        return this.entityData.get(DATA_OWNER)
                .map(uuid -> this.level().getPlayerByUUID(uuid))
                .orElse(null);
    }

    public TaskMode getTaskMode() {
        int ord = this.entityData.get(DATA_TASK_MODE);
        return ord == 1 ? TaskMode.ASSIST : TaskMode.FOLLOW;
    }

    public void setTaskMode(TaskMode mode) {
        this.entityData.set(DATA_TASK_MODE, mode.ordinal());
    }

    public Optional<BlockPos> getWorkCenter() {
        return this.entityData.get(DATA_WORK_CENTER);
    }

    public void setWorkCenter(BlockPos pos) {
        this.entityData.set(DATA_WORK_CENTER, Optional.ofNullable(pos));
    }

    public int getWorkRadius() {
        return this.entityData.get(DATA_WORK_RADIUS);
    }

    public void setWorkRadius(int radius) {
        BalanceConfig.FamiliarBalance f = BalanceConfig.get().familiar;
        this.entityData.set(DATA_WORK_RADIUS, Math.max(f.task_radius_step, Math.min(f.task_radius_max, radius)));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            spawnTypeParticles();
            return;
        }

        Player owner = findOwner();
        TaskMode mode = getTaskMode();

        // 任务模式下,owner 离线不会消散 — 这是"持续基础设施"的核心价值
        if (owner == null && mode == TaskMode.FOLLOW) {
            despawnTimer++;
            if (despawnTimer >= BalanceConfig.get().familiar.despawn_timer) {
                this.discard();
            }
            return;
        }
        despawnTimer = 0;

        // FOLLOW 模式 → 跟随 owner;ASSIST 模式 → 围绕 work_center 巡游
        if (mode == TaskMode.FOLLOW && owner != null) {
            followOwner(owner);
        } else if (mode == TaskMode.ASSIST) {
            anchorToWorkCenter(owner);
        }

        // 行为节流
        behaviorCooldown--;
        if (behaviorCooldown <= 0) {
            behaviorCooldown = BalanceConfig.get().familiar.behavior_interval;
            executeBehavior(owner);
        }
    }

    /** FOLLOW 模式:远 16 格传送,远 4 格寻路。 */
    private void followOwner(Player owner) {
        double distSq = this.distanceToSqr(owner);
        if (distSq > 256.0) {
            this.teleportTo(
                    owner.getX() + (this.random.nextDouble() - 0.5) * 3,
                    owner.getY() + 0.5,
                    owner.getZ() + (this.random.nextDouble() - 0.5) * 3);
        } else if (distSq > 16.0) {
            this.getNavigation().moveTo(owner.getX(), owner.getY(), owner.getZ(), 1.0);
        }
    }

    /** ASSIST 模式:超出 work_center 一定距离时返回锚点;无锚点回退到跟随 owner。 */
    private void anchorToWorkCenter(Player owner) {
        Optional<BlockPos> center = getWorkCenter();
        if (center.isEmpty()) {
            if (owner != null) followOwner(owner);
            return;
        }
        BlockPos c = center.get();
        BalanceConfig.FamiliarBalance f = BalanceConfig.get().familiar;
        int radius = getWorkRadius();
        double leashSq = (double)(radius * f.guard_return_threshold_mult) * (radius * f.guard_return_threshold_mult);
        if (this.blockPosition().distSqr(c) > leashSq) {
            this.getNavigation().moveTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 1.1);
        }
    }

    /** 行为派发:type × mode 决定具体行为。 */
    private void executeBehavior(Player owner) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        TaskMode mode = getTaskMode();
        FamiliarType type = getFamiliarType();

        if (mode == TaskMode.ASSIST) {
            executeAssistBehavior(sl, owner, type);
        } else {
            // R20 行为(向后兼容)
            if (owner == null) return;
            switch (type) {
                case AETHER_WISP -> behaviorAetherWisp(sl, owner);
                case BLOOD_HOUND -> behaviorBloodHound(sl, owner);
                case COSMIC_OWL -> behaviorCosmicOwl(sl, owner);
                case TAINTED_IMP -> behaviorTaintedImp(sl, owner);
            }
        }
    }

    /** Round 54: ASSIST 模式行为派发。 */
    private void executeAssistBehavior(ServerLevel sl, Player owner, FamiliarType type) {
        switch (type) {
            case AETHER_WISP -> taskTransport(sl);
            case BLOOD_HOUND -> taskGuard(sl, owner);
            case COSMIC_OWL -> taskCollect(sl, owner);
            case TAINTED_IMP -> taskHarvest(sl, owner);
        }
    }

    // ============ R20 FOLLOW 行为 ============

    private void behaviorAetherWisp(ServerLevel sl, Player owner) {
        AABB search = this.getBoundingBox().inflate(BalanceConfig.get().familiar.aether_pickup_radius);
        var items = sl.getEntitiesOfClass(ItemEntity.class, search,
                ie -> ie.isAlive() && !ie.hasPickUpDelay());
        for (ItemEntity ie : items) {
            ItemStack stack = ie.getItem();
            if (owner.getInventory().add(stack.copy())) {
                ie.discard();
                sl.playSound(null, this.blockPosition(),
                        SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.4F, 1.8F);
                break;
            }
        }
    }

    private void behaviorBloodHound(ServerLevel sl, Player owner) {
        BalanceConfig.FamiliarBalance f = BalanceConfig.get().familiar;
        AABB search = this.getBoundingBox().inflate(f.blood_search_radius);
        LivingEntity target = findNearestEnemy(sl, owner, search);
        if (target == null) return;
        if (this.distanceToSqr(target) < 4.0) {
            target.hurt(this.damageSources().mobAttack(this), f.blood_attack_damage);
            sl.playSound(null, target.blockPosition(),
                    SoundEvents.WOLF_GROWL, SoundSource.HOSTILE, 0.8F, 1.5F);
        } else {
            this.getNavigation().moveTo(target, 1.2);
        }
    }

    private void behaviorCosmicOwl(ServerLevel sl, Player owner) {
        BalanceConfig.FamiliarBalance f = BalanceConfig.get().familiar;
        long time = sl.getDayTime() % 24000;
        boolean isNight = time > 13000 && time < 23000;
        if (isNight || sl.dimensionType().hasFixedTime()) {
            owner.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION, f.cosmic_night_vision_duration, 0, true, false));
        }
        if (owner.getDeltaMovement().y < -0.5) {
            owner.addEffect(new MobEffectInstance(
                    MobEffects.SLOW_FALLING, f.cosmic_slow_falling_duration, 0, true, false));
        }
    }

    private void behaviorTaintedImp(ServerLevel sl, Player owner) {
        BalanceConfig.FamiliarBalance f = BalanceConfig.get().familiar;
        AABB search = this.getBoundingBox().inflate(f.tainted_search_radius);
        var enemies = sl.getEntitiesOfClass(LivingEntity.class, search,
                e -> e != owner && e != this && (e instanceof Enemy ||
                        (e instanceof Mob m && !m.isAlliedTo(owner))) && e.isAlive());
        for (LivingEntity e : enemies) {
            if (e.getHealth() <= 1.5F) continue;
            e.hurt(this.damageSources().magic(), f.tainted_damage);
            int current = MagicCrystalHelper.getInnateMana(owner);
            int max = MagicCrystalHelper.getInnateMaxMana(owner);
            MagicCrystalHelper.setInnateMana(owner, Math.min(max, current + f.tainted_mana_gain));
        }
    }

    // ============ R54 ASSIST 任务行为 ============

    /**
     * AETHER_WISP TRANSPORT:
     * 在 work_radius 内拾取掉落物 → 投递到 work_center 周围的 IItemHandler。
     * 无可用 container → 回退到 owner 背包(若 owner 在线)。
     */
    private void taskTransport(ServerLevel sl) {
        BlockPos center = getWorkCenter().orElse(this.blockPosition());
        int radius = getWorkRadius();
        AABB scan = new AABB(center).inflate(radius);
        var items = sl.getEntitiesOfClass(ItemEntity.class, scan,
                ie -> ie.isAlive() && !ie.hasPickUpDelay());
        if (items.isEmpty()) return;

        BalanceConfig.FamiliarBalance f = BalanceConfig.get().familiar;
        int budget = f.task_transport_per_tick;

        for (ItemEntity ie : items) {
            if (budget <= 0) break;
            ItemStack stack = ie.getItem();
            ItemStack remaining = depositToNearbyContainer(sl, center, stack);
            if (remaining.getCount() < stack.getCount()) {
                budget--;
                if (remaining.isEmpty()) {
                    ie.discard();
                } else {
                    ie.setItem(remaining);
                }
                sl.playSound(null, this.blockPosition(),
                        SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.3F, 1.6F);
            } else {
                // 没有 container 接收 → 回退到 owner
                Player owner = findOwner();
                if (owner != null && owner.getInventory().add(stack.copy())) {
                    ie.discard();
                    budget--;
                }
            }
        }
    }

    /**
     * BLOOD_HOUND GUARD:
     * 锚定 work_center,只在 work_radius 内主动攻击敌怪;超出范围不追击。
     */
    private void taskGuard(ServerLevel sl, Player owner) {
        BalanceConfig.FamiliarBalance f = BalanceConfig.get().familiar;
        BlockPos center = getWorkCenter().orElse(this.blockPosition());
        int radius = getWorkRadius();
        AABB scan = new AABB(center).inflate(radius);

        var enemies = sl.getEntitiesOfClass(LivingEntity.class, scan,
                e -> e != this && e != owner && e.isAlive() &&
                        (e instanceof Enemy ||
                                (e instanceof Mob m && (owner == null || !m.isAlliedTo(owner)))));
        if (enemies.isEmpty()) return;

        LivingEntity target = enemies.stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(this), b.distanceToSqr(this)))
                .orElse(null);
        if (target == null) return;

        if (this.distanceToSqr(target) < 4.0) {
            target.hurt(this.damageSources().mobAttack(this), f.blood_attack_damage);
            sl.playSound(null, target.blockPosition(),
                    SoundEvents.WOLF_GROWL, SoundSource.HOSTILE, 0.8F, 1.6F);
        } else {
            this.getNavigation().moveTo(target, 1.2);
        }
    }

    /**
     * COSMIC_OWL COLLECT:
     * 大范围(radius × 1.5)拾取掉落物 → 投递到 work_center 周围 container。
     * Drygmy 风简化版,专注于 mob 死亡掉落物的回收。
     */
    private void taskCollect(ServerLevel sl, Player owner) {
        BalanceConfig.FamiliarBalance f = BalanceConfig.get().familiar;
        BlockPos center = getWorkCenter().orElse(this.blockPosition());
        double r = getWorkRadius() * f.cosmic_collect_radius_mult;
        AABB scan = new AABB(center).inflate(r);

        var items = sl.getEntitiesOfClass(ItemEntity.class, scan,
                ie -> ie.isAlive() && !ie.hasPickUpDelay());
        if (items.isEmpty()) return;

        int budget = f.task_transport_per_tick;
        for (ItemEntity ie : items) {
            if (budget <= 0) break;
            ItemStack stack = ie.getItem();
            ItemStack remaining = depositToNearbyContainer(sl, center, stack);
            if (remaining.isEmpty()) {
                ie.discard();
                budget--;
            } else if (remaining.getCount() < stack.getCount()) {
                ie.setItem(remaining);
                budget--;
            } else if (owner != null && owner.getInventory().add(stack.copy())) {
                ie.discard();
                budget--;
            }
        }
    }

    /**
     * TAINTED_IMP HARVEST:
     * 扫 work_radius 内成熟 CropBlock / NetherWartBlock → 砍掉 + 重种。
     * 每次操作扣 owner 1 mana(数据驱动)。掉落物送 work_center container。
     * <p>owner 离线时不工作,避免"无代价自动收割"漏洞。
     */
    private void taskHarvest(ServerLevel sl, Player owner) {
        // owner 离线 → 不收割(mana 代价是平衡基石)
        if (owner == null) return;

        BalanceConfig.FamiliarBalance f = BalanceConfig.get().familiar;
        BlockPos center = getWorkCenter().orElse(this.blockPosition());
        int radius = getWorkRadius();

        BlockPos target = findMatureCrop(sl, center, radius);
        if (target == null) return;

        // 扣 owner mana(若失败则跳过本次收割)
        if (f.tainted_harvest_mana_cost > 0) {
            int curr = MagicCrystalHelper.getInnateMana(owner);
            if (curr < f.tainted_harvest_mana_cost) return;
            MagicCrystalHelper.setInnateMana(owner, curr - f.tainted_harvest_mana_cost);
        }

        // 移动到目标(如果远)
        if (this.blockPosition().distSqr(target) > 4.0) {
            this.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
            return;
        }

        // 收割 + 重种
        BlockState state = sl.getBlockState(target);
        List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(state, sl, target, null);
        if (state.getBlock() instanceof CropBlock crop) {
            sl.setBlockAndUpdate(target, crop.getStateForAge(0));
        } else if (state.getBlock() instanceof NetherWartBlock) {
            sl.setBlockAndUpdate(target, state.getBlock().defaultBlockState());
        } else {
            sl.removeBlock(target, false);
        }

        // 投递掉落物
        for (ItemStack drop : drops) {
            ItemStack remaining = depositToNearbyContainer(sl, center, drop);
            if (!remaining.isEmpty()) {
                // 没收纳处 → 掉到地上
                sl.addFreshEntity(new ItemEntity(sl,
                        target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, remaining));
            }
        }

        sl.playSound(null, target,
                SoundEvents.CROP_BREAK, SoundSource.NEUTRAL, 0.5F, 1.2F);
    }

    // ============ 共用工具方法 ============

    /** 在 center 周围 1 格内查找能接收 stack 的 IItemHandler;返回剩余无法插入的部分。 */
    private static ItemStack depositToNearbyContainer(ServerLevel sl, BlockPos center, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack remaining = stack.copy();
        for (Direction d : Direction.values()) {
            BlockPos p = center.relative(d);
            BlockEntity be = sl.getBlockEntity(p);
            if (be == null) continue;
            IItemHandler h = be.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).orElse(null);
            if (h == null) continue;
            remaining = ItemHandlerHelper.insertItemStacked(h, remaining, false);
            if (remaining.isEmpty()) return ItemStack.EMPTY;
        }
        // 也扫 center 本身(花架/箱子放在锚点位置的情况)
        BlockEntity beSelf = sl.getBlockEntity(center);
        if (beSelf != null) {
            IItemHandler h = beSelf.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
            if (h != null) remaining = ItemHandlerHelper.insertItemStacked(h, remaining, false);
        }
        return remaining;
    }

    /** 在 center ± radius 立方体内查找最近的成熟农作物。 */
    private static BlockPos findMatureCrop(ServerLevel sl, BlockPos center, int radius) {
        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState st = sl.getBlockState(m);
                    if (isMatureHarvestable(st)) {
                        double d = center.distSqr(m);
                        if (d < bestDistSq) {
                            bestDistSq = d;
                            best = m.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    private static boolean isMatureHarvestable(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (state.getBlock() instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE;
        }
        return false;
    }

    private LivingEntity findNearestEnemy(ServerLevel sl, Player owner, AABB search) {
        var enemies = sl.getEntitiesOfClass(LivingEntity.class, search,
                e -> e != owner && e != this && (e instanceof Enemy ||
                        (e instanceof Mob m && !m.isAlliedTo(owner))) && e.isAlive());
        return enemies.stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(this), b.distanceToSqr(this)))
                .orElse(null);
    }

    // ============ 玩家交互 (Round 54) ============

    /**
     * mobInteract 配置面板:
     * <ul>
     *   <li>空手右键 → 切换 task_mode (FOLLOW ↔ ASSIST)</li>
     *   <li>潜行 + 空手右键 → 设置当前位置为 work_center;若 ASSIST 模式下已有 → 清除</li>
     *   <li>magic_crystal 右键 → 增加 work_radius (循环 step → max)</li>
     *   <li>潜行 + magic_crystal 右键 → 重置半径并清除 work_center</li>
     * </ul>
     * 仅 owner 可配置。
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        // 仅 owner 可配置
        if (!getOwnerUUID().map(u -> u.equals(player.getUUID())).orElse(false)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        boolean sneaking = player.isShiftKeyDown();

        // magic_crystal 调整半径
        if (held.getItem() == com.huige233.transcend.init.ModItems.magic_crystal.get()) {
            BalanceConfig.FamiliarBalance f = BalanceConfig.get().familiar;
            if (sneaking) {
                setWorkRadius(f.task_radius_default);
                setWorkCenter(null);
                sendActionBar(player, Component.translatable("familiar.transcend.task.reset")
                        .withStyle(ChatFormatting.GRAY));
            } else {
                int next = getWorkRadius() + f.task_radius_step;
                // 超过上限 → 循环回最小步进值
                if (next > f.task_radius_max) next = f.task_radius_step;
                setWorkRadius(next);
                sendActionBar(player, Component.translatable("familiar.transcend.task.radius",
                        getWorkRadius()).withStyle(ChatFormatting.AQUA));
            }
            return InteractionResult.CONSUME;
        }

        // 空手交互
        if (held.isEmpty()) {
            if (sneaking) {
                // 设置/清除 work_center
                if (getWorkCenter().isPresent()) {
                    setWorkCenter(null);
                    sendActionBar(player, Component.translatable("familiar.transcend.task.center_cleared")
                            .withStyle(ChatFormatting.YELLOW));
                } else {
                    setWorkCenter(this.blockPosition());
                    sendActionBar(player, Component.translatable("familiar.transcend.task.center_set",
                                    this.blockPosition().getX(), this.blockPosition().getY(), this.blockPosition().getZ())
                            .withStyle(ChatFormatting.GREEN));
                }
            } else {
                // 切换 mode
                TaskMode next = getTaskMode().next();
                setTaskMode(next);
                String key = next == TaskMode.ASSIST
                        ? "familiar.transcend.task.mode_assist"
                        : "familiar.transcend.task.mode_follow";
                sendActionBar(player, Component.translatable(key)
                        .withStyle(next == TaskMode.ASSIST ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY));
            }
            this.level().playSound(null, this.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.6F, 1.6F);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    private static void sendActionBar(Player player, Component text) {
        player.displayClientMessage(text, true);
    }

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
        // ASSIST 模式额外强调粒子(让玩家直观分辨工作中的灵宠)
        if (getTaskMode() == TaskMode.ASSIST && this.random.nextFloat() < 0.25F) {
            this.level().addParticle(
                    new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 0.7F),
                    this.getX() + (this.random.nextDouble() - 0.5) * 0.8,
                    this.getY() + 0.6 + this.random.nextDouble() * 0.3,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.8,
                    0.0, 0.0, 0.0);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (source.getEntity() instanceof Player p &&
                this.getOwnerUUID().map(u -> u.equals(p.getUUID())).orElse(false)) {
            return false;
        }
        return super.hurt(source, amount);
    }
}
