package com.huige233.transcend.block;

import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.world.mana.ChunkManaSavedData;
import net.minecraft.core.BlockPos;
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

    /** 抽取间隔（tick） */
    private static final int EXTRACT_INTERVAL = 200;
    /** 每次抽取的魔力量 */
    private static final float EXTRACT_AMOUNT = 5.0F;
    /** 产出1个水晶所需的缓冲魔力 */
    private static final float CRYSTAL_COST = 10.0F;
    /** 区块最低可抽取阈值 */
    private static final float MIN_EXTRACT = 1.0F;
    /** 内部水晶存储上限 */
    private static final int MAX_STORED_CRYSTALS = 16;

    private float manaBuffer = 0.0F;
    private int storedCrystals = 0;
    private int extractTimer = 0;
    private boolean working = false;

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

        // 检查是否可以工作
        be.working = currentMana >= MIN_EXTRACT;

        if (!be.working) {
            // 每4秒同步一次状态给客户端（即使停止也要更新显示）
            if (be.extractTimer % 80 == 0) {
                be.clientChunkMana = currentMana;
                be.syncToClient();
            }
            return;
        }

        // 抽取魔力
        if (be.extractTimer >= EXTRACT_INTERVAL) {
            be.extractTimer = 0;

            float extracted = manaData.consumeMana(chunkPos, EXTRACT_AMOUNT);
            if (extracted > 0) {
                be.manaBuffer += extracted;

                // 播放抽取音效
                level.playSound(null, pos, SoundEvents.BEACON_AMBIENT,
                        SoundSource.BLOCKS, 0.5F, 1.2F + level.random.nextFloat() * 0.3F);

                // 结晶：缓冲区满则产出水晶
                while (be.manaBuffer >= CRYSTAL_COST) {
                    be.manaBuffer -= CRYSTAL_COST;
                    if (be.storedCrystals < MAX_STORED_CRYSTALS) {
                        be.storedCrystals++;
                    } else {
                        // 存储已满，弹出掉落
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
        }
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

    // ─── NBT 序列化 ─────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("ManaBuffer", manaBuffer);
        tag.putInt("StoredCrystals", storedCrystals);
        tag.putInt("ExtractTimer", extractTimer);
        tag.putBoolean("Working", working);
        tag.putFloat("ClientChunkMana", clientChunkMana);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        manaBuffer = tag.getFloat("ManaBuffer");
        storedCrystals = tag.getInt("StoredCrystals");
        extractTimer = tag.getInt("ExtractTimer");
        working = tag.getBoolean("Working");
        clientChunkMana = tag.getFloat("ClientChunkMana");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putBoolean("Working", working);
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
