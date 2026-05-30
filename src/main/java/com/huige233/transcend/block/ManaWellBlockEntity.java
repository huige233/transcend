package com.huige233.transcend.block;

import com.huige233.transcend.block.mana.ManaCondenserBlockEntity;
import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.init.ModBlocks;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.world.mana.ChunkManaSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 魔力井方块实体 — 处理魔力抽取、缓冲、结晶逻辑。
 *
 * 服务端Tick逻辑：
 * 1. 每 EXTRACT_INTERVAL (200) tick 从区块抽取 EXTRACT_AMOUNT (5.0) 魔力
 * 2. 抽取的魔力存入内部缓冲区
 * 3. 缓冲区满 CRYSTAL_COST (10.0) 时产出1个魔力水晶
 * 4. 产出的水晶存入内部存储（最多16个），满则弹出掉落
 * 5. 区块魔力 < MIN_EXTRACT (1.0) 时停止工作
 *
 * 客户端Tick：粒子特效
 */
public class ManaWellBlockEntity extends BlockEntity {

    /** 抽取间隔（tick） — 默认值，Resonance 模式会改用 {@link #RESONANCE_EXTRACT_INTERVAL}。 */
    private static final int EXTRACT_INTERVAL = 200;
    /**
     * R61: Resonance 模式抽取间隔（tick）。
     * 当 4 邻面 NSEW 全部为 ancient_crystal 时启用。等价于 ×1.33 的吞吐倍率，
     * 与高级倍率上限叠加为高投入阶段（8 块水晶）的有效抽取效率提升。
     */
    private static final int RESONANCE_EXTRACT_INTERVAL = 150;
    /** 每次抽取的魔力量（基础值，会按区块分级乘修正） */
    private static final float EXTRACT_AMOUNT = 5.0F;
    /** 产出1个水晶所需的缓冲魔力 */
    private static final float CRYSTAL_COST = 10.0F;
    /** 内部水晶存储上限 */
    private static final int MAX_STORED_CRYSTALS = 16;

    /**
     * R57 → R72: 升级方块抽取速率上限。
     * <p>R57 设 3.0× 对应设计稿"9 mana/min hard cap"。R72 玩家反馈完全体环境抽取应为 ~8 mana/min，
     * 故 3.0 → 2.67（基础 30 CM/min × 2.67 = 80 CM/min = 8 crystal/min）。
     */
    private static final float MAX_PRODUCTION_MULTIPLIER = 2.67F;
    /**
     * R61 → R72: Resonance 高投入阶段的抽取倍率上限。
     * <p>R61 设 5.0× × 1.33 cadence ≈ 20 crystal/min。R72 玩家完全体目标 ~8。
     * 改为 2.4× × 1.33 cadence ≈ 9.6 crystal/min（保留 Resonance 微小奖励，不再翻倍）。
     */
    private static final float RESONANCE_MAX_PRODUCTION_MULTIPLIER = 2.40F;
    /**
     * R61: Resonance 阵型激活带来的固定加成。
     * 4 块 ancient_crystal 在 NSEW 已经各自 +0.25（横向 ancient 共 +1.0），
     * 此项再 +1.0 是阵型自身的"共振红利"，鼓励玩家完成完整图案。
     */
    private static final float RESONANCE_BONUS = 1.0F;
    /** R57: 4 邻面同种水晶方块的累加上限。 */
    private static final int MAX_HORIZONTAL_BOOSTERS = 4;

    private float manaBuffer = 0.0F;
    private int storedCrystals = 0;
    private int extractTimer = 0;
    private boolean working = false;
    /** R61: Resonance 阵型激活状态，每 tick 重算并同步给客户端供 UI 显示。 */
    private boolean resonance = false;

    // 用于客户端显示（通过 sync tag 同步）
    private float clientChunkMana = ChunkManaSavedData.DEFAULT_MANA;

    public ManaWellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_WELL_BE.get(), pos, state);
    }

    // ─── 服务端逻辑 ─────────────────────────────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaWellBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        be.extractTimer++;
        ChunkPos chunkPos = new ChunkPos(pos);
        ChunkManaSavedData manaData = ChunkManaSavedData.get(serverLevel);
        float currentMana = manaData.getMana(chunkPos);

        // R56: 区块分级判定 — Exhausted 或低于抽取下限则停工
        ChunkManaSavedData.Tier tier = manaData.getTier(chunkPos);
        float floor = manaData.getExtractFloor(chunkPos);
        be.working = tier != ChunkManaSavedData.Tier.EXHAUSTED && currentMana > floor;

        // R61: 每 tick 重算 Resonance 状态（玩家可能放置/破坏阵型方块）
        boolean prevResonance = be.resonance;
        be.resonance = isResonanceActive(level, pos);
        int interval = be.resonance ? RESONANCE_EXTRACT_INTERVAL : EXTRACT_INTERVAL;

        if (!be.working) {
            // 每4秒同步一次状态给客户端（即使停止也要更新显示）
            if (be.extractTimer % 80 == 0 || prevResonance != be.resonance) {
                be.clientChunkMana = currentMana;
                be.syncToClient();
            }
            return;
        }

        // 抽取魔力
        if (be.extractTimer >= interval) {
            be.extractTimer = 0;

            // R57: 升级方块乘数（设计稿 D3） + R61 Resonance 加成
            float upgradeMult = computeProductionMultiplier(level, pos);
            float requested = EXTRACT_AMOUNT * upgradeMult;

            // R56: consumeManaSafe 自带分级倍率与下限保护
            float extracted = manaData.consumeManaSafe(chunkPos, requested);
            if (extracted > 0) {
                be.manaBuffer += extracted;

                // 播放抽取音效
                level.playSound(null, pos, SoundEvents.BEACON_AMBIENT,
                        SoundSource.BLOCKS, 0.5F, 1.2F + level.random.nextFloat() * 0.3F);

                // R57: 检测相邻 condenser，若有则改为直接注入网络缓冲（不产水晶）
                ManaCondenserBlockEntity condenser = findAdjacentCondenser(level, pos);

                while (be.manaBuffer >= CRYSTAL_COST) {
                    be.manaBuffer -= CRYSTAL_COST;
                    if (condenser != null) {
                        // 直连网络：1 crystal = 1 item mana
                        condenser.receiveFromWell(1);
                    } else if (be.storedCrystals < MAX_STORED_CRYSTALS) {
                        be.storedCrystals++;
                    } else {
                        be.ejectCrystal(serverLevel, pos);
                    }
                    level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME,
                            SoundSource.BLOCKS, 1.0F, 0.8F + level.random.nextFloat() * 0.4F);
                }

                be.setChanged();
            }

            // 同步到客户端
            be.clientChunkMana = manaData.getMana(chunkPos);
            be.syncToClient();
        } else if (prevResonance != be.resonance) {
            // R61: 阵型变化但没到抽取周期 — 主动同步一次状态给客户端
            be.syncToClient();
        }
    }

    /**
     * R57 + R61: 检测 mana_well 周围的升级方块，按设计稿 D3 表计算速率乘数。
     *
     * <p>方块位置约定（标准 4 邻）：
     * <ul>
     *   <li>正下方 (pos.below())：magic_crystal_block +25% / concentrated_crystal_block +60% / ancient_crystal +100%</li>
     *   <li>4 水平邻 (NSEW)：每个 magic_crystal_block +10% (max 4×) / 每个 concentrated_crystal_block +20% (max 4×)</li>
     * </ul>
     *
     * <p><b>R61 Resonance 阵型</b>（4 邻面 NSEW 全为 ancient_crystal 时激活）：
     * <ul>
     *   <li>4 横向 ancient_crystal 各 +25% = 共 +100%</li>
     *   <li>Resonance 红利 +100%（{@link #RESONANCE_BONUS}）</li>
     *   <li>4 对角 (NE/NW/SE/SW) 每个 magic_crystal_block 再 +25%（最多 +100%）</li>
     *   <li>抽取间隔从 200 → 150 ticks（serverTick 处理）</li>
     *   <li>上限提升 3.0× → 5.0×（{@link #RESONANCE_MAX_PRODUCTION_MULTIPLIER}）</li>
     * </ul>
     *
     * <p>阵型对应玩家投入：8 块水晶（4 ancient + 4 magic_crystal_block）+ 1 ancient 下方 = 9 块。
     * 满配理论极值：1.0 + 1.0 (below ancient) + 1.0 (4 ancient horiz) + 1.0 (resonance bonus) + 1.0 (4 diag magic) = 5.0×。
     */
    private static float computeProductionMultiplier(Level level, BlockPos pos) {
        float mult = 1.0F;
        // 正下方
        var belowBlock = level.getBlockState(pos.below()).getBlock();
        if (belowBlock == ModBlocks.ANCIENT_CRYSTAL.get())                mult += 1.0F;
        else if (belowBlock == ModBlocks.CONCENTRATED_CRYSTAL_BLOCK.get()) mult += 0.6F;
        else if (belowBlock == ModBlocks.MAGIC_CRYSTAL_BLOCK.get())        mult += 0.25F;

        // R61: Resonance 阵型走专门加成路径
        if (isResonanceActive(level, pos)) {
            // 4 横向 ancient_crystal 各 +0.25（与下方 ancient 解耦，单独计入）
            mult += 4 * 0.25F;
            // Resonance 红利
            mult += RESONANCE_BONUS;
            // 4 对角 magic_crystal_block 每个 +0.25
            var magic = ModBlocks.MAGIC_CRYSTAL_BLOCK.get();
            int diagMagic = 0;
            if (level.getBlockState(pos.north().east()).getBlock() == magic) diagMagic++;
            if (level.getBlockState(pos.north().west()).getBlock() == magic) diagMagic++;
            if (level.getBlockState(pos.south().east()).getBlock() == magic) diagMagic++;
            if (level.getBlockState(pos.south().west()).getBlock() == magic) diagMagic++;
            mult += 0.25F * diagMagic;
            return Math.min(mult, RESONANCE_MAX_PRODUCTION_MULTIPLIER);
        }

        // 标准 4 水平邻
        int magicCount = 0, concCount = 0;
        for (Direction dir : new Direction[]{ Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST }) {
            var nb = level.getBlockState(pos.relative(dir)).getBlock();
            if (nb == ModBlocks.MAGIC_CRYSTAL_BLOCK.get())        magicCount++;
            else if (nb == ModBlocks.CONCENTRATED_CRYSTAL_BLOCK.get()) concCount++;
        }
        magicCount = Math.min(magicCount, MAX_HORIZONTAL_BOOSTERS);
        concCount = Math.min(concCount, MAX_HORIZONTAL_BOOSTERS);
        mult += 0.10F * magicCount;
        mult += 0.20F * concCount;

        return Math.min(mult, MAX_PRODUCTION_MULTIPLIER);
    }

    /**
     * R61: Resonance 阵型判定 — 4 邻面 NSEW 全部为 ancient_crystal 时激活。
     * 不要求下方/对角条件，那些只是叠加加成。
     */
    private static boolean isResonanceActive(Level level, BlockPos pos) {
        var ancient = ModBlocks.ANCIENT_CRYSTAL.get();
        return level.getBlockState(pos.north()).getBlock() == ancient
            && level.getBlockState(pos.south()).getBlock() == ancient
            && level.getBlockState(pos.east()).getBlock() == ancient
            && level.getBlockState(pos.west()).getBlock() == ancient;
    }

    /**
     * R57: 查找 6 邻面任意位置的 ManaCondenser，用于直连网络模式。
     */
    @Nullable
    private static ManaCondenserBlockEntity findAdjacentCondenser(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockEntity nb = level.getBlockEntity(pos.relative(dir));
            if (nb instanceof ManaCondenserBlockEntity cond) return cond;
        }
        return null;
    }

    // ─── 客户端粒子 ─────────────────────────────────────────────────

    public static void clientTick(Level level, BlockPos pos, BlockState state, ManaWellBlockEntity be) {
        if (!be.working) return;

        // 旋转上升的魔力粒子
        int tick = (int) (level.getGameTime() % 360);
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.8;
        double cz = pos.getZ() + 0.5;

        if (tick % 4 == 0) {
            double angle = tick * 0.15;
            double radius = 0.4 + Math.sin(tick * 0.05) * 0.15;
            double px = cx + Math.cos(angle) * radius;
            double pz = cz + Math.sin(angle) * radius;
            level.addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    px, cy + Math.random() * 0.5, pz,
                    0, 0.05, 0);
        }

        if (tick % 8 == 0) {
            // 从远处飘向井口的粒子（模拟吸取环境魔力）
            double offX = (level.random.nextDouble() - 0.5) * 4.0;
            double offZ = (level.random.nextDouble() - 0.5) * 4.0;
            level.addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT,
                    cx + offX, cy + 1.0 + level.random.nextDouble(), cz + offZ,
                    -offX * 0.05, -0.02, -offZ * 0.05);
        }

        // 井口上方的端棒粒子（浓度指示）
        if (tick % 12 == 0 && be.clientChunkMana > 50) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    cx, cy + 1.2, cz,
                    (level.random.nextDouble() - 0.5) * 0.02, 0.03, (level.random.nextDouble() - 0.5) * 0.02);
        }
    }

    // ─── 辅助方法 ────────────────────────────────────────────────────

    private void ejectCrystal(ServerLevel level, BlockPos pos) {
        ItemStack crystal = new ItemStack(ModItems.magic_crystal.get(), 1);
        ItemEntity item = new ItemEntity(level,
                pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, crystal);
        item.setDeltaMovement(
                (level.random.nextDouble() - 0.5) * 0.1,
                0.2,
                (level.random.nextDouble() - 0.5) * 0.1);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }

    /** 方块被破坏时掉落存储的水晶 */
    public void dropContents() {
        if (storedCrystals > 0 && level instanceof ServerLevel sl) {
            ItemStack crystals = new ItemStack(ModItems.magic_crystal.get(), storedCrystals);
            ItemEntity item = new ItemEntity(sl,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, crystals);
            item.setDefaultPickUpDelay();
            sl.addFreshEntity(item);
            storedCrystals = 0;
        }
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ─── 公开查询接口 ────────────────────────────────────────────────

    public float getChunkManaDisplay() {
        if (level instanceof ServerLevel sl) {
            return ChunkManaSavedData.get(sl).getMana(new ChunkPos(worldPosition));
        }
        return clientChunkMana;
    }

    public float getManaBuffer() { return manaBuffer; }
    public int getStoredCrystals() { return storedCrystals; }
    public boolean isWorking() { return working; }
    /** R61: 当前是否处于 Resonance 阵型激活状态（已由 serverTick 同步给客户端）。 */
    public boolean isResonance() { return resonance; }
    /** R61: 当前生效的抽取间隔（用于 UI / 调试显示）。 */
    public int getCurrentInterval() { return resonance ? RESONANCE_EXTRACT_INTERVAL : EXTRACT_INTERVAL; }
    /** R61: 当前生效的产能倍率上限。 */
    public float getCurrentMultiplierCap() { return resonance ? RESONANCE_MAX_PRODUCTION_MULTIPLIER : MAX_PRODUCTION_MULTIPLIER; }

    // ─── NBT 序列化 ─────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("ManaBuffer", manaBuffer);
        tag.putInt("StoredCrystals", storedCrystals);
        tag.putInt("ExtractTimer", extractTimer);
        tag.putBoolean("Working", working);
        tag.putBoolean("Resonance", resonance);
        tag.putFloat("ClientChunkMana", clientChunkMana);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        manaBuffer = tag.getFloat("ManaBuffer");
        storedCrystals = tag.getInt("StoredCrystals");
        extractTimer = tag.getInt("ExtractTimer");
        working = tag.getBoolean("Working");
        resonance = tag.getBoolean("Resonance");
        clientChunkMana = tag.getFloat("ClientChunkMana");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putBoolean("Working", working);
        tag.putBoolean("Resonance", resonance);
        tag.putFloat("ClientChunkMana", clientChunkMana);
        tag.putFloat("ManaBuffer", manaBuffer);
        tag.putInt("StoredCrystals", storedCrystals);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
