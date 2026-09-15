package com.huige233.transcend.block;

import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.items.tech.MagneticConfinementContainerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;


/** 绑定装载中的磁约束容器，按周期消耗能量与奇点物质并产出微型奇点和空容器。 */
public class BlackHoleSeedBreederBlockEntity extends BlockEntity implements com.huige233.transcend.tech.energy.DirectEnergyReceiver {
    public static final int CYCLE_TICKS = 200;
    public static final int MATTER_INTERVAL = 10;
    public static final int MATTER_PER_CYCLE = CYCLE_TICKS / MATTER_INTERVAL;
    public static final int FE_PER_TICK = 10_000;
    public static final int ENERGY_CAPACITY = CYCLE_TICKS * FE_PER_TICK;
    public static final TagKey<Item> SINGULARITY_MATTER = TagKey.create(Registries.ITEM,
             new ResourceLocation("transcend", "singularity_matter"));
    public static final int STATUS_NO_CONTAINER = 0;
    public static final int STATUS_RUNNING = 1;
    public static final int STATUS_OUTPUT_FULL = 2;
    public static final int STATUS_NO_ENERGY = 3;
    public static final int STATUS_NO_MATTER = 4;

    private int progress;
    private int storedEnergy;
    private int matterConsumed;
    private ItemStack boundContainer = ItemStack.EMPTY;
    private CompoundTag legacyBreeding = new CompoundTag();
    private boolean internalChange;
    private final ItemStackHandler items = new ItemStackHandler(4) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case 0 -> !isProcessing() && isLoadedContainer(stack);
                case 1 -> stack.is(SINGULARITY_MATTER);
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) { return slot == 0 ? 1 : 64; }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot == 0 && isProcessing()) return stack;
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == 0 && isProcessing() && !internalChange) return ItemStack.EMPTY;
            return super.extractItem(slot, amount, simulate);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            
            if (slot == 0 && isProcessing() && !internalChange) return;
            super.setStackInSlot(slot, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot == 0 && !internalChange) resetCycle();
            setChanged();
        }
    };
    private final IEnergyStorage energyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int accepted = Math.min(Math.max(0, amount), ENERGY_CAPACITY - storedEnergy);
            if (!simulate && accepted > 0) { storedEnergy += accepted; setChanged(); }
            return accepted;
        }
        @Override public int extractEnergy(int amount, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return storedEnergy; }
        @Override public int getMaxEnergyStored() { return ENERGY_CAPACITY; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> items);
    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyStorage);

    public BlackHoleSeedBreederBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLACK_HOLE_SEED_BREEDER.get(), pos, state);
    }

    public ItemStackHandler items() { return items; }
    public IEnergyStorage energy() { return energyStorage; }
    public int progress() { return progress; }
    public int energyStored() { return storedEnergy; }
    @Override public int receiveExternalEnergy(int amount) {
        if (level == null || level.isClientSide || isRemoved()) return 0;
        return energyStorage.receiveEnergy(Math.max(0, amount), false);
    }
    public int matterConsumed() { return matterConsumed; }
    public boolean isProcessing() { return progress > 0; }

    public static boolean isLoadedContainer(ItemStack stack) {
        return stack.is(ModItems.magnetic_confinement_container.get())
                && MagneticConfinementContainerItem.isLoaded(stack);
    }

    private boolean validInput() {
        ItemStack input = items.getStackInSlot(0);
        return input.getCount() == 1 && isLoadedContainer(input);
    }

    private ItemStack emptyContainer() {
        return new ItemStack(ModItems.magnetic_confinement_container.get());
    }

    private boolean canOutput(int slot, ItemStack result) {
        ItemStack current = items.getStackInSlot(slot);
        return current.isEmpty() || (ItemStack.isSameItemSameTags(current, result)
                && current.getCount() < Math.min(current.getMaxStackSize(), items.getSlotLimit(slot)));
    }

    private boolean outputBlocked() {
        
        return !canOutput(2, new ItemStack(ModItems.mini_singularity.get())) || !canOutput(3, emptyContainer());
    }

    public int status() {
        if (!validInput()) return STATUS_NO_CONTAINER;
        if (outputBlocked()) return STATUS_OUTPUT_FULL;
        if (storedEnergy < FE_PER_TICK) return STATUS_NO_ENERGY;
        if (progress % MATTER_INTERVAL == 0 && !items.getStackInSlot(1).is(SINGULARITY_MATTER)) {
            return STATUS_NO_MATTER;
        }
        return STATUS_RUNNING;
    }

    private void resetCycle() {
        progress = 0;
        matterConsumed = 0;
        boundContainer = ItemStack.EMPTY;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlackHoleSeedBreederBlockEntity be) {
        if (level.isClientSide || be.isRemoved()) return;
        
        if (be.isProcessing() && (!be.validInput()
                || !ItemStack.isSameItemSameTags(be.boundContainer, be.items.getStackInSlot(0)))) {
            be.resetCycle();
            be.setChanged();
        }
        if (be.status() != STATUS_RUNNING) return;
        if (be.progress == 0) be.boundContainer = be.items.getStackInSlot(0).copy();
        
        if (be.progress % MATTER_INTERVAL == 0) {
            be.items.extractItem(1, 1, false);
            be.matterConsumed++;
        }
        be.storedEnergy -= FE_PER_TICK;
        be.progress++;
        if (be.progress == CYCLE_TICKS) {
            be.internalChange = true;
            try {
                be.items.setStackInSlot(0, ItemStack.EMPTY);
                be.appendOutput(2, new ItemStack(ModItems.mini_singularity.get()));
                be.appendOutput(3, be.emptyContainer());
                be.resetCycle();
            } finally {
                be.internalChange = false;
            }
        }
        be.setChanged();
    }

    private void appendOutput(int slot, ItemStack result) {
        ItemStack current = items.getStackInSlot(slot);
        if (!current.isEmpty()) result.setCount(current.getCount() + 1);
        items.setStackInSlot(slot, result);
    }

    
    public void dropContents() {
        if (level == null || level.isClientSide) return;
        internalChange = true;
        try {
            resetCycle();
            for (int slot = 0; slot < items.getSlots(); slot++) {
                ItemStack stack = items.getStackInSlot(slot);
                items.setStackInSlot(slot, ItemStack.EMPTY);
                if (!stack.isEmpty()) Containers.dropItemStack(level,
                        worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
            }
        } finally {
            internalChange = false;
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        tag.putInt("Energy", storedEnergy);
        CompoundTag breeding = new CompoundTag();
        breeding.putInt("Version", 1);
        breeding.putInt("Progress", progress);
        breeding.putInt("MatterConsumed", matterConsumed);
        breeding.put("Container", boundContainer.save(new CompoundTag()));
        tag.put("Breeding", breeding);
        if (!legacyBreeding.isEmpty()) tag.put("LegacyBreeding", legacyBreeding.copy());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        resetCycle();
        internalChange = true;
        try {
            
            CompoundTag inventory = tag.getCompound("Items").copy();
            inventory.putInt("Size", 4);
            items.deserializeNBT(inventory);
        } finally {
            internalChange = false;
        }
        storedEnergy = Math.max(0, Math.min(ENERGY_CAPACITY, tag.getInt("Energy")));
        legacyBreeding = tag.getCompound("LegacyBreeding").copy();
        for (String key : new String[] {"Seeds", "Output", "Progress", "seeds", "output", "progress"}) {
            if (tag.contains(key)) legacyBreeding.put(key, tag.get(key).copy());
        }
        if (tag.contains("Breeding", Tag.TAG_COMPOUND)) {
            CompoundTag breeding = tag.getCompound("Breeding");
            int savedProgress = breeding.getInt("Progress");
            int savedMatter = breeding.getInt("MatterConsumed");
            ItemStack savedContainer = ItemStack.of(breeding.getCompound("Container"));
            if (breeding.getInt("Version") == 1 && savedProgress > 0 && savedProgress < CYCLE_TICKS
                    && savedMatter == (savedProgress + MATTER_INTERVAL - 1) / MATTER_INTERVAL
                    && validInput() && savedContainer.getCount() == 1
                    && ItemStack.isSameItemSameTags(savedContainer, items.getStackInSlot(0))) {
                progress = savedProgress;
                matterConsumed = savedMatter;
                boundContainer = savedContainer;
            }
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (!isRemoved()) {
            if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCapability.cast();
            if (cap == ForgeCapabilities.ENERGY) return energyCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
        energyCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemCapability = LazyOptional.of(() -> items);
        energyCapability = LazyOptional.of(() -> energyStorage);
    }
}
