package com.huige233.transcend.items.tech;

import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.tech.shield.CapacitorEnergy;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;


/** 提供可替换的有限容量标准电容，通过 Forge 能量能力充放电并显示剩余电量。 */
public class StandardCapacitorItem extends Item {
    public StandardCapacitorItem() {
        super(new Properties().stacksTo(1));
    }

    public static ItemStack charged() {
        ItemStack stack = new ItemStack(ModItems.standard_capacitor.get());
        CapacitorEnergy.set(stack.getOrCreateTag(), CapacitorEnergy.CAPACITY);
        return stack;
    }

    public static float energy(ItemStack stack) {
        return stack.getTag() == null ? 0.0F : CapacitorEnergy.stored(stack.getTag());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.transcend.phase_shield.capacitor",
                String.format(Locale.ROOT, "%.2f", energy(stack)), (int) CapacitorEnergy.CAPACITY)
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.transcend.standard_capacitor.use").withStyle(ChatFormatting.GRAY));
    }

    @Override public boolean isBarVisible(@NotNull ItemStack stack) { return energy(stack) < CapacitorEnergy.CAPACITY; }
    @Override public int getBarWidth(@NotNull ItemStack stack) { return Math.round(13 * energy(stack) / CapacitorEnergy.CAPACITY); }
    @Override public int getBarColor(@NotNull ItemStack stack) { return 0x00AACC; }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ICapabilityProvider() {
            private final LazyOptional<IEnergyStorage> energy = LazyOptional.of(() -> new IEnergyStorage() {
                @Override public int receiveEnergy(int amount, boolean simulate) {
                    CompoundTag tag = stack.getTag();
                    if (simulate) return CapacitorEnergy.receive(tag == null ? new CompoundTag() : tag, amount, true);
                    if (amount <= 0) return 0;
                    return CapacitorEnergy.receive(stack.getOrCreateTag(), amount, false);
                }
                @Override public int extractEnergy(int amount, boolean simulate) {
                    int extracted = Math.min(Math.max(0, amount), getEnergyStored());
                    if (!simulate && extracted > 0) CapacitorEnergy.drain(stack.getOrCreateTag(), extracted);
                    return extracted;
                }
                @Override public int getEnergyStored() { return (int) StandardCapacitorItem.energy(stack); }
                @Override public int getMaxEnergyStored() { return (int) CapacitorEnergy.CAPACITY; }
                @Override public boolean canExtract() { return true; }
                @Override public boolean canReceive() { return true; }
            });

            @Override
            public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
                return capability == ForgeCapabilities.ENERGY ? energy.cast() : LazyOptional.empty();
            }
        };
    }
}
