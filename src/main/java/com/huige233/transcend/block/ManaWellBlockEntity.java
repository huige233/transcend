package com.huige233.transcend.block;

import com.huige233.transcend.block.mana.ManaCondenserBlockEntity;
import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.init.ModBlocks;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.world.mana.ChunkManaSavedData;
import com.huige233.transcend.world.mana.ChunkManaObservation;
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

import java.util.Optional;

/** 魔力井方块实体：存储并向外输出魔力。 */
public class ManaWellBlockEntity extends BlockEntity {

    private static final int EXTRACT_INTERVAL = 200;

    private static final int RESONANCE_EXTRACT_INTERVAL = 150;

    private static final float EXTRACT_AMOUNT = 5.0F;

    private static final float CRYSTAL_COST = 10.0F;

    private static final int MAX_STORED_CRYSTALS = 16;

    private static final float MAX_PRODUCTION_MULTIPLIER = 2.67F;

    private static final float RESONANCE_MAX_PRODUCTION_MULTIPLIER = 2.40F;

    private static final float RESONANCE_BONUS = 1.0F;

    private static final int MAX_HORIZONTAL_BOOSTERS = 4;

    private float manaBuffer = 0.0F;
    private int storedCrystals = 0;
    private int extractTimer = 0;
    private boolean working = false;

    private boolean resonance = false;

    private float clientChunkMana = ChunkManaSavedData.DEFAULT_MANA;

    public ManaWellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_WELL_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaWellBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        be.extractTimer++;
        ChunkPos chunkPos = new ChunkPos(pos);
        ChunkManaSavedData manaData = ChunkManaSavedData.get(serverLevel);
        float currentMana = manaData.getMana(chunkPos);

        ChunkManaSavedData.Tier tier = manaData.getTier(chunkPos);
        float floor = manaData.getExtractFloor(chunkPos);
        be.working = tier != ChunkManaSavedData.Tier.EXHAUSTED && currentMana > floor;

        boolean prevResonance = be.resonance;
        be.resonance = isResonanceActive(level, pos);
        int interval = be.resonance ? RESONANCE_EXTRACT_INTERVAL : EXTRACT_INTERVAL;

        if (!be.working) {

            if (be.extractTimer % 80 == 0 || prevResonance != be.resonance) {
                be.clientChunkMana = currentMana;
                be.syncToClient();
            }
            return;
        }

        if (be.extractTimer >= interval) {
            be.extractTimer = 0;

            float upgradeMult = computeProductionMultiplier(level, pos);
            float requested = EXTRACT_AMOUNT * upgradeMult;

            float extracted = manaData.consumeManaSafe(chunkPos, requested);
            if (extracted > 0) {
                be.manaBuffer += extracted;

                level.playSound(null, pos, SoundEvents.BEACON_AMBIENT,
                        SoundSource.BLOCKS, 0.5F, 1.2F + level.random.nextFloat() * 0.3F);

                ManaCondenserBlockEntity condenser = findAdjacentCondenser(level, pos);

                while (be.manaBuffer >= CRYSTAL_COST) {
                    be.manaBuffer -= CRYSTAL_COST;
                    if (condenser != null) {

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

            be.clientChunkMana = manaData.getMana(chunkPos);
            be.syncToClient();
        } else if (prevResonance != be.resonance) {

            be.syncToClient();
        }
    }

    private static float computeProductionMultiplier(Level level, BlockPos pos) {
        float mult = 1.0F;

        var belowBlock = level.getBlockState(pos.below()).getBlock();
        if (belowBlock == ModBlocks.ANCIENT_CRYSTAL.get())                mult += 1.0F;
        else if (belowBlock == ModBlocks.CONCENTRATED_CRYSTAL_BLOCK.get()) mult += 0.6F;
        else if (belowBlock == ModBlocks.MAGIC_CRYSTAL_BLOCK.get())        mult += 0.25F;

        if (isResonanceActive(level, pos)) {

            mult += 4 * 0.25F;

            mult += RESONANCE_BONUS;

            var magic = ModBlocks.MAGIC_CRYSTAL_BLOCK.get();
            int diagMagic = 0;
            if (level.getBlockState(pos.north().east()).getBlock() == magic) diagMagic++;
            if (level.getBlockState(pos.north().west()).getBlock() == magic) diagMagic++;
            if (level.getBlockState(pos.south().east()).getBlock() == magic) diagMagic++;
            if (level.getBlockState(pos.south().west()).getBlock() == magic) diagMagic++;
            mult += 0.25F * diagMagic;
            return Math.min(mult, RESONANCE_MAX_PRODUCTION_MULTIPLIER);
        }

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

    private static boolean isResonanceActive(Level level, BlockPos pos) {
        var ancient = ModBlocks.ANCIENT_CRYSTAL.get();
        return level.getBlockState(pos.north()).getBlock() == ancient
            && level.getBlockState(pos.south()).getBlock() == ancient
            && level.getBlockState(pos.east()).getBlock() == ancient
            && level.getBlockState(pos.west()).getBlock() == ancient;
    }

    @Nullable
    private static ManaCondenserBlockEntity findAdjacentCondenser(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockEntity nb = level.getBlockEntity(pos.relative(dir));
            if (nb instanceof ManaCondenserBlockEntity cond) return cond;
        }
        return null;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, ManaWellBlockEntity be) {
        if (!be.working) return;

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

            double offX = (level.random.nextDouble() - 0.5) * 4.0;
            double offZ = (level.random.nextDouble() - 0.5) * 4.0;
            level.addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT,
                    cx + offX, cy + 1.0 + level.random.nextDouble(), cz + offZ,
                    -offX * 0.05, -0.02, -offZ * 0.05);
        }

        if (tick % 12 == 0 && be.clientChunkMana > 50) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    cx, cy + 1.2, cz,
                    (level.random.nextDouble() - 0.5) * 0.02, 0.03, (level.random.nextDouble() - 0.5) * 0.02);
        }
    }

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

    public Optional<Float> getChunkManaDisplay() {
        if (level instanceof ServerLevel sl) {
            return ChunkManaObservation.observe(sl, new ChunkPos(worldPosition)).mana();
        }
        return Optional.of(clientChunkMana);
    }

    public float getManaBuffer() { return manaBuffer; }
    public int getStoredCrystals() { return storedCrystals; }
    public boolean isWorking() { return working; }

    public boolean isResonance() { return resonance; }

    public int getCurrentInterval() { return resonance ? RESONANCE_EXTRACT_INTERVAL : EXTRACT_INTERVAL; }

    public float getCurrentMultiplierCap() { return resonance ? RESONANCE_MAX_PRODUCTION_MULTIPLIER : MAX_PRODUCTION_MULTIPLIER; }

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
