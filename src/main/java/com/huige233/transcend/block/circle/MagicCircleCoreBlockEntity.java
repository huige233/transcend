package com.huige233.transcend.block.circle;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleFunctionExecutorRegistry;
import com.huige233.transcend.circle.CircleFunctionSettings;
import com.huige233.transcend.circle.CircleFunctionType;
import com.huige233.transcend.circle.CircleManaMath;
import com.huige233.transcend.circle.CircleStructureCache;
import com.huige233.transcend.circle.CircleStructureValidator;
import com.huige233.transcend.circle.CircleTier;
import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.mana.IManaHandler;
import com.huige233.transcend.mana.ManaHandlerCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.huige233.transcend.items.MagicCrystalItem;
import com.huige233.transcend.items.ManaStorageItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MagicCircleCoreBlockEntity extends BlockEntity implements MenuProvider {

    public static final String BE_ID = "circle_core_be";

    private CircleTier detectedTier = null;
    private boolean structureValid = false;
    private long lastValidationTime = 0;
    private boolean structureDirty = true;

    private int lastMissingBlockCount = 0;

    private CircleFunctionType activeFunction = null;
    private ItemStack functionSigil = ItemStack.EMPTY;
    private CircleFunctionExecutor executor = null;
    private boolean active = false;
    private boolean enabled = true;

    private int storedMana = 0;
    private float manaDebt = 0f;
    private int stallTicks = 0;
    private int tickCounter = 0;

    private UUID owner = null;

    private int powerLevel = 0;
    private int durationLevel = 0;
    private int efficiencyLevel = 0;
    private int specialLevel = 0;

    private final List<ItemStack> cachedCatalysts = new ArrayList<>();

    private Map<String, Integer> functionSettings = new HashMap<>();

    private boolean sigilLocked = false;

    private final SimpleContainer guiContainer = new SimpleContainer(2);

    private final IManaHandler manaCapability = new IManaHandler() {
        @Override
        public int getManaStored() {
            return storedMana;
        }

        @Override
        public int getMaxManaStored() {
            return getMaxMana();
        }

        @Override
        public int receiveMana(int amount, boolean simulate) {
            if (amount <= 0) return 0;
            int max = getMaxMana();
            int space = Math.max(0, max - storedMana);
            int accepted = Math.min(space, amount);
            if (!simulate && accepted > 0) {
                storedMana += accepted;
                setChanged();
            }
            return accepted;
        }

        @Override
        public int extractMana(int amount, boolean simulate) {
            if (amount <= 0) return 0;
            int extracted = Math.min(storedMana, amount);
            if (!simulate && extracted > 0) {
                storedMana -= extracted;
                setChanged();
            }
            return extracted;
        }

        @Override
        public boolean canReceive() {
            return getMaxMana() > 0;
        }

        @Override
        public boolean canExtract() {
            return true;
        }
    };

    private final LazyOptional<IManaHandler> manaCapHolder = LazyOptional.of(() -> manaCapability);

    public MagicCircleCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CIRCLE_CORE_BE.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        if (detectedTier != null) {
            tag.putString("Tier", detectedTier.name());
        }
        tag.putBoolean("StructureValid", structureValid);

        if (activeFunction != null) {
            tag.putString("Function", activeFunction.name());
        }

        if (functionSigil != null && !functionSigil.isEmpty()) {
            CompoundTag sigilTag = new CompoundTag();
            functionSigil.save(sigilTag);
            tag.put("Sigil", sigilTag);
        }

        tag.putBoolean("Active", active);
        tag.putBoolean("Enabled", enabled);

        tag.putInt("StoredMana", storedMana);
        tag.putFloat("ManaDebt", manaDebt);
        tag.putInt("StallTicks", stallTicks);

        saveOwnerToNbt(tag, owner);

        tag.putInt("PowerLevel", powerLevel);
        tag.putInt("DurationLevel", durationLevel);
        tag.putInt("EfficiencyLevel", efficiencyLevel);
        tag.putInt("SpecialLevel", specialLevel);

        CircleFunctionSettings.saveSettings(tag, functionSettings);

        tag.putBoolean("SigilLocked", sigilLocked);

        net.minecraft.nbt.ListTag guiItems = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < guiContainer.getContainerSize(); i++) {
            ItemStack stack = guiContainer.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                stack.save(itemTag);
                guiItems.add(itemTag);
            }
        }
        tag.put("GuiItems", guiItems);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.contains("Tier")) {
            try {
                this.detectedTier = CircleTier.valueOf(tag.getString("Tier"));
            } catch (IllegalArgumentException ignored) {
                this.detectedTier = null;
            }
        } else {
            this.detectedTier = null;
        }
        this.structureValid = tag.getBoolean("StructureValid");

        if (tag.contains("Function")) {
            try {
                this.activeFunction = CircleFunctionType.valueOf(tag.getString("Function"));
            } catch (IllegalArgumentException ignored) {
                this.activeFunction = null;
            }
        } else {
            this.activeFunction = null;
        }

        if (tag.contains("Sigil")) {
            this.functionSigil = ItemStack.of(tag.getCompound("Sigil"));
        } else {
            this.functionSigil = ItemStack.EMPTY;
        }

        this.active = tag.getBoolean("Active");
        this.enabled = tag.contains("Enabled") ? tag.getBoolean("Enabled") : true;

        this.storedMana = Math.max(0, tag.getInt("StoredMana"));
        float loadedDebt = tag.getFloat("ManaDebt");
        this.manaDebt = Float.isFinite(loadedDebt) ? Math.max(0.0F, Math.min(1_000_000.0F, loadedDebt)) : 0.0F;
        this.stallTicks = Math.max(0, Math.min(1_000_000, tag.getInt("StallTicks")));

        this.owner = loadOwnerFromNbt(tag);

        this.powerLevel = tag.getInt("PowerLevel");
        this.durationLevel = tag.getInt("DurationLevel");
        this.efficiencyLevel = tag.getInt("EfficiencyLevel");
        this.specialLevel = tag.getInt("SpecialLevel");

        this.functionSettings = CircleFunctionSettings.loadSettings(tag);

        this.sigilLocked = tag.getBoolean("SigilLocked");

        for (int i = 0; i < guiContainer.getContainerSize(); i++) {
            guiContainer.setItem(i, ItemStack.EMPTY);
        }
        if (tag.contains("GuiItems", net.minecraft.nbt.Tag.TAG_LIST)) {
            net.minecraft.nbt.ListTag guiItems = tag.getList("GuiItems", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < guiItems.size(); i++) {
                CompoundTag itemTag = guiItems.getCompound(i);
                int slot = itemTag.getByte("Slot") & 0xFF;
                if (slot < guiContainer.getContainerSize()) {
                    guiContainer.setItem(slot, ItemStack.of(itemTag));
                }
            }
        }

        this.structureDirty = true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MagicCircleCoreBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        be.tickCounter++;
        if (be.tickCounter < 20) return;
        be.tickCounter = 0;

        be.refreshAugmentLevels(serverLevel);

        boolean slotChanged = false;
        slotChanged |= be.pullManaFromInputSlot();
        slotChanged |= be.pushManaToOutputSlot();
        if (slotChanged) {
            be.setChanged();
            be.syncToClient();
        }

        if (be.structureDirty) {
            be.validateStructure(serverLevel);
        }

        if (!be.active || !be.enabled || be.activeFunction == null || be.executor == null) {
            return;
        }

        float chunkMana = com.huige233.transcend.world.mana.ChunkManaSavedData
                .get(serverLevel).getMana(new net.minecraft.world.level.ChunkPos(pos));
        float upkeep = CircleManaMath.computeFinalUpkeep(
                be.activeFunction.getBaseUpkeepPerMinute(),
                be.powerLevel, be.durationLevel, be.efficiencyLevel, be.specialLevel,
                1,
                chunkMana
        );
        if (be.owner != null) {
            net.minecraft.server.level.ServerPlayer ownerPlayer = serverLevel.getServer()
                    .getPlayerList().getPlayer(be.owner);
            if (ownerPlayer != null) {
                upkeep *= com.huige233.transcend.ascension.AscensionCapability.get(ownerPlayer)
                        .getVowCircleUpkeepMult();
            }
        }
        upkeep = Float.isFinite(upkeep) ? Math.max(0.0F, Math.min(1_000_000.0F, upkeep)) : 0.0F;

        be.manaDebt = Math.min(1_000_000.0F, be.manaDebt + upkeep / 60.0f);

        if (be.manaDebt >= 1.0f) {
            int requested = (int) be.manaDebt;
            int paid = Math.min(requested, be.storedMana);
            be.storedMana -= paid;
            be.manaDebt -= paid;

            if (paid < requested) {
                be.stallTicks = Math.min(1_000_000, be.stallTicks + 20);
            } else {
                be.stallTicks = 0;
            }
        }

        int graceTicks = CircleManaMath.computeGraceTicks(be.durationLevel);
        if (be.stallTicks > graceTicks) {

            return;
        }

        CircleFunctionContext ctx = be.buildContext(serverLevel);
        be.executor.tick(ctx);

        be.storedMana = ctx.getStoredMana();

        be.setChanged();
    }

    private static final int TRANSFER_PER_TICK = 8;

    private boolean pullManaFromInputSlot() {
        ItemStack stack = guiContainer.getItem(0);
        if (stack.isEmpty()) return false;
        int maxMana = getMaxMana();
        if (maxMana <= 0 || storedMana >= maxMana) return false;

        int space = maxMana - storedMana;

        if (stack.getItem() instanceof MagicCrystalItem crystal) {
            int value = crystal.getCrystalValue();
            if (value > space) return false;
            storedMana += value;
            stack.shrink(1);
            return true;
        }

        if (stack.getItem() instanceof ManaStorageItem) {
            int available = ManaStorageItem.getStoredMana(stack);
            if (available <= 0) return false;
            int toTransfer = Math.min(Math.min(available, space), TRANSFER_PER_TICK);
            if (toTransfer <= 0) return false;
            ManaStorageItem.setStoredMana(stack, available - toTransfer);
            storedMana += toTransfer;
            return true;
        }

        return false;
    }

    private boolean pushManaToOutputSlot() {
        ItemStack stack = guiContainer.getItem(1);
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof ManaStorageItem)) return false;
        if (storedMana <= 0) return false;

        int containerStored = ManaStorageItem.getStoredMana(stack);
        int containerSpace = ManaStorageItem.MAX_MANA - containerStored;
        if (containerSpace <= 0) return false;

        int toTransfer = Math.min(Math.min(storedMana, containerSpace), TRANSFER_PER_TICK);
        if (toTransfer <= 0) return false;

        ManaStorageItem.setStoredMana(stack, containerStored + toTransfer);
        storedMana -= toTransfer;
        return true;
    }

    public SimpleContainer getGuiContainer() {
        return guiContainer;
    }

    private void validateStructure(ServerLevel serverLevel) {
        CircleStructureCache cache = CircleStructureValidator.validate(serverLevel, worldPosition);
        this.detectedTier = cache.getTier();
        this.structureValid = cache.isValid();
        this.lastMissingBlockCount = cache.getMissingEntries() != null ? cache.getMissingEntries().size() : 0;
        this.structureDirty = false;
        this.lastValidationTime = serverLevel.getGameTime();
    }

    public void markStructureDirty() {
        this.structureDirty = true;
    }

    private CircleFunctionContext buildContext(ServerLevel serverLevel) {
        int maxMana = getMaxMana();
        int throughput = detectedTier != null ? detectedTier.getThroughputPerMinute() : 0;
        return new CircleFunctionContext(
                serverLevel,
                worldPosition,
                detectedTier,
                activeFunction,
                owner,
                storedMana,
                maxMana,
                throughput,
                new ArrayList<>(cachedCatalysts),
                powerLevel,
                durationLevel,
                efficiencyLevel,
                specialLevel
        );
    }

    public boolean activate() {
        if (!structureValid || activeFunction == null || functionSigil.isEmpty()) {
            return false;
        }
        if (level == null || level.isClientSide) {
            return false;
        }

        this.executor = CircleFunctionExecutorRegistry.get(activeFunction);
        if (this.executor == null) {
            return false;
        }

        if (level instanceof ServerLevel serverLevel) {
            CircleFunctionContext ctx = buildContext(serverLevel);
            if (!this.executor.canActivate(ctx)) {
                return false;
            }
            this.executor.onActivate(ctx);
            this.storedMana = ctx.getStoredMana();
        }

        this.active = true;
        this.stallTicks = 0;
        this.manaDebt = 0f;
        setChanged();
        syncToClient();
        return true;
    }

    public void deactivate() {
        if (this.executor != null && level instanceof ServerLevel serverLevel) {
            CircleFunctionContext ctx = buildContext(serverLevel);
            this.executor.onDeactivate(ctx);
            this.storedMana = ctx.getStoredMana();
        }
        this.active = false;
        this.executor = null;
        setChanged();
        syncToClient();
    }

    public void setFunction(CircleFunctionType function, ItemStack sigil) {

        if (this.sigilLocked && this.activeFunction != null) {
            return;
        }

        if (this.active) {
            deactivate();
        }

        if (this.activeFunction != function) {
            this.functionSettings.clear();
        }
        this.activeFunction = function;
        this.functionSigil = sigil == null ? ItemStack.EMPTY : sigil.copy();
        this.sigilLocked = true;

        if (level instanceof ServerLevel serverLevel) {
            validateStructure(serverLevel);
        }

        setChanged();
        syncToClient();
    }

    public boolean isSigilLocked() {
        return sigilLocked;
    }

    public int insertMana(int amount) {
        if (amount <= 0) return 0;
        int max = getMaxMana();
        int space = Math.max(0, max - storedMana);
        int accepted = Math.min(space, amount);
        if (accepted > 0) {
            storedMana += accepted;
            setChanged();
        }
        return accepted;
    }

    public int extractMana(int amount) {
        if (amount <= 0) return 0;
        int extracted = Math.min(storedMana, amount);
        if (extracted > 0) {
            storedMana -= extracted;
            setChanged();
        }
        return extracted;
    }

    public int getMaxMana() {
        return detectedTier != null ? detectedTier.getManaCapacity() : 0;
    }

    public CircleState getCircleState() {
        if (!enabled) return CircleState.DISABLED;
        if (!structureValid || detectedTier == null) return CircleState.INVALID;
        if (!active || activeFunction == null) return CircleState.IDLE;

        int graceTicks = CircleManaMath.computeGraceTicks(durationLevel);
        if (stallTicks > graceTicks) return CircleState.DORMANT;
        if (stallTicks > 0) return CircleState.FLICKERING;

        return CircleState.ACTIVE;
    }

    public CircleTier getDetectedTier() {
        return detectedTier;
    }

    public boolean isStructureValid() {
        return structureValid;
    }

    public CircleFunctionType getActiveFunction() {
        return activeFunction;
    }

    public ItemStack getFunctionSigil() {
        return functionSigil;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        setChanged();
    }

    public int getStoredMana() {
        return storedMana;
    }

    public int getStallTicks() {
        return stallTicks;
    }

    public UUID getOwner() {
        return owner;
    }

    public synchronized boolean claimOrAuthorize(UUID serverSender, boolean adminBypass) {
        if (serverSender == null) {
            return false;
        }
        if (adminBypass) {
            return true;
        }
        if (owner == null) {
            owner = serverSender;
            setChanged();
            syncToClient();
            return true;
        }
        return owner.equals(serverSender);
    }

    public boolean claimOrAuthorize(ServerPlayer player) {
        if (player == null || !player.serverLevel().getServer().isSameThread()) {
            return false;
        }
        return claimOrAuthorize(player.getUUID(), player.hasPermissions(2));
    }

    public static UUID loadOwnerFromNbt(CompoundTag tag) {
        return tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    public static void saveOwnerToNbt(CompoundTag tag, UUID owner) {
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public int getPowerLevel() {
        return powerLevel;
    }

    public int getDurationLevel() {
        return durationLevel;
    }

    public int getEfficiencyLevel() {
        return efficiencyLevel;
    }

    public int getSpecialLevel() {
        return specialLevel;
    }

    private void refreshAugmentLevels(ServerLevel level) {
        int haste = 0, eff = 0, pres = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        long r2 = (long) AUGMENT_SCAN_RADIUS * AUGMENT_SCAN_RADIUS;
        for (int dx = -AUGMENT_SCAN_RADIUS; dx <= AUGMENT_SCAN_RADIUS; dx++) {
            for (int dy = -AUGMENT_SCAN_RADIUS; dy <= AUGMENT_SCAN_RADIUS; dy++) {
                for (int dz = -AUGMENT_SCAN_RADIUS; dz <= AUGMENT_SCAN_RADIUS; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r2) continue;
                    m.set(worldPosition.getX() + dx, worldPosition.getY() + dy, worldPosition.getZ() + dz);
                    var block = level.getBlockState(m).getBlock();
                    if (block instanceof com.huige233.transcend.block.augment.AugmentRuneBlock rune) {
                        switch (rune.getAugmentType()) {
                            case HASTE -> haste++;
                            case EFFICIENCY -> eff++;
                            case PRESERVATION -> pres++;
                        }
                    }
                }
            }
        }
        int newPower = Math.min(haste, AUGMENT_CAP_PER_TYPE);
        int newEff = Math.min(eff, AUGMENT_CAP_PER_TYPE);
        int newDur = Math.min(pres, AUGMENT_CAP_PER_TYPE);
        if (newPower != powerLevel || newEff != efficiencyLevel || newDur != durationLevel) {
            powerLevel = newPower;
            efficiencyLevel = newEff;
            durationLevel = newDur;
            setChanged();
        }
    }

    private static final int AUGMENT_SCAN_RADIUS = 16;
    private static final int AUGMENT_CAP_PER_TYPE = 5;

    public List<ItemStack> getCachedCatalysts() {
        return cachedCatalysts;
    }

    public int getMissingBlockCount() {
        return lastMissingBlockCount;
    }

    public int getCatalystCount() {
        return cachedCatalysts.size();
    }

    public int getCatalystSatisfiedCount() {
        return cachedCatalysts.size();
    }

    public Map<String, Integer> getFunctionSettings() {
        return functionSettings;
    }

    public void setSettingValue(String key, int value) {
        if (key == null) return;
        CircleFunctionSettings.SettingDef def =
                CircleFunctionSettings.findDef(activeFunction, key);
        if (def == null) {

            return;
        }
        functionSettings.put(key, def.clamp(value));
        setChanged();
        syncToClient();
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.transcend.circle_core");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (player instanceof ServerPlayer serverPlayer && !claimOrAuthorize(serverPlayer)) {
            return null;
        }

        int settingsCount = activeFunction != null
                ? CircleFunctionSettings.getSettingsFor(activeFunction).size()
                : 0;
        return new CircleCoreMenu(containerId, inventory,
                worldPosition,
                detectedTier != null ? detectedTier.getLevel() : 0,
                storedMana,
                getMaxMana(),
                active,
                structureValid,
                activeFunction != null ? activeFunction.getId() : "",
                activeFunction != null ? activeFunction.getBaseUpkeepPerMinute() : 0f,
                settingsCount,
                sigilLocked,
                lastMissingBlockCount,
                getCatalystCount(),
                getCatalystSatisfiedCount());
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ManaHandlerCapability.MANA_HANDLER) {
            return manaCapHolder.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        manaCapHolder.invalidate();
    }

    public enum CircleState {
        INVALID,
        IDLE,
        ACTIVE,
        FLICKERING,
        DORMANT,
        DISABLED
    }
}
