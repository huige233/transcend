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

public class ManaCondenserBlockEntity extends BlockEntity {

    public static final String BE_ID = "mana_condenser_be";

    public static final int CAPACITY = 256;

    public static final int PER_PUSH = 8;

    private static final int PUSH_INTERVAL = 5;

    private final SimpleManaStorage buffer;
    private final LazyOptional<IManaHandler> manaCap;
    private int tickCounter = 0;

    public ManaCondenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_CONDENSER_BE.get(), pos, state);
        this.buffer = new SimpleManaStorage(CAPACITY, PER_PUSH, PER_PUSH);
        this.manaCap = LazyOptional.of(() -> this.buffer);
    }

    public int getStoredMana() { return buffer.getManaStored(); }

    public int getCapacity() { return buffer.getMaxManaStored(); }

    public int receiveFromWell(int amount) {
        int accepted = buffer.receiveMana(amount, false);
        if (accepted > 0) setChanged();
        return accepted;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaCondenserBlockEntity be) {
        be.tickCounter++;
        if (be.tickCounter < PUSH_INTERVAL) return;
        be.tickCounter = 0;
        if (be.buffer.getManaStored() <= 0) return;

        boolean changed = false;
        int budget = PER_PUSH;
        for (Direction dir : Direction.values()) {
            if (budget <= 0 || be.buffer.getManaStored() <= 0) break;
            BlockEntity nb = level.getBlockEntity(pos.relative(dir));
            if (nb == null || nb == be) continue;

            if (nb instanceof com.huige233.transcend.block.ManaWellBlockEntity) continue;
            IManaHandler h = nb.getCapability(
                    ManaHandlerCapability.MANA_HANDLER, dir.getOpposite()).resolve().orElse(null);
            if (h == null || !h.canReceive()) continue;
            int avail = be.buffer.extractMana(budget, true);
            if (avail <= 0) continue;
            int accepted = h.receiveMana(avail, false);
            if (accepted > 0) {
                be.buffer.extractMana(accepted, false);
                budget -= accepted;
                changed = true;
            }
        }
        if (changed) be.setChanged();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Buffer", buffer.serializeNBT());
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Buffer")) buffer.deserializeNBT(tag.getCompound("Buffer"));
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
