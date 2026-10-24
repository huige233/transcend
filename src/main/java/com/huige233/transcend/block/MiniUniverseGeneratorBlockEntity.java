package com.huige233.transcend.block;

import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.util.MachineDataValues;
import com.huige233.transcend.util.UniverseOutputCycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import java.math.BigInteger;


/** 消耗微型奇点与启动能量，通过独立输入输出缓冲和超频周期释放大整数总发电量。 */
public class MiniUniverseGeneratorBlockEntity extends BlockEntity implements com.huige233.transcend.tech.energy.DirectEnergyReceiver {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    
    public static final int BUFFER = Integer.MAX_VALUE;
    
    public static final int RATE = Integer.MAX_VALUE;
    public static final long LONG_OUTPUT_RATE = 1_000_000_000_000L;
    
    public static final BigInteger STARTUP = BigInteger.valueOf(Integer.MAX_VALUE);
    
    public static final BigInteger T5_STANDARD_OUTPUT = BigInteger.TEN.pow(26);
    public static final BigInteger OUTPUT = T5_STANDARD_OUTPUT.divide(BigInteger.valueOf(3));
    private static final int MAX_DIGITS = 64;
    /** 区分微型宇宙发电机的空闲、启动充能和发电阶段。 */
    private enum Phase { IDLE, CHARGING, GENERATING }
    /** 细分微型宇宙发电机缺少奇点、等待供能、充能、发电及输出受阻等显示状态。 */
    public enum Status { NO_SINGULARITY, WAITING_INPUT_FE, CHARGING, GENERATING, OUTPUT_FULL, OUTPUT_BLOCKED, IDLE }
    private Phase phase = Phase.IDLE;
    private Status status = Status.NO_SINGULARITY;
    private final EnergyStorage input = storage();
    private final EnergyStorage output = storage();
    private final ItemStackHandler items = new ItemStackHandler(2) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && stack.is(ModItems.mini_singularity.get());
        }
    };
    private BigInteger charged = BigInteger.ZERO, remaining = BigInteger.ZERO, generatedPool = BigInteger.ZERO, legacySeed = BigInteger.ZERO;
    private boolean overclock;
    private final UniverseOutputCycle outputCycle = new UniverseOutputCycle();
    private LazyOptional<IEnergyStorage> inputCap = LazyOptional.empty(), outputCap = LazyOptional.empty();
    private LazyOptional<IItemHandler> itemCap = LazyOptional.empty();
    private LazyOptional<com.huige233.transcend.tech.api.ITechEnergy> longOutputCap = LazyOptional.empty();
    private long exportTick = Long.MIN_VALUE, exportedThisTick;
    private int visualUsage;
    private int lastSyncedVisualUsage = -1;

    public MiniUniverseGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINI_UNIVERSE_GENERATOR.get(), pos, state);
        createCaps();
    }

    private EnergyStorage storage() {
        return new EnergyStorage(BUFFER, BUFFER, BUFFER) {
            @Override public int receiveEnergy(int amount, boolean simulate) {
                int accepted = super.receiveEnergy(Math.max(0, amount), simulate);
                if (!simulate && accepted > 0) setChanged();
                return accepted;
            }
            @Override public int extractEnergy(int amount, boolean simulate) {
                int extracted = super.extractEnergy(Math.max(0, amount), simulate);
                if (!simulate && extracted > 0) setChanged();
                return extracted;
            }
        };
    }

    private IEnergyStorage port(EnergyStorage buffer, boolean receiving) {
        return new IEnergyStorage() {
            @Override public int receiveEnergy(int amount, boolean simulate) { return receiving ? buffer.receiveEnergy(amount, simulate) : 0; }
            @Override public int extractEnergy(int amount, boolean simulate) { return receiving ? 0 : (int) extractOutput(Math.max(0, amount), simulate, false); }
            @Override public int getEnergyStored() { return buffer.getEnergyStored(); }
            @Override public int getMaxEnergyStored() { return buffer.getMaxEnergyStored(); }
            @Override public boolean canExtract() { return !receiving; }
            @Override public boolean canReceive() { return receiving; }
        };
    }

    private void createCaps() {
        inputCap = LazyOptional.of(() -> port(input, true));
        outputCap = LazyOptional.of(() -> port(output, false));
        itemCap = LazyOptional.of(() -> items);
        longOutputCap = LazyOptional.of(() -> new com.huige233.transcend.tech.api.ITechEnergy() {
            public long stored() { return availableOutput().min(BigInteger.valueOf(Long.MAX_VALUE)).longValue(); }
            public long capacity() { return Long.MAX_VALUE; }
            public long receive(long amount, boolean simulate) { return 0; }
            public long extract(long amount, boolean simulate) { return extractOutput(amount, simulate, false); }
        });
    }

    private BigInteger availableOutput() {
        return generatedPool.add(BigInteger.valueOf(output.getEnergyStored()));
    }
    public long outputRate() { return overclock ? LONG_OUTPUT_RATE * 32 / 5 : LONG_OUTPUT_RATE; }
    private long outputBudget() {
        if (level == null || level.isClientSide || isRemoved()) return 0;
        if (exportTick != level.getGameTime()) { exportTick = level.getGameTime(); exportedThisTick = 0; }
        return Math.max(0, outputRate() - exportedThisTick);
    }
    /** 全部方向、FE 与长整型接口共用预算；循环填缓冲期间只允许取缓冲，防止覆盖剩余总量。 */
    private long extractOutput(long amount, boolean simulate, boolean bufferedOnly) {
        long requested = Math.min(Math.max(0, amount), outputBudget());
        BigInteger available = bufferedOnly ? BigInteger.valueOf(output.getEnergyStored()) : availableOutput();
        long accepted = available.min(BigInteger.valueOf(requested)).longValue();
        if (!simulate && accepted > 0) {
            int buffered = output.extractEnergy((int) Math.min(Integer.MAX_VALUE, accepted), false);
            long fromPool = accepted - buffered;
            if (fromPool > 0) generatedPool = generatedPool.subtract(BigInteger.valueOf(fromPool));
            exportedThisTick += accepted;
            setChanged();
        }
        return accepted;
    }

    public ItemStackHandler items() { return items; }
    public IItemHandler itemHandler() { return items; }
    public long energy() { return output.getEnergyStored(); }
    public long capacity() { return BUFFER; }
    public int inputEnergy() { return input.getEnergyStored(); }
    public int outputEnergy() { return output.getEnergyStored(); }
    @Override public int receiveExternalEnergy(int amount) {
        if (level == null || level.isClientSide || isRemoved()) return 0;
        int accepted = input.receiveEnergy(Math.max(0, amount), false);
        if (accepted > 0) setChanged();
        return accepted;
    }
    public BigInteger chargedAmount() { return charged; }
    /** 尚未进入统一输出池的总量；生成池通过输出缓冲和 availableOutput 另行结算。 */
    public BigInteger remainingAmount() { return remaining; }
    public BigInteger generatedAmount() { return generatedPool; }
    public String phaseName() { return phase.name(); }

    public float generationUsage() {
        return visualUsage / 1000f;
    }

    private int calculateVisualUsage() {
        if (phase != Phase.GENERATING) return 0;
        BigInteger used = OUTPUT.subtract(remaining).max(BigInteger.ZERO).min(OUTPUT);
        return used.multiply(BigInteger.valueOf(1000)).divide(OUTPUT).intValue();
    }

    private void syncVisualUsage(boolean phaseChanged) {
        if (!phaseChanged && lastSyncedVisualUsage >= 0 && level.getGameTime() % 20 != 0) return;
        visualUsage = calculateVisualUsage();
        if (visualUsage != lastSyncedVisualUsage || phaseChanged) {
            lastSyncedVisualUsage = visualUsage;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public int phaseId() { return phase.ordinal(); }
    public boolean active() { return phase == Phase.GENERATING; }
    public int statusId() { return status.ordinal(); }
    public Status status() { return status; }
    private void updateStatus(Level level) {
        Status next;
        if (phase == Phase.CHARGING) {
            next = input.getEnergyStored() > 0 ? Status.CHARGING : Status.WAITING_INPUT_FE;
        } else if (phase == Phase.GENERATING) {
            next = output.getEnergyStored() >= BUFFER ? Status.OUTPUT_FULL
                    : (remaining.signum() == 0 && generatedPool.signum() == 0 ? Status.IDLE : Status.GENERATING);
        } else if (output.getEnergyStored() > 0) {
            next = hasOutputReceiver(level) ? Status.IDLE : Status.OUTPUT_BLOCKED;
        } else {
            next = Status.NO_SINGULARITY;
        }
        if (next != status) { status = next; setChanged(); }
    }

    private boolean hasOutputReceiver(Level level) {
        for (Direction side : Direction.values()) {
            if (side == Direction.DOWN || side == Direction.UP) continue;
            BlockPos targetPos = worldPosition.relative(side);
            if (!level.hasChunkAt(targetPos)) continue;
            BlockEntity target = level.getBlockEntity(targetPos);
            if (target == null) continue;
            var longStorage = target.getCapability(com.huige233.transcend.tech.api.TechCapabilities.TECH_ENERGY, side.getOpposite()).orElse(null);
            if (longStorage != null && longStorage.receive(1, true) > 0) return true;
            IEnergyStorage storage = target.getCapability(ForgeCapabilities.ENERGY, side.getOpposite()).orElse(null);
            if (storage != null && storage.canReceive()) return true;
        }
        return false;
    }
    public boolean overclocked() { return overclock; }
    public void setOverclocked(boolean value) {
        if (overclock != value) {
            overclock = value;
            outputCycle.reset();
            setChanged();
        }
    }


    public static void serverTick(Level level, BlockPos pos, BlockState state, MiniUniverseGeneratorBlockEntity be) {
        if (level.isClientSide) return;
        Phase previousPhase = be.phase;
        if (be.phase == Phase.IDLE && be.input.getEnergyStored() >= STARTUP.intValueExact()
                && be.items.isItemValid(0, be.items.getStackInSlot(0)) && !be.items.getStackInSlot(0).isEmpty()) {
            be.items.extractItem(0, 1, false);
            be.charged = BigInteger.ZERO;
            be.remaining = BigInteger.ZERO;
            be.phase = Phase.CHARGING;
            be.setChanged();
        }
        if (be.phase == Phase.CHARGING) {
            int n = be.input.extractEnergy(MachineDataValues.production(STARTUP.subtract(be.charged), RATE, be.input.getEnergyStored()), false);
            be.charged = be.charged.add(BigInteger.valueOf(n));
            if (be.charged.compareTo(STARTUP) >= 0) {
                be.charged = STARTUP;
                be.remaining = OUTPUT;
                be.phase = Phase.GENERATING;
                be.setChanged();
            }
            if (n > 0) be.setChanged();
        }
        
        BigInteger before = be.remaining;
        BigInteger perTick = BigInteger.valueOf(be.outputRate());
        BigInteger generated = be.remaining.min(perTick);
        if (generated.signum() > 0) {
            be.remaining = be.remaining.subtract(generated);
            be.generatedPool = be.generatedPool.add(generated);
        }
        be.pushEnergy(level);
        be.pushLongEnergy(level);
        if (!be.remaining.equals(before)) be.setChanged();
        if (be.phase == Phase.GENERATING) {
            if (be.remaining.signum() == 0 && be.generatedPool.signum() == 0 && be.output.getEnergyStored() == 0) {
                be.phase = Phase.IDLE;
                be.charged = BigInteger.ZERO;
                be.setChanged();
            }
        }
        be.setActiveState(be.active());
        be.updateStatus(level);
        be.syncVisualUsage(previousPhase != be.phase);
    }

    private void pushEnergy(Level level) {
        int budget = (int) extractOutput(Integer.MAX_VALUE, true, false);
        for (Direction side : Direction.values()) {
            if (budget <= 0) break;
            if (side == Direction.DOWN || side == Direction.UP) continue;
            BlockPos targetPos = worldPosition.relative(side);
            if (!level.hasChunkAt(targetPos)) continue;
            BlockEntity target = level.getBlockEntity(targetPos);
            if (target == null) continue;
            IEnergyStorage storage = target.getCapability(ForgeCapabilities.ENERGY, side.getOpposite()).orElse(null);
            if (storage == null || !storage.canReceive()) continue;
            int offered = (int) extractOutput(budget, true, false);
            int sent = Math.max(0, Math.min(offered, storage.receiveEnergy(offered, false)));
            extractOutput(sent, false, false);
            budget -= sent;
        }
    }

    private void pushLongEnergy(Level level) {
        for (Direction side : Direction.values()) {
            if (side == Direction.DOWN || side == Direction.UP) continue;
            long offered = extractOutput(Long.MAX_VALUE, true, false);
            if (offered == 0) break;
            BlockPos targetPos = worldPosition.relative(side);
            if (!level.hasChunkAt(targetPos)) continue;
            BlockEntity target = level.getBlockEntity(targetPos);
            if (target == null) continue;
            var receiver = target.getCapability(com.huige233.transcend.tech.api.TechCapabilities.TECH_ENERGY, side.getOpposite()).orElse(null);
            if (receiver == null) continue;
            long accepted = Math.max(0, Math.min(offered, receiver.receive(offered, true)));
            if (accepted > 0) {
                long sent = Math.max(0, Math.min(accepted, receiver.receive(accepted, false)));
                extractOutput(sent, false, false);
            }
        }
    }

    private void setActiveState(boolean value) {
        if (level != null && getBlockState().getValue(ACTIVE) != value) {
            level.setBlock(worldPosition, getBlockState().setValue(ACTIVE, value), 3);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, MiniUniverseGeneratorBlockEntity be) {
        
        
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        
        return new net.minecraft.world.phys.AABB(worldPosition).inflate(.3, 0, .3).expandTowards(0, 1.3, 0);
    }

    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == com.huige233.transcend.tech.api.TechCapabilities.TECH_ENERGY)
            return side != null && side != Direction.DOWN && side != Direction.UP ? longOutputCap.cast() : LazyOptional.empty();
        if (cap == ForgeCapabilities.ENERGY) {
            if (side == null) return LazyOptional.empty();
            if (side == Direction.UP) return LazyOptional.empty();
            return side == Direction.DOWN ? inputCap.cast() : outputCap.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() {
        super.invalidateCaps();
        inputCap.invalidate(); outputCap.invalidate(); itemCap.invalidate(); longOutputCap.invalidate();
    }
    @Override public void reviveCaps() { super.reviveCaps(); createCaps(); }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        tag.putString("Phase", phase.name());
        tag.putString("Charged", charged.toString());
        tag.putString("Remaining", remaining.toString());
        tag.putString("GeneratedPool", generatedPool.toString());
        tag.putString("LegacySeed", legacySeed.toString());
        tag.putInt("InputEnergy", input.getEnergyStored());
        tag.putInt("OutputEnergy", output.getEnergyStored());
        tag.putBoolean("Overclock", overclock);
    }

    private static BigInteger readBig(CompoundTag tag, String key) {
        String value = tag.getString(key);
        return value.length() <= MAX_DIGITS && value.matches("[0-9]+") ? new BigInteger(value) : BigInteger.ZERO;
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        
        input.extractEnergy(input.getEnergyStored(), false);
        output.extractEnergy(output.getEnergyStored(), false);
        items.deserializeNBT(tag.getCompound("Items"));
        legacySeed = readBig(tag, "LegacySeed");
        try { phase = Phase.valueOf(tag.getString("Phase")); }
        catch (IllegalArgumentException e) { phase = Phase.IDLE; }
        charged = readBig(tag, "Charged").min(STARTUP);
        remaining = readBig(tag, "Remaining").min(OUTPUT);
        generatedPool = readBig(tag, "GeneratedPool").min(OUTPUT);
        int inputAmount = Math.max(0, Math.min(BUFFER, tag.getInt("InputEnergy")));
        int outputAmount = Math.max(0, Math.min(BUFFER, tag.getInt("OutputEnergy")));
        if (!tag.contains("InputEnergy")) {
            for (int i = 0; i < 16; i++) {
                inputAmount = (int) Math.min(BUFFER, (long) inputAmount + Math.max(0, tag.getInt("Energy" + i)));
            }
        }
        input.receiveEnergy(inputAmount, false);
        output.receiveEnergy(outputAmount, false);
        overclock = tag.getBoolean("Overclock");
        outputCycle.reset();
        if (phase == Phase.GENERATING && remaining.signum() == 0 && generatedPool.signum() == 0 && output.getEnergyStored() == 0) phase = Phase.IDLE;
        if (phase == Phase.IDLE) { charged = BigInteger.ZERO; remaining = BigInteger.ZERO; generatedPool = BigInteger.ZERO; }
        visualUsage = calculateVisualUsage();
        lastSyncedVisualUsage = -1;
    }
    @Override public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("VisualUsage", calculateVisualUsage());
        return tag;
    }
    @Override public void handleUpdateTag(CompoundTag tag) {
        visualUsage = Math.max(0, Math.min(1000, tag.getInt("VisualUsage")));
    }
    @Override public void onDataPacket(net.minecraft.network.Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) handleUpdateTag(packet.getTag());
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
