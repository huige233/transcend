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

/**
 * 法环核心方块实体。
 *
 * <p>负责：
 * <ul>
 *     <li>结构验证缓存（Phase 3 实装）</li>
 *     <li>当前功能与符印的持有</li>
 *     <li>魔力存储与消耗节奏（manaDebt / stallTicks）</li>
 *     <li>每 20 tick 调用功能执行器</li>
 *     <li>NBT 持久化全部状态</li>
 * </ul>
 */
public class MagicCircleCoreBlockEntity extends BlockEntity implements MenuProvider {

    /** 方块实体注册 ID */
    public static final String BE_ID = "circle_core_be";

    // ============================================================
    // 状态字段
    // ============================================================

    // 结构缓存
    private CircleTier detectedTier = null;
    private boolean structureValid = false;
    private long lastValidationTime = 0;
    private boolean structureDirty = true;
    /** 上次校验缺失的方块数量（GUI 显示用） */
    private int lastMissingBlockCount = 0;

    // 功能
    private CircleFunctionType activeFunction = null;
    private ItemStack functionSigil = ItemStack.EMPTY;
    private CircleFunctionExecutor executor = null;
    private boolean active = false;
    private boolean enabled = true;

    // 魔力
    private int storedMana = 0;
    private float manaDebt = 0f;
    private int stallTicks = 0;
    private int tickCounter = 0;

    // 所有权
    private UUID owner = null;

    // 强化（来自触媒石座上的增幅板）
    private int powerLevel = 0;
    private int durationLevel = 0;
    private int efficiencyLevel = 0;
    private int specialLevel = 0;

    // 催化剂（从邻近的 CatalystPlinth 读取）
    private final List<ItemStack> cachedCatalysts = new ArrayList<>();

    // 功能特定的设置（按设置 ID -> 整型值）
    private Map<String, Integer> functionSettings = new HashMap<>();

    // 符印是否已锁定（一次性安装后不可替换）
    private boolean sigilLocked = false;

    // GUI 物品槽（槽 0 = 魔力源输入，槽 1 = 魔力抽取输出）
    private final SimpleContainer guiContainer = new SimpleContainer(2);

    /**
     * Forge capability 适配器：将核心的 storedMana 暴露为 IManaHandler。
     * 双向支持：可被 spreader/burst 注入，也可被相邻 receiver/burst 抽取。
     */
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

    /** Cap 的 LazyOptional 包装，便于 invalidate */
    private final LazyOptional<IManaHandler> manaCapHolder = LazyOptional.of(() -> manaCapability);

    // ============================================================
    // 构造
    // ============================================================

    public MagicCircleCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CIRCLE_CORE_BE.get(), pos, state);
    }

    // ============================================================
    // NBT 序列化
    // ============================================================

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

        if (owner != null) {
            tag.putUUID("Owner", owner);
        }

        tag.putInt("PowerLevel", powerLevel);
        tag.putInt("DurationLevel", durationLevel);
        tag.putInt("EfficiencyLevel", efficiencyLevel);
        tag.putInt("SpecialLevel", specialLevel);

        CircleFunctionSettings.saveSettings(tag, functionSettings);

        tag.putBoolean("SigilLocked", sigilLocked);

        // GUI 物品槽
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

        this.storedMana = tag.getInt("StoredMana");
        this.manaDebt = tag.getFloat("ManaDebt");
        this.stallTicks = tag.getInt("StallTicks");

        this.owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;

        this.powerLevel = tag.getInt("PowerLevel");
        this.durationLevel = tag.getInt("DurationLevel");
        this.efficiencyLevel = tag.getInt("EfficiencyLevel");
        this.specialLevel = tag.getInt("SpecialLevel");

        this.functionSettings = CircleFunctionSettings.loadSettings(tag);

        this.sigilLocked = tag.getBoolean("SigilLocked");

        // GUI 物品槽
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

        // 加载后标记结构需重新验证
        this.structureDirty = true;
    }

    // ============================================================
    // 服务端 Tick
    // ============================================================

    public static void serverTick(Level level, BlockPos pos, BlockState state, MagicCircleCoreBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        // 内部计数器，每 20 tick 执行一次主逻辑
        be.tickCounter++;
        if (be.tickCounter < 20) return;
        be.tickCounter = 0;

        // 每秒一次：刷新来自周围 augment_rune 的强化等级
        be.refreshAugmentLevels(serverLevel);

        // 0. GUI 物品槽自动转移（始终执行，无论是否激活）
        boolean slotChanged = false;
        slotChanged |= be.pullManaFromInputSlot();
        slotChanged |= be.pushManaToOutputSlot();
        if (slotChanged) {
            be.setChanged();
            be.syncToClient();
        }

        // 1. 结构脏标记 → 重新验证（占位实现）
        if (be.structureDirty) {
            be.validateStructure(serverLevel);
        }

        // 2. 未激活 / 未启用 / 无功能 / 无执行器 → 跳过
        if (!be.active || !be.enabled || be.activeFunction == null || be.executor == null) {
            return;
        }

        // 3. 魔力消耗（每分钟开销 → 每次 tick 调用消耗 = upkeep / 60）
        // 获取区块魔力浓度用于计算环境质量系数
        float chunkMana = com.huige233.transcend.world.mana.ChunkManaSavedData
                .get(serverLevel).getMana(new net.minecraft.world.level.ChunkPos(pos));
        float upkeep = CircleManaMath.computeFinalUpkeep(
                be.activeFunction.getBaseUpkeepPerMinute(),
                be.powerLevel, be.durationLevel, be.efficiencyLevel, be.specialLevel,
                1,         // TODO: 来自区块扫描的干涉计数（同一区块同主人法阵数）
                chunkMana
        );

        // 1 minute = 1200 ticks，本方法每 20 ticks 调用一次 → 每分钟 60 次
        be.manaDebt += upkeep / 60.0f;

        if (be.manaDebt >= 1.0f) {
            int requested = (int) be.manaDebt;
            int paid = Math.min(requested, be.storedMana);
            be.storedMana -= paid;
            be.manaDebt -= paid;

            if (paid < requested) {
                be.stallTicks += 20;
            } else {
                be.stallTicks = 0;
            }
        }

        // 4. 状态判断：超过宽限时间则休眠
        int graceTicks = CircleManaMath.computeGraceTicks(be.durationLevel);
        if (be.stallTicks > graceTicks) {
            // 休眠：不执行功能
            return;
        }

        // 5. 执行功能
        CircleFunctionContext ctx = be.buildContext(serverLevel);
        be.executor.tick(ctx);
        // 回写消耗的魔力
        be.storedMana = ctx.getStoredMana();

        be.setChanged();
    }

    // ============================================================
    // GUI 物品槽自动转移
    // ============================================================

    /** 每次转移的最大 CM 量 */
    private static final int TRANSFER_PER_TICK = 8;

    /**
     * 从输入槽（槽 0）抽取魔力充入核心。
     * 支持 MagicCrystalItem（消耗物品）和 ManaStorageItem（抽取内部存储）。
     */
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

    /**
     * 将核心魔力推送到输出槽（槽 1）中的 ManaStorageItem。
     */
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

    /** 获取 GUI 容器引用（供 CircleCoreMenu 使用） */
    public SimpleContainer getGuiContainer() {
        return guiContainer;
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    /**
     * 结构验证占位实现。
     * TODO Phase 3: 检查多方块结构、催化石座、增幅板等。
     */
    private void validateStructure(ServerLevel serverLevel) {
        CircleStructureCache cache = CircleStructureValidator.validate(serverLevel, worldPosition);
        this.detectedTier = cache.getTier();
        this.structureValid = cache.isValid();
        this.lastMissingBlockCount = cache.getMissingEntries() != null ? cache.getMissingEntries().size() : 0;
        this.structureDirty = false;
        this.lastValidationTime = serverLevel.getGameTime();
    }

    /** 标记结构需要重新验证（被外部方块变化触发时调用） */
    public void markStructureDirty() {
        this.structureDirty = true;
    }

    /** 构造一份功能执行上下文 */
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

    /**
     * 尝试激活法环。
     * 必须满足：结构有效 + 已设置功能 + 已设置符印。
     */
    public boolean activate() {
        if (!structureValid || activeFunction == null || functionSigil.isEmpty()) {
            return false;
        }
        if (level == null || level.isClientSide) {
            return false;
        }

        // 从注册表获取功能执行器
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

    /** 关闭法环 */
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

    /** 设置当前功能与对应符印（一次性安装，安装后锁定不可替换） */
    public void setFunction(CircleFunctionType function, ItemStack sigil) {
        // 符印已锁定，拒绝替换
        if (this.sigilLocked && this.activeFunction != null) {
            return;
        }
        // 切换功能时若正在运行，先关闭
        if (this.active) {
            deactivate();
        }
        // 切换到不同功能时清空旧的功能设置（保留同功能的设置）
        if (this.activeFunction != function) {
            this.functionSettings.clear();
        }
        this.activeFunction = function;
        this.functionSigil = sigil == null ? ItemStack.EMPTY : sigil.copy();
        this.sigilLocked = true; // 安装后立即锁定
        
        // 立即验证结构
        if (level instanceof ServerLevel serverLevel) {
            validateStructure(serverLevel);
        }
        
        setChanged();
        syncToClient();
    }

    /** 符印是否已锁定 */
    public boolean isSigilLocked() {
        return sigilLocked;
    }

    /**
     * 注入魔力。返回实际接收的数量（受最大容量限制）。
     */
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

    /**
     * 抽取魔力。返回实际抽取的数量。
     */
    public int extractMana(int amount) {
        if (amount <= 0) return 0;
        int extracted = Math.min(storedMana, amount);
        if (extracted > 0) {
            storedMana -= extracted;
            setChanged();
        }
        return extracted;
    }

    /** 获取当前最大魔力容量 */
    public int getMaxMana() {
        return detectedTier != null ? detectedTier.getManaCapacity() : 0;
    }

    /** 获取当前法环综合状态 */
    public CircleState getCircleState() {
        if (!enabled) return CircleState.DISABLED;
        if (!structureValid || detectedTier == null) return CircleState.INVALID;
        if (!active || activeFunction == null) return CircleState.IDLE;

        int graceTicks = CircleManaMath.computeGraceTicks(durationLevel);
        if (stallTicks > graceTicks) return CircleState.DORMANT;
        if (stallTicks > 0) return CircleState.FLICKERING;

        return CircleState.ACTIVE;
    }

    // ============================================================
    // 简单 Getter / Setter
    // ============================================================

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

    /**
     * 扫描法环核心 {@value #AUGMENT_SCAN_RADIUS} 球范围内的 {@link com.huige233.transcend.block.augment.AugmentRuneBlock}
     * 并把同类型数量映射到法环的强化等级字段：HASTE → powerLevel、EFFICIENCY → efficiencyLevel、PRESERVATION → durationLevel。
     * 同类型计数上限 {@value #AUGMENT_CAP_PER_TYPE}。仅当某个等级与上一帧不同才写回字段并 markDirty。
     */
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

    /** 上次结构校验中缺失的方块数量。 */
    public int getMissingBlockCount() {
        return lastMissingBlockCount;
    }

    /** 当前催化基座数量（已缓存的催化剂列表大小）。 */
    public int getCatalystCount() {
        return cachedCatalysts.size();
    }

    /** 当前已放有催化剂的基座数量。
     *  目前 cachedCatalysts 只存非空催化剂，因此 satisfied == count。
     *  未来如果区分"基座存在但空载"则需要在此调整。 */
    public int getCatalystSatisfiedCount() {
        return cachedCatalysts.size();
    }

    /** 获取功能设置的可读视图（不可修改） */
    public Map<String, Integer> getFunctionSettings() {
        return functionSettings;
    }

    /**
     * 修改某个功能设置项。设置变更会触发 NBT 持久化与客户端同步。
     * 仅接受当前激活功能定义的合法 key（值会被夹紧到 [min,max]）。
     */
    public void setSettingValue(String key, int value) {
        if (key == null) return;
        CircleFunctionSettings.SettingDef def =
                CircleFunctionSettings.findDef(activeFunction, key);
        if (def == null) {
            // 未知 key 或当前功能无此设置 — 忽略
            return;
        }
        functionSettings.put(key, def.clamp(value));
        setChanged();
        syncToClient();
    }

    // ============================================================
    // 客户端同步
    // ============================================================

    /** 通知客户端方块实体数据已更新 */
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

    // ============================================================
    // MenuProvider
    // ============================================================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.transcend.circle_core");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        // 服务端构造：直接传参（客户端通过 FriendlyByteBuf 在 openScreen 中同步）
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

    // ============================================================
    // Forge Capability — 暴露 IManaHandler 让 Botania 风格的
    // 魔力流体系（Spreader/Burst/Pool）能与法环核心交互。
    // ============================================================

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

    // ============================================================
    // 内部枚举
    // ============================================================

    /** 法环综合状态枚举 */
    public enum CircleState {
        INVALID,    // 结构无效
        IDLE,       // 空闲（结构有效但未激活）
        ACTIVE,     // 正常运行
        FLICKERING, // 闪烁（魔力短暂不足，宽限期内）
        DORMANT,    // 休眠（超过宽限期，等待补充魔力）
        DISABLED    // 被禁用（玩家手动关闭）
    }
}
