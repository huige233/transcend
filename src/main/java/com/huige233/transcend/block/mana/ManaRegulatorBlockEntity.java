package com.huige233.transcend.block.mana;

import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.mana.IManaHandler;
import com.huige233.transcend.mana.ManaHandlerCapability;
import com.huige233.transcend.mana.SimpleManaStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** 魔力调节器方块实体。 */
public class ManaRegulatorBlockEntity extends BlockEntity {

    public static final String BE_ID = "mana_regulator_be";

    public static final int MIN_PRIORITY = 1;

    public static final int MAX_PRIORITY = 4;

    private static final int TICK_INTERVAL = 5;

    private static final int[] RATE_TABLE = { 0, 4, 8, 16, 32 };

    private final SimpleManaStorage buffer;
    private final LazyOptional<IManaHandler> manaCap;

    private int priority = 2;
    private int tickCounter = 0;

    public ManaRegulatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_REGULATOR_BE.get(), pos, state);
        int rate = rateForPriority(priority);
        this.buffer = new SimpleManaStorage(Math.max(16, rate * 4), rate, rate);
        this.manaCap = LazyOptional.of(() -> this.buffer);
    }

    public static int rateForPriority(int p) {
        if (p < MIN_PRIORITY) p = MIN_PRIORITY;
        if (p > MAX_PRIORITY) p = MAX_PRIORITY;
        return RATE_TABLE[p];
    }

    public int getPriority() { return priority; }

    public int cyclePriority() {
        priority = priority >= MAX_PRIORITY ? MIN_PRIORITY : priority + 1;
        setChanged();
        return priority;
    }

    public int getBuffered() { return buffer.getManaStored(); }

    public int getCapacity() { return buffer.getMaxManaStored(); }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaRegulatorBlockEntity be) {
        be.tickCounter++;
        if (be.tickCounter < TICK_INTERVAL) return;
        be.tickCounter = 0;

        if (level.hasNeighborSignal(pos)) return;

        Direction outDir = state.getValue(ManaRegulatorBlock.FACING);
        int budget = rateForPriority(be.priority);
        boolean changed = false;

        int free = be.buffer.getMaxManaStored() - be.buffer.getManaStored();
        if (free > 0) {
            int leftToPull = Math.min(free, budget);
            for (Direction dir : Direction.values()) {
                if (leftToPull <= 0) break;
                if (dir == outDir) continue;
                BlockEntity nb = level.getBlockEntity(pos.relative(dir));
                if (nb == null || nb == be) continue;
                IManaHandler h = nb.getCapability(
                        ManaHandlerCapability.MANA_HANDLER, dir.getOpposite()).resolve().orElse(null);
                if (h == null || !h.canExtract()) continue;
                int sim = h.extractMana(leftToPull, true);
                if (sim <= 0) continue;
                int accepted = be.buffer.receiveMana(sim, false);
                if (accepted > 0) {
                    h.extractMana(accepted, false);
                    leftToPull -= accepted;
                    changed = true;
                }
            }
        }

        if (be.buffer.getManaStored() > 0) {
            BlockEntity outBe = level.getBlockEntity(pos.relative(outDir));
            if (outBe != null && outBe != be) {
                IManaHandler outH = outBe.getCapability(
                        ManaHandlerCapability.MANA_HANDLER, outDir.getOpposite()).resolve().orElse(null);
                if (outH != null && outH.canReceive()) {
                    int avail = be.buffer.extractMana(budget, true);
                    if (avail > 0) {
                        int accepted = outH.receiveMana(avail, false);
                        if (accepted > 0) {
                            be.buffer.extractMana(accepted, false);
                            changed = true;
                        }
                    }
                }
            }
        }

        if (changed) be.setChanged();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Buffer", buffer.serializeNBT());
        tag.putByte("Priority", (byte) priority);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Buffer")) buffer.deserializeNBT(tag.getCompound("Buffer"));
        if (tag.contains("Priority")) {
            int p = tag.getByte("Priority") & 0xFF;
            priority = Math.max(MIN_PRIORITY, Math.min(MAX_PRIORITY, p));
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ManaHandlerCapability.MANA_HANDLER) {
            return manaCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        manaCap.invalidate();
    }
}
