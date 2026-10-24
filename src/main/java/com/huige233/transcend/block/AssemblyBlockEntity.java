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
    public static final int INPUT_RATE = 200_000;
    public static final int SLOT_COUNT = 6;
    public final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public int getSlotLimit(int slot) { return slot == 0 || slot == 5 ? 1 : super.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot != 4; }
        @Override public void deserializeNBT(CompoundTag tag) {
            CompoundTag fixed = tag.copy();
            fixed.putInt("Size", SLOT_COUNT);
            super.deserializeNBT(fixed);
        }
    };
    private final EnergyStorage energy = new EnergyStorage(CAPACITY, INPUT_RATE, 0) {
        @Override public int receiveEnergy(int amount, boolean simulate) {
            if (level == null || level.isClientSide || isRemoved()) return 0;
            int received = super.receiveEnergy(Math.max(0, amount), simulate);
            if (!simulate && received > 0) setChanged();
            return received;
        }
    };
    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energy);
    private LazyOptional<net.minecraftforge.items.IItemHandler> automationCapability = LazyOptional.of(this::automationHandler);
    private long lastOperationTick = Long.MIN_VALUE;
    private long chargeTick = Long.MIN_VALUE;
    private int chargedThisTick;
    private com.huige233.transcend.tech.assembly.AssemblyJob job;
    private int batchSize = 1;
    private int status;
    private final int[] recipeSelections = new int[5];
    public int recipeSelection(int process) { return recipeSelections[process]; }
    public boolean cycleRecipe(int process, int matches) {
        if (job != null || process < 0 || process >= recipeSelections.length || matches < 1) return false;
        recipeSelections[process] = (Math.floorMod(recipeSelections[process], matches) + 1) % matches;
        setChanged(); return true;
    }

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
                com.huige233.transcend.tech.assembly.AssemblyOperations.signature(recipe), batchSize, recipe.durationTicks());
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
        if (level.isClientSide || bench.isRemoved()) return;
        bench.chargeFromItem();
        if (bench.job == null) return;
        Player player = level.getPlayerByUUID(bench.job.owner());
        boolean supervised = player != null && player.containerMenu instanceof AssemblyMenu menu && menu.isOperating(bench, player);
        boolean automated = player != null && player.getCapability(com.huige233.transcend.tech.research.ResearchCapabilities.PROGRESS)
                .map(p -> p.completed().contains("automation")).orElse(false);
        if (player == null || !player.isAlive() || (!supervised && !automated)) {
            bench.status = 2;
            return;
        }
        var recipe = level.getRecipeManager().byKey(bench.job.recipe()).orElse(null);
        if (!(recipe instanceof com.huige233.transcend.tech.assembly.AssemblyRecipe assembly)
                || !bench.job.signature().equals(com.huige233.transcend.tech.assembly.AssemblyOperations.signature(assembly))) {
            bench.status = 7;
            return;
        }
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
        if (level == null || level.isClientSide || isRemoved()) return false;
        if (chargeTick != level.getGameTime()) {
            chargeTick = level.getGameTime();
            chargedThisTick = 0;
        }
        int request = Math.min(INPUT_RATE - chargedThisTick, CAPACITY - energy());
        if (request <= 0) return false;
        ItemStack stack = items.getStackInSlot(5);
        IEnergyStorage battery = stack.getCapability(ForgeCapabilities.ENERGY).orElse(null);
        if (battery == null || !battery.canExtract()) return false;
        int amount = Math.max(0, Math.min(request, battery.extractEnergy(request, true)));
        if (amount <= 0) return false;
        int extracted = Math.max(0, Math.min(amount, battery.extractEnergy(amount, false)));
        if (extracted == 0) return false;
        chargedThisTick += energy.receiveEnergy(extracted, false);
        setChanged();
        return true;
    }

    /** 只暴露材料、成品和供电槽，自动化不能触碰枪盾及安装模块。 */
    private net.minecraftforge.items.IItemHandler automationHandler() {
        return new net.minecraftforge.items.IItemHandler() {
            private final int[] slots = {2, 3, 4, 5};
            private int physical(int slot) { if (slot < 0 || slot >= slots.length) throw new IndexOutOfBoundsException(slot); return slots[slot]; }
            public int getSlots() { return slots.length; }
            public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(physical(slot)).copy(); }
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (!isItemValid(slot, stack)) return stack;
                return items.insertItem(physical(slot), stack, simulate);
            }
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                int target = physical(slot);
                return hasJob() && (target == 2 || target == 3) ? ItemStack.EMPTY : items.extractItem(target, amount, simulate);
            }
            public int getSlotLimit(int slot) { return physical(slot) == 5 ? 1 : 64; }
            public boolean isItemValid(int slot, ItemStack stack) {
                int target = physical(slot);
                if (hasJob() && (target == 2 || target == 3) && level != null) {
                    var current = level.getRecipeManager().byKey(job.recipe()).orElse(null);
                    if (!(current instanceof com.huige233.transcend.tech.assembly.AssemblyRecipe recipe)) return false;
                    return target == 2 ? recipe.first().test(stack) : recipe.secondCount() > 0 && recipe.second().test(stack);
                }
                return target != 4 && (target != 5 || stack.getCapability(ForgeCapabilities.ENERGY).isPresent());
            }
        };
    }

    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyCapability.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return automationCapability.cast();
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() {
        super.invalidateCaps();
        energyCapability.invalidate();
        automationCapability.invalidate();
    }
    @Override public void reviveCaps() {
        super.reviveCaps();
        energyCapability = LazyOptional.of(() -> energy);
        automationCapability = LazyOptional.of(this::automationHandler);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", items.serializeNBT());
        tag.putInt("Energy", energy());
        tag.putInt("BatchSize", batchSize);
        tag.putIntArray("RecipeSelections", recipeSelections);
        if (job != null) tag.put("Job", job.save());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        batchSize = Math.max(1, Math.min(64, tag.getInt("BatchSize")));
        java.util.Arrays.fill(recipeSelections, 0);
        int[] selections = tag.getIntArray("RecipeSelections");
        for (int i = 0; i < Math.min(selections.length, recipeSelections.length); i++) recipeSelections[i] = Math.max(0, selections[i]);
        job = com.huige233.transcend.tech.assembly.AssemblyJob.load(tag.getCompound("Job"));
        status = job == null ? 0 : 2;
        items.deserializeNBT(tag.getCompound("Inventory"));
        energy.deserializeNBT(net.minecraft.nbt.IntTag.valueOf(Math.max(0, Math.min(CAPACITY, tag.getInt("Energy")))));
        lastOperationTick = Long.MIN_VALUE;
        chargeTick = Long.MIN_VALUE;
        chargedThisTick = 0;
    }
    @Override public Component getDisplayName() { return Component.translatable("block.transcend.assembly_table"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AssemblyMenu(id, inventory, this);
    }
}
