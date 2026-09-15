package com.huige233.transcend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.Arrays;
import java.util.EnumMap;


/** 执行燃料、天气条件或创造模式发电，并按面配置在总输出预算内传输能量。 */
public class FEGeneratorBlockEntity extends BlockEntity implements com.huige233.transcend.tech.energy.DirectEnergyReceiver {
    public static final int CAPACITY = 100000000;
    public static final int CREATIVE_MIN_OUTPUT = 0;
    public static final int CREATIVE_MAX_OUTPUT = 100000000;
    public static final int CREATIVE_OUTPUT_STEP = 100;
    /** 区分发电机各面的禁用、能量输入和能量输出模式。 */
    public enum FaceMode { NONE, INPUT, OUTPUT }
    /** 区分燃料燃烧、风力和创造模式三种发电方式。 */
    public enum Mode { FIRE, WIND, CREATIVE }
    /** 表示发电机空闲、发电、储能已满或发电条件受阻的运行状态。 */
    public enum Status { IDLE, GENERATING, FULL, BLOCKED }

    private final EnergyStorage energy = new EnergyStorage(CAPACITY, CAPACITY, CAPACITY) {
        @Override public int receiveEnergy(int amount, boolean simulate) {
            int received = super.receiveEnergy(Math.max(0, amount), simulate);
            if (!simulate && received > 0) setChanged();
            return received;
        }
        @Override public int extractEnergy(int amount, boolean simulate) {
            int extracted = super.extractEnergy(Math.max(0, amount), simulate);
            if (!simulate && extracted > 0) setChanged();
            return extracted;
        }
    };
    private final FaceMode[] faces = new FaceMode[Direction.values().length];
    private final EnumMap<Direction, LazyOptional<IEnergyStorage>> energyCaps = new EnumMap<>(Direction.class);
    private LazyOptional<IItemHandler> fuelCap = LazyOptional.empty();
    private boolean capsValid;
    private int capGeneration;
    private final int production;
    private final Mode mode;
    private int configuredOutput;
    private final ItemStackHandler fuelHandler = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return mode == Mode.FIRE && isFuel(stack);
        }
    };
    private int burnTime;
    private int burnDuration;
    private Status status = Status.IDLE;

    public FEGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int production, Mode mode) {
        super(type, pos, state);
        this.production = Math.max(0, production);
        this.mode = mode;
        this.configuredOutput = this.production;
        Arrays.fill(faces, FaceMode.OUTPUT);
        createCaps();
    }

    public int getEnergy() { return energy.getEnergyStored(); }
    @Override public int receiveExternalEnergy(int amount) {
        if (level == null || level.isClientSide || isRemoved()) return 0;
        int accepted = energy.receiveEnergy(Math.max(0, amount), false);
        if (accepted > 0) setChanged();
        return accepted;
    }
    public int getCapacity() { return energy.getMaxEnergyStored(); }
    public int getProduction() { return production; }
    public int getConfiguredOutput() { return configuredOutput; }
    public void setConfiguredOutput(int value) {
        if (mode != Mode.CREATIVE) return;
        int next = Math.max(CREATIVE_MIN_OUTPUT, Math.min(CREATIVE_MAX_OUTPUT, value));
        if (configuredOutput != next) { configuredOutput = next; setChanged(); }
    }
    public void adjustConfiguredOutput(int delta) { setConfiguredOutput(configuredOutput + delta); }
    public Mode getMode() { return mode; }
    public int getBurnTime() { return burnTime; }
    public int getBurnDuration() { return burnDuration; }
    public Status getStatus() { return status; }
    public IItemHandler fuelHandler() { return fuelHandler; }
    public static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty() && ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0;
    }
    public FaceMode faceMode(Direction side) { return side == null ? FaceMode.NONE : faces[side.ordinal()]; }
    public void cycleFace(Direction side) {
        if (side == null) return;
        int index = side.ordinal();
        faces[index] = FaceMode.values()[(faces[index].ordinal() + 1) % FaceMode.values().length];
        setChanged();
    }
    public void setFuel(ItemStack stack) {
        if (stack.isEmpty() || mode == Mode.FIRE && isFuel(stack)) {
            ItemStack copy = stack.copy();
            if (!copy.isEmpty()) copy.setCount(Math.min(copy.getCount(), copy.getMaxStackSize()));
            fuelHandler.setStackInSlot(0, copy);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FEGeneratorBlockEntity entity) {
        if (level.isClientSide) return;
        
        entity.pushEnergy(level, pos);
        Status next;
        if (entity.getEnergy() >= entity.getCapacity()) {
            next = Status.FULL;
        } else {
            boolean active = switch (entity.mode) {
                case CREATIVE -> true;
                case WIND -> level.canSeeSky(pos.above()) && !level.isRaining() && !level.isThundering();
                case FIRE -> entity.tickFire();
            };
            if (active) {
                entity.energy.receiveEnergy(entity.mode == Mode.CREATIVE ? entity.configuredOutput : entity.production, false);
                next = Status.GENERATING;
            } else {
                next = entity.mode == Mode.WIND ? Status.BLOCKED : Status.IDLE;
            }
        }
        if (entity.status != next) {
            entity.status = next;
            entity.setChanged();
        }
    }

    private void pushEnergy(Level level, BlockPos pos) {
        
        
        int outputBudget = mode == Mode.CREATIVE ? configuredOutput : production;
        int remaining = Math.min(outputBudget, getEnergy());
        for (Direction side : Direction.values()) {
            if (remaining <= 0) break;
            if (faceMode(side) != FaceMode.OUTPUT) continue;
            BlockPos neighborPos = pos.relative(side);
            
            if (!level.hasChunkAt(neighborPos)) continue;
            BlockEntity target = level.getBlockEntity(neighborPos);
            if (target == null) continue;
            IEnergyStorage storage = target.getCapability(ForgeCapabilities.ENERGY, side.getOpposite()).orElse(null);
            if (storage == null || !storage.canReceive()) continue;
            int offered = energy.extractEnergy(remaining, true);
            int sent = Math.max(0, Math.min(offered, storage.receiveEnergy(offered, false)));
            energy.extractEnergy(sent, false);
            remaining -= sent;
        }
    }

    private boolean tickFire() {
        if (burnTime <= 0) {
            ItemStack stack = fuelHandler.getStackInSlot(0);
            int burn = ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
            if (stack.isEmpty() || burn <= 0) return false;
            ItemStack consumed = fuelHandler.extractItem(0, 1, false);
            ItemStack remainder = consumed.getCraftingRemainingItem();
            if (!remainder.isEmpty()) {
                if (fuelHandler.getStackInSlot(0).isEmpty()) {
                    fuelHandler.setStackInSlot(0, remainder);
                } else if (level != null) {
                    
                    Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1,
                            worldPosition.getZ() + 0.5, remainder);
                }
            }
            burnTime = burn;
            burnDuration = burn;
        }
        
        burnTime--;
        setChanged();
        return true;
    }

    public void dropContents() {
        if (level == null || level.isClientSide) return;
        ItemStack stack = fuelHandler.extractItem(0, Integer.MAX_VALUE, false);
        if (!stack.isEmpty()) Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, stack);
    }

    private void createCaps() {
        capsValid = true;
        int generation = ++capGeneration;
        for (Direction side : Direction.values()) {
            energyCaps.put(side, LazyOptional.of(() -> new FaceEnergy(side, generation)));
        }
        fuelCap = LazyOptional.of(() -> fuelHandler);
    }

    /** 按端口方向和能力实例有效期校验发电机的能量接收与提取权限。 */
    private final class FaceEnergy implements IEnergyStorage {
        private final Direction side;
        private final int generation;
        private FaceEnergy(Direction side, int generation) { this.side = side; this.generation = generation; }
        private boolean allows(FaceMode expected) {
            return capsValid && !isRemoved() && generation == capGeneration && faceMode(side) == expected;
        }
        @Override public int receiveEnergy(int amount, boolean simulate) {
            return canReceive() ? energy.receiveEnergy(amount, simulate) : 0;
        }
        @Override public int extractEnergy(int amount, boolean simulate) {
            return canExtract() ? energy.extractEnergy(amount, simulate) : 0;
        }
        @Override public int getEnergyStored() { return energy.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return energy.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return allows(FaceMode.OUTPUT); }
        @Override public boolean canReceive() { return allows(FaceMode.INPUT); }
    }

    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        if (capability == ForgeCapabilities.ENERGY) {
            
            return capsValid && !isRemoved() && side != null ? energyCaps.get(side).cast() : LazyOptional.empty();
        }
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return capsValid && !isRemoved() && mode == Mode.FIRE ? fuelCap.cast() : LazyOptional.empty();
        }
        return super.getCapability(capability, side);
    }
    @Override public void invalidateCaps() {
        super.invalidateCaps();
        capsValid = false;
        energyCaps.values().forEach(LazyOptional::invalidate);
        energyCaps.clear();
        fuelCap.invalidate();
    }
    @Override public void reviveCaps() {
        super.reviveCaps();
        createCaps();
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", getEnergy());
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnDuration", burnDuration);
        tag.put("FuelInventory", fuelHandler.serializeNBT());
        tag.putInt("ConfiguredOutput", configuredOutput);
        tag.putIntArray("EnergyFaces", Arrays.stream(faces).mapToInt(Enum::ordinal).toArray());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        energy.extractEnergy(energy.getEnergyStored(), false);
        energy.receiveEnergy(Math.max(0, Math.min(getCapacity(), tag.getInt("Energy"))), false);
        burnTime = Math.max(0, tag.getInt("BurnTime"));
        burnDuration = Math.max(burnTime, tag.getInt("BurnDuration"));
        fuelHandler.setStackInSlot(0, ItemStack.EMPTY);
        if (tag.contains("FuelInventory")) fuelHandler.deserializeNBT(tag.getCompound("FuelInventory"));
        else if (tag.contains("Fuel")) fuelHandler.setStackInSlot(0, ItemStack.of(tag.getCompound("Fuel")));
        configuredOutput = Math.max(0, Math.min(production, tag.contains("ConfiguredOutput") ? tag.getInt("ConfiguredOutput") : production));
        Arrays.fill(faces, FaceMode.OUTPUT);
        int[] savedFaces = tag.getIntArray("EnergyFaces");
        for (int i = 0; i < Math.min(faces.length, savedFaces.length); i++) {
            if (savedFaces[i] >= 0 && savedFaces[i] < FaceMode.values().length) faces[i] = FaceMode.values()[savedFaces[i]];
        }
    }

    /** 承接发电机方块传入的实体类型、产能和模式以创建具体发电机实例。 */
    public static class Generator extends FEGeneratorBlockEntity {
        public Generator(BlockEntityType<?> type, BlockPos pos, BlockState state, int production, Mode mode) {
            super(type, pos, state, production, mode);
        }
    }
}
