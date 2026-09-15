package com.huige233.transcend.block;

import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.menu.AssemblyMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;


/** 保存装配台库存与能量，并在操作者和配方校验通过时推进批量制造任务。 */
public class AssemblyBlockEntity extends BlockEntity implements MenuProvider, com.huige233.transcend.tech.energy.DirectEnergyReceiver {
    public static final int CAPACITY = 50_000_000;
    public static final int SLOT_COUNT = 6;
    public final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot != 4; }
    };
    private final EnergyStorage energy = new EnergyStorage(CAPACITY, 200_000, 0) {
        @Override public int receiveEnergy(int amount, boolean simulate) {
            int received = super.receiveEnergy(amount, simulate);
            if (!simulate && received > 0) setChanged();
            return received;
        }
    };
    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energy);
    private long lastOperationTick = Long.MIN_VALUE;
    private com.huige233.transcend.tech.assembly.AssemblyJob job;
    private int batchSize = 1;
    private int status;

    public boolean hasJob() { return job != null; }
    public int batchSize() { return batchSize; }
    public int remaining() { return job == null ? 0 : job.remaining(); }
    public int progress() { return job == null ? 0 : job.progress(); }
    public int duration() { return job == null ? 0 : job.duration(); }
    public int status() { return status; }

    public boolean changeBatch(int change) {
        if (job != null) return false;
        batchSize = Math.max(1, Math.min(64, batchSize + change));
        setChanged();
        return true;
    }

    public boolean startJob(Player player, com.huige233.transcend.tech.assembly.AssemblyRecipe recipe) {
        if (level == null || level.isClientSide || job != null
                || com.huige233.transcend.tech.assembly.AssemblyOperations.readiness(this, player, recipe) != 1) return false;
        job = new com.huige233.transcend.tech.assembly.AssemblyJob(player.getUUID(), recipe.id(),
                com.huige233.transcend.tech.assembly.AssemblyOperations.signature(recipe), batchSize, recipe.tier() * 40);
        status = 1;
        setChanged();
        return true;
    }

    public boolean cancelJob(Player player) {
        if (job == null || (!job.owner().equals(player.getUUID()) && !player.hasPermissions(2))) return false;
        job = null;
        status = 0;
        setChanged();
        return true;
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, AssemblyBlockEntity bench) {
        if (level.isClientSide || bench.job == null) return;
        Player player = level.getPlayerByUUID(bench.job.owner());
        if (player == null || !player.isAlive() || !(player.containerMenu instanceof AssemblyMenu menu)
                || !menu.isOperating(bench, player)) {
            bench.status = 2;
            return;
        }
        var recipe = level.getRecipeManager().byKey(bench.job.recipe()).orElse(null);
        if (!(recipe instanceof com.huige233.transcend.tech.assembly.AssemblyRecipe assembly)
                || !bench.job.signature().equals(com.huige233.transcend.tech.assembly.AssemblyOperations.signature(assembly))) {
            bench.status = 7;
            return;
        }
        bench.chargeFromItem();
        bench.status = com.huige233.transcend.tech.assembly.AssemblyOperations.readiness(bench, player, assembly);
        if (bench.status != 1) return;
        if (bench.job.advance() && com.huige233.transcend.tech.assembly.AssemblyOperations.complete(bench, player, assembly)) {
            bench.job.completeOne();
            if (bench.job.remaining() == 0) {
                bench.job = null;
                bench.status = 0;
            }
        }
        bench.setChanged();
    }

    public AssemblyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASSEMBLY.get(), pos, state);
    }

    public int energy() { return energy.getEnergyStored(); }

    @Override public int receiveExternalEnergy(int amount) {
        if (level == null || level.isClientSide || isRemoved()) return 0;
        int accepted = Math.min(Math.max(0, amount), CAPACITY - energy());
        if (accepted > 0) {
            energy.deserializeNBT(net.minecraft.nbt.IntTag.valueOf(energy() + accepted));
            setChanged();
        }
        return accepted;
    }

    public void consumeEnergy(int cost) {
        if (cost < 0 || cost > energy()) throw new IllegalArgumentException("Insufficient assembly FE");
        energy.deserializeNBT(net.minecraft.nbt.IntTag.valueOf(energy() - cost));
        setChanged();
    }

    public boolean claimOperation() {
        if (level == null || level.isClientSide || lastOperationTick == level.getGameTime()) return false;
        lastOperationTick = level.getGameTime();
        return true;
    }

    public boolean chargeFromItem() {
        ItemStack stack = items.getStackInSlot(5);
        IEnergyStorage battery = stack.getCapability(ForgeCapabilities.ENERGY).orElse(null);
        if (battery == null || !battery.canExtract()) return false;
        int amount = battery.extractEnergy(Math.min(200_000, CAPACITY - energy()), true);
        if (amount <= 0) return false;
        energy.receiveEnergy(battery.extractEnergy(amount, false), false);
        setChanged();
        return true;
    }

    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyCapability.cast();
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() {
        super.invalidateCaps();
        energyCapability.invalidate();
    }
    @Override public void reviveCaps() {
        super.reviveCaps();
        energyCapability = LazyOptional.of(() -> energy);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", items.serializeNBT());
        tag.putInt("Energy", energy());
        tag.putInt("BatchSize", batchSize);
        if (job != null) tag.put("Job", job.save());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        batchSize = Math.max(1, Math.min(64, tag.getInt("BatchSize")));
        job = com.huige233.transcend.tech.assembly.AssemblyJob.load(tag.getCompound("Job"));
        status = job == null ? 0 : 2;
        items.deserializeNBT(tag.getCompound("Inventory"));
        if (items.getSlots() != SLOT_COUNT) items.setSize(SLOT_COUNT);
        energy.deserializeNBT(net.minecraft.nbt.IntTag.valueOf(Math.max(0, Math.min(CAPACITY, tag.getInt("Energy")))));
    }
    @Override public Component getDisplayName() { return Component.translatable("block.transcend.assembly_table"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AssemblyMenu(id, inventory, this);
    }
}
