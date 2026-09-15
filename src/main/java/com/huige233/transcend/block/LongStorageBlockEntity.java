package com.huige233.transcend.block;

import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.tech.api.ITechEnergy;
import com.huige233.transcend.tech.api.TechCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;


/** 持久化长整型储能，通过科技能量与 Forge 能量接口提供充放电并向邻接设备供能。 */
public class LongStorageBlockEntity extends BlockEntity implements com.huige233.transcend.tech.energy.DirectEnergyReceiver, net.minecraft.world.MenuProvider {
    private final long capacity;
    private final com.huige233.transcend.tech.energy.LongEnergyStorage tech;
    private final EnergyStorage fe = new EnergyStorage(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE) {
        @Override public int receiveEnergy(int amount, boolean simulate) {
            long value = tech.receive(amount, simulate);
            if (!simulate && value > 0) setChanged();
            return (int)Math.min(Integer.MAX_VALUE, value);
        }
        @Override public int extractEnergy(int amount, boolean simulate) {
            long value = tech.extract(amount, simulate);
            if (!simulate && value > 0) setChanged();
            return (int)Math.min(Integer.MAX_VALUE, value);
        }
        @Override public int getEnergyStored() { return (int)Math.min(Integer.MAX_VALUE, tech.stored()); }
        @Override public int getMaxEnergyStored() { return Integer.MAX_VALUE; }
    };
    private LazyOptional<IEnergyStorage> feCap = LazyOptional.of(() -> fe);
    private LazyOptional<ITechEnergy> techCap = LazyOptional.of(() -> new ITechEnergy() {
        public long stored() { return tech.stored(); }
        public long capacity() { return capacity; }
        public long receive(long amount, boolean simulate) { long n = tech.receive(amount, simulate); if (!simulate && n > 0) setChanged(); return n; }
        public long extract(long amount, boolean simulate) { long n = tech.extract(amount, simulate); if (!simulate && n > 0) setChanged(); return n; }
    });
    public LongStorageBlockEntity(BlockPos pos, BlockState state, long capacity) {
        super(ModBlockEntities.LONG_STORAGE.get(), pos, state);
        this.capacity = Math.max(0L, capacity);
        this.tech = new com.huige233.transcend.tech.energy.LongEnergyStorage(this.capacity);
    }
    @Override public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("block.transcend.long_storage");
    }
    @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.entity.player.Player player) {
        return new com.huige233.transcend.menu.LongStorageMenu(id, inventory, this);
    }
    public long stored() { return tech.stored(); }
    public long capacity() { return capacity; }
    @Override public int receiveExternalEnergy(int amount) {
        if (level == null || level.isClientSide || isRemoved()) return 0;
        int accepted = (int) tech.receive(Math.max(0, amount), false);
        if (accepted > 0) setChanged();
        return accepted;
    }
    public static void tick(net.minecraft.world.level.Level level, LongStorageBlockEntity be) {
        if (level.isClientSide) return;
        for (Direction direction : Direction.values()) {
            var target = level.getBlockEntity(be.worldPosition.relative(direction));
            if (target == null) continue;
            IEnergyStorage output = target.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).orElse(null);
            if (output == null || !output.canReceive()) continue;
            int amount = be.fe.extractEnergy(Integer.MAX_VALUE, true);
            if (amount <= 0) break;
            int accepted = output.receiveEnergy(amount, false);
            if (accepted > 0) be.fe.extractEnergy(accepted, false);
        }
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putLong("Energy", tech.stored()); }
    @Override public void load(CompoundTag tag) { super.load(tag); tech.set(tag.getLong("Energy")); }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return feCap.cast();
        if (cap == TechCapabilities.TECH_ENERGY) return techCap.cast();
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); feCap.invalidate(); techCap.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); feCap = LazyOptional.of(() -> fe); techCap = LazyOptional.of(() -> new ITechEnergy() {
        public long stored() { return tech.stored(); } public long capacity() { return capacity; }
        public long receive(long amount, boolean simulate) { return tech.receive(amount, simulate); }
        public long extract(long amount, boolean simulate) { return tech.extract(amount, simulate); }
    }); }
}
