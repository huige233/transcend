package com.huige233.transcend.items.tech;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import com.huige233.transcend.tech.api.ITechEnergy;
import com.huige233.transcend.tech.api.TechCapabilities;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;
import java.util.List;


/** 以长整数持久化便携电容能量，同时提供完整科技能量接口与限幅 Forge 能量接口。 */
public class LongCapacitorItem extends Item {
    public static final long RF_PER_TECH = 100_000L;
    private final long capacity;
    public LongCapacitorItem(long capacity) { super(new Properties().stacksTo(1)); this.capacity = capacity; }
    public long capacity() { return capacity; }
    private long stored(ItemStack stack) {
        return Math.min(capacity, Math.max(0L, stack.getTag() == null ? 0L : stack.getTag().getLong("Energy")));
    }
    private long receive(ItemStack stack, long amount, boolean simulate) {
        long current = stored(stack), accepted = Math.min(Math.max(0L, amount), Math.max(0L, capacity - current));
        if (!simulate && accepted > 0) stack.getOrCreateTag().putLong("Energy", current + accepted);
        return accepted;
    }
    private long extract(ItemStack stack, long amount, boolean simulate) {
        long extracted = Math.min(Math.max(0L, amount), stored(stack));
        if (!simulate && extracted > 0) stack.getOrCreateTag().putLong("Energy", stored(stack) - extracted);
        return extracted;
    }
    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.transcend.capacitor.energy", stored(stack), capacity));
        tooltip.add(Component.translatable("tooltip.transcend.capacitor.conversion", RF_PER_TECH));
    }
    @Override public boolean isBarVisible(ItemStack stack) { return stored(stack) < capacity; }
    @Override public int getBarWidth(ItemStack stack) {
        return capacity == 0 ? 0 : (int)Math.min(13L, (double)13L * stored(stack) / capacity);
    }
    @Override public int getBarColor(ItemStack stack) { return 0x25D9FF; }
    @Override public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ICapabilityProvider() {
            private final LazyOptional<IEnergyStorage> cap = LazyOptional.of(() -> new IEnergyStorage() {
                public int receiveEnergy(int amount, boolean simulate) { return (int)Math.min(Integer.MAX_VALUE, receive(stack, amount, simulate)); }
                public int extractEnergy(int amount, boolean simulate) { return (int)Math.min(Integer.MAX_VALUE, extract(stack, amount, simulate)); }
                public int getEnergyStored() { return (int)Math.min(Integer.MAX_VALUE, stored(stack)); }
                public int getMaxEnergyStored() { return (int)Math.min(Integer.MAX_VALUE, capacity); }
                public boolean canExtract() { return true; }
                public boolean canReceive() { return true; }
            });
            private final LazyOptional<ITechEnergy> techCap = LazyOptional.of(() -> new ITechEnergy() {
                public long stored() { return LongCapacitorItem.this.stored(stack); }
                public long capacity() { return LongCapacitorItem.this.capacity; }
                public long receive(long amount, boolean simulate) { return LongCapacitorItem.this.receive(stack, amount, simulate); }
                public long extract(long amount, boolean simulate) { return LongCapacitorItem.this.extract(stack, amount, simulate); }
            });
            @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable net.minecraft.core.Direction side) {
                if (capability == ForgeCapabilities.ENERGY) return cap.cast();
                if (capability == TechCapabilities.TECH_ENERGY) return techCap.cast();
                return LazyOptional.empty();
            }
        };
    }
}
