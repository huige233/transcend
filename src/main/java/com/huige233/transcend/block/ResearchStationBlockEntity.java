package com.huige233.transcend.block;

import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.menu.ResearchStationMenu;
import com.huige233.transcend.tech.research.ResearchCapabilities;
import com.huige233.transcend.tech.research.ResearchEnergyAccount;
import com.huige233.transcend.tech.research.ResearchConversion;
import com.huige233.transcend.tech.research.ResearchUnlocks;
import com.huige233.transcend.tech.api.ITechEnergy;
import com.huige233.transcend.tech.api.TechCapabilities;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
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
import java.math.BigInteger;
import java.util.UUID;


/** 管理研究站材料、插件、储能和研究点，绑定研究者并驱动其研究进度。 */
public final class ResearchStationBlockEntity extends BlockEntity implements MenuProvider {
    public static final long BASE_INPUT_RATE = 1_000_000L;
    public static final long RF_PER_RESEARCH_POINT = ResearchEnergyAccount.RF_PER_POINT;
    public static final int PLUGIN_SLOTS = 4;
    public static final int INPUT_SLOTS = 2;
    private static final String CLAIM = "TranscendResearchStation";
    private final ResearchEnergyAccount account = new ResearchEnergyAccount();
    private final ItemStackHandler inputs = new ItemStackHandler(INPUT_SLOTS) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public int getSlotLimit(int slot) { return slot == 0 ? 16 : 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 ? stack.is(com.huige233.transcend.init.ModItems.blank_research_component.get())
                    : stack.is(com.huige233.transcend.init.ModItems.research_assist_unit.get());
        }
        @Override public void deserializeNBT(CompoundTag tag) {
            CompoundTag fixed = tag.copy(); fixed.putInt("Size", INPUT_SLOTS); super.deserializeNBT(fixed);
        }
    };
    private final ItemStackHandler plugins = new ItemStackHandler(PLUGIN_SLOTS) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof ResearchPluginBlock;
        }
        @Override public void deserializeNBT(CompoundTag tag) {
            CompoundTag fixed = tag.copy(); fixed.putInt("Size", PLUGIN_SLOTS); super.deserializeNBT(fixed);
        }
    };
    private final IEnergyStorage port = new IEnergyStorage() {
        @Override public int receiveEnergy(int amount, boolean simulate) {
            return (int) ResearchStationBlockEntity.this.receiveEnergy(Math.max(0, amount), simulate);
        }
        @Override public int extractEnergy(int amount, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return (int) Math.min(Integer.MAX_VALUE, account.stored()); }
        @Override public int getMaxEnergyStored() { return Integer.MAX_VALUE; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };
    private final ITechEnergy longPort = new ITechEnergy() {
        public long stored() { return energyStored(); }
        public long capacity() { return Long.MAX_VALUE; }
        public long receive(long amount, boolean simulate) { return receiveEnergy(amount, simulate); }
        public long extract(long amount, boolean simulate) { return 0; }
    };
    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> port);
    private LazyOptional<ITechEnergy> longEnergyCapability = LazyOptional.of(() -> longPort);
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> new CombinedInvWrapper(inputs, plugins));
    private UUID researcher;
    private boolean autoPrepare;
    private long budgetTick = Long.MIN_VALUE, receivedThisTick, workTick = Long.MIN_VALUE;

    public ResearchStationBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.RESEARCH_STATION.get(), pos, state); }
    public ItemStackHandler inputs() { return inputs; }
    public ItemStackHandler plugins() { return plugins; }
    public long energyStored() { return account.stored(); }
    
    public boolean preparePoints() { return preparePoints(false) > 0; }

    public int preparePoints(boolean bulk) {
        if (level == null || level.isClientSide || isRemoved()) return 0;
        if (!hasInputs()) return 0;
        long power = powerMultiplier();
        int batches = ResearchConversion.batches(inputs.getStackInSlot(0).getCount(), inputs.getStackInSlot(1).getCount(),
                account.stored(), power, bulk);
        long unitCost = ResearchConversion.energy(power);
        batches = (int) Math.min(batches, Long.MAX_VALUE / unitCost);
        while (batches > 0 && !account.convertExact(unitCost * batches)) batches--;
        if (batches == 0) return 0;
        inputs.extractItem(0, batches, false);
        inputs.extractItem(1, batches, false);
        setChanged();
        return batches;
    }

    private boolean hasInputs() {
        return !inputs.getStackInSlot(0).isEmpty() && !inputs.getStackInSlot(1).isEmpty()
                && inputs.isItemValid(0, inputs.getStackInSlot(0)) && inputs.isItemValid(1, inputs.getStackInSlot(1));
    }

    private void prepareFor(BigInteger remaining) {
        if (!hasInputs() || remaining.signum() <= 0) return;
        long points = remaining.min(BigInteger.valueOf(pointsPerBatch())).longValue();
        if (!account.convertExact(points * RF_PER_RESEARCH_POINT)) return;
        inputs.extractItem(0, 1, false); inputs.extractItem(1, 1, false); setChanged();
    }

    public boolean autoPrepare() { return autoPrepare; }
    public void setAutoPrepare(boolean enabled) { autoPrepare = enabled; setChanged(); }
    public long pointsPerBatch() { return ResearchConversion.points(powerMultiplier()); }
    public long energyPerBatch() { return ResearchConversion.energy(powerMultiplier()); }

    /** 推送、主动拉取、FE 与长整型能量共用同一 tick 预算，模拟调用不扣预算。 */
    public long receiveEnergy(long amount, boolean simulate) {
        if (level == null || level.isClientSide || isRemoved()) return 0;
        long tick = level.getGameTime();
        if (budgetTick != tick) { budgetTick = tick; receivedThisTick = 0; }
        long request = Math.min(Math.max(0, amount), Math.max(0, inputRate() - receivedThisTick));
        long received = account.receive(request, simulate);
        if (!simulate && received > 0) { receivedThisTick += received; setChanged(); }
        return received;
    }

    public long capacity() { return Long.MAX_VALUE; }
    public long inputRate() {
        long power = powerMultiplier();
        return power > Long.MAX_VALUE / BASE_INPUT_RATE ? Long.MAX_VALUE : BASE_INPUT_RATE * power;
    }

    public long powerMultiplier() {
        long multiplier = 1;
        for (int i = 0; i < plugins.getSlots(); i++) {
            ItemStack stack = plugins.getStackInSlot(i);
            if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof ResearchPluginBlock plugin)
                multiplier += Math.min(Long.MAX_VALUE - multiplier, plugin.powerMultiplier() - 1);
        }
        return multiplier;
    }
    public BigInteger researchPoints() { return account.points(); }
    public long remainder() { return account.remainder(); }
    
    public long conversionRemainder() { return remainder(); }
    public UUID researcher() { return researcher; }

    public static void tick(Level level, ResearchStationBlockEntity station) {
        if (level.isClientSide || level.getServer() == null || station.isRemoved()) return;

        long budget = station.receiveEnergy(Long.MAX_VALUE, true);
        for (Direction side : Direction.values()) {
            if (budget == 0) break;
            BlockPos target = station.worldPosition.relative(side);
            if (!level.hasChunkAt(target)) continue;
            BlockEntity neighbor = level.getBlockEntity(target);
            if (neighbor == null) continue;
            ITechEnergy longSource = neighbor.getCapability(TechCapabilities.TECH_ENERGY, side.getOpposite()).orElse(null);
            if (longSource != null) {
                long available = Math.max(0, Math.min(budget, longSource.extract(budget, true)));
                if (available > 0) {
                    budget -= station.receiveEnergy(Math.max(0, Math.min(available, longSource.extract(available, false))), false);
                    continue;
                }
            }
            IEnergyStorage source = neighbor.getCapability(ForgeCapabilities.ENERGY, side.getOpposite()).orElse(null);
            if (source == null || !source.canExtract()) continue;
            int request = (int) Math.min(Integer.MAX_VALUE, budget);
            if (request == 0) break;
            int received = Math.max(0, Math.min(request, source.extractEnergy(request, false)));
            if (received > 0) budget -= station.receiveEnergy(received, false);
        }
        if (station.researcher == null) return;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(station.researcher);
        if (player == null) return;
        station.tickResearch(player);
    }

    void tickResearch(ServerPlayer player) {
        ResearchStationBlockEntity station = this;
        if (level == null || level.isClientSide || isRemoved() || player.server != level.getServer()
                || !player.getUUID().equals(researcher) || workTick == level.getGameTime()) return;
        workTick = level.getGameTime();
        if (!station.claim(player)) { station.researcher = null; station.setChanged(); return; }
        player.getCapability(ResearchCapabilities.PROGRESS).ifPresent(progress -> {
            if (progress.active() == null) { station.pauseResearch(player); return; }
            int steps = ResearchConversion.processingSteps(station.powerMultiplier());
            for (int i = 0; i < steps && progress.active() != null; i++) {
                if (station.autoPrepare && station.account.points().compareTo(progress.requiredEnergyPerTick()) < 0)
                    station.prepareFor(progress.remainingPoints().subtract(station.account.points()));
                String id = progress.active();
                if (!progress.advance(station.account)) break;
                station.setChanged();
                if (progress.active() == null) {
                    ResearchUnlocks.applyKnowledge(player, progress.completed());
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable("gui.transcend.research.action.completed",
                            net.minecraft.network.chat.Component.translatable("research.transcend." + id)), false);
                }
            }
            if (progress.active() == null) station.pauseResearch(player);
        });
    }

    private boolean matchesClaim(CompoundTag tag) {
        return tag.getLong("Pos") == worldPosition.asLong()
                && tag.getString("Dimension").equals(level.dimension().location().toString());
    }
    private boolean claim(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(CLAIM)) {
            CompoundTag previous = data.getCompound(CLAIM);
            if (!matchesClaim(previous)) {
                for (var otherLevel : player.server.getAllLevels()) {
                    if (!otherLevel.dimension().location().toString().equals(previous.getString("Dimension"))) continue;
                    BlockPos pos = BlockPos.of(previous.getLong("Pos"));
                    if (!otherLevel.hasChunkAt(pos)) return false;
                    if (otherLevel.getBlockEntity(pos) instanceof ResearchStationBlockEntity other
                            && player.getUUID().equals(other.researcher)) return false;
                }
            }
        }
        CompoundTag tag = new CompoundTag();
        tag.putLong("Pos", worldPosition.asLong());
        tag.putString("Dimension", level.dimension().location().toString());
        data.put(CLAIM, tag);
        return true;
    }
    public boolean startResearch(ServerPlayer player, String id) {
        if (level == null || level.isClientSide || isRemoved() || player.level() != level) return false;
        if (researcher != null && !researcher.equals(player.getUUID())) return false;
        return player.getCapability(ResearchCapabilities.PROGRESS).map(progress -> {
            if (!progress.canStartOrResume(id, account.points())) return false;
            if (!claim(player)) return false;
            if (progress.active() == null && !progress.start(id)) return false;
            researcher = player.getUUID(); setChanged(); return true;
        }).orElse(false);
    }
    public void pauseResearch(Player player) {
        if (researcher == null || !researcher.equals(player.getUUID())) return;
        if (level != null && matchesClaim(player.getPersistentData().getCompound(CLAIM))) player.getPersistentData().remove(CLAIM);
        researcher = null; setChanged();
    }

    @Override public net.minecraft.network.chat.Component getDisplayName() { return net.minecraft.network.chat.Component.translatable("block.transcend.research_station"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new ResearchStationMenu(id, inv, this); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); account.save(tag);
        if (researcher != null) tag.putUUID("Researcher", researcher);
        tag.put("Inputs", inputs.serializeNBT());
        tag.put("Plugins", plugins.serializeNBT());
        tag.putBoolean("AutoPrepare", autoPrepare);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); account.load(tag);
        researcher = tag.hasUUID("Researcher") ? tag.getUUID("Researcher") : null;
        inputs.deserializeNBT(tag.getCompound("Inputs"));
        plugins.deserializeNBT(tag.getCompound("Plugins"));
        autoPrepare = tag.getBoolean("AutoPrepare");
        budgetTick = Long.MIN_VALUE; workTick = Long.MIN_VALUE; receivedThisTick = 0;
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyCapability.cast();
        if (cap == TechCapabilities.TECH_ENERGY) return longEnergyCapability.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCapability.cast();
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); energyCapability.invalidate(); longEnergyCapability.invalidate(); itemCapability.invalidate(); }
    @Override public void reviveCaps() {
        super.reviveCaps(); energyCapability = LazyOptional.of(() -> port);
        longEnergyCapability = LazyOptional.of(() -> longPort);
        itemCapability = LazyOptional.of(() -> new CombinedInvWrapper(inputs, plugins));
    }
}
