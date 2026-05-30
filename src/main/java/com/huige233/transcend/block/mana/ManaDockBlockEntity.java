package com.huige233.transcend.block.mana;

import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.items.ManaStorageItem;
import com.huige233.transcend.mana.IManaHandler;
import com.huige233.transcend.mana.ManaHandlerCapability;
import com.huige233.transcend.mana.SimpleManaStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mana Dock 方块实体：1 槽 ManaStorage + 内部缓冲 + 三模式自动充放电。
 *
 * <p>tick 节奏 5 tick = 4 Hz；每次最多移动 {@link #PER_TICK} CM。
 * 内部缓冲既是网络 IO 的平滑层，也是单边不可达情况下的暂存。
 *
 * <p>NBT 持久化字段：{@code Buffer} (CompoundTag)、{@code Item} (ItemStack)、{@code Mode} (byte)。
 */
public class ManaDockBlockEntity extends BlockEntity {

    public static final String BE_ID = "mana_dock_be";

    /** 内部缓冲容量（与 mana_reservoir 同级 1/8 用作平滑层）。 */
    public static final int CAPACITY = 256;
    /** 单 tick 在任一阶段的最大转移量。 */
    public static final int PER_TICK = 16;
    /** 主 tick 间隔（ticks）。 */
    private static final int TICK_INTERVAL = 5;

    public enum Mode {
        OFF, CHARGE, DRAIN;

        public Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private final SimpleManaStorage buffer;
    private final LazyOptional<IManaHandler> manaCap;
    private ItemStack storageItem = ItemStack.EMPTY;
    private Mode mode = Mode.CHARGE;
    private int tickCounter = 0;

    public ManaDockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_DOCK_BE.get(), pos, state);
        this.buffer = new SimpleManaStorage(CAPACITY, PER_TICK, PER_TICK);
        this.manaCap = LazyOptional.of(() -> this.buffer);
    }

    // ============================================================
    // 状态访问
    // ============================================================

    public Mode getMode() { return mode; }

    public Mode cycleMode() {
        mode = mode.next();
        setChanged();
        return mode;
    }

    public ItemStack getStorageItem() { return storageItem; }

    public int getBuffered() { return buffer.getManaStored(); }

    public int getCapacity() { return buffer.getMaxManaStored(); }

    /** 比较器信号 = 缓冲百分比映射到 0-15。 */
    public int getRedstoneSignal() {
        if (buffer.getMaxManaStored() <= 0) return 0;
        return Math.min(15, (int) ((long) buffer.getManaStored() * 15L / buffer.getMaxManaStored()));
    }

    /**
     * 把当前持物 stack 放入 dock；返回原 dock 中的 stack（可为空）。
     * 仅当传入 stack 是 ManaStorageItem 才接受。
     */
    public ItemStack swapStorageItem(ItemStack incoming) {
        if (!incoming.isEmpty() && !(incoming.getItem() instanceof ManaStorageItem)) {
            return incoming;
        }
        ItemStack old = this.storageItem;
        this.storageItem = incoming.isEmpty() ? ItemStack.EMPTY : incoming.copyWithCount(1);
        setChanged();
        return old;
    }

    public ItemStack takeStorageItem() {
        ItemStack old = this.storageItem;
        this.storageItem = ItemStack.EMPTY;
        setChanged();
        return old;
    }

    /** 单行状态总览，供 actionbar 提示。 */
    public MutableComponent statusLine() {
        String modeKey = switch (mode) {
            case CHARGE -> "msg.transcend.mana_dock.mode_charge";
            case DRAIN  -> "msg.transcend.mana_dock.mode_drain";
            default     -> "msg.transcend.mana_dock.mode_off";
        };
        ChatFormatting modeColor = switch (mode) {
            case CHARGE -> ChatFormatting.AQUA;
            case DRAIN  -> ChatFormatting.GOLD;
            default     -> ChatFormatting.GRAY;
        };
        int itemMana = storageItem.isEmpty() ? 0 : ManaStorageItem.getStoredMana(storageItem);
        int itemMax  = storageItem.isEmpty() ? 0 : ManaStorageItem.getMaxMana(storageItem);

        return Component.translatable(modeKey).withStyle(modeColor)
                .append(Component.literal(" §7| §b" + buffer.getManaStored() + "/" + buffer.getMaxManaStored() + " CM"))
                .append(Component.literal(" §7| §d" + itemMana + "/" + itemMax + " §7(item)"));
    }

    // ============================================================
    // Tick 主逻辑
    // ============================================================

    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaDockBlockEntity be) {
        be.tickCounter++;
        if (be.tickCounter < TICK_INTERVAL) return;
        be.tickCounter = 0;
        if (be.mode == Mode.OFF) return;

        boolean changed = false;

        if (be.mode == Mode.CHARGE) {
            // 1) 抽取邻居 (canExtract) → 缓冲
            int free = be.buffer.getMaxManaStored() - be.buffer.getManaStored();
            if (free > 0) {
                changed |= pullFromNeighbors(level, pos, be, Math.min(free, PER_TICK));
            }
            // 2) 缓冲 → 物品
            if (!be.storageItem.isEmpty()) {
                int avail = be.buffer.extractMana(PER_TICK, true);
                if (avail > 0) {
                    int itemSpace = ManaStorageItem.getMaxMana(be.storageItem) - ManaStorageItem.getStoredMana(be.storageItem);
                    int move = Math.min(avail, itemSpace);
                    if (move > 0) {
                        be.buffer.extractMana(move, false);
                        ManaStorageItem.setStoredMana(be.storageItem,
                                ManaStorageItem.getStoredMana(be.storageItem) + move);
                        changed = true;
                    }
                }
            }
        } else { // DRAIN
            // 1) 物品 → 缓冲
            if (!be.storageItem.isEmpty()) {
                int free = be.buffer.getMaxManaStored() - be.buffer.getManaStored();
                int avail = ManaStorageItem.getStoredMana(be.storageItem);
                int move = Math.min(PER_TICK, Math.min(free, avail));
                if (move > 0) {
                    int accepted = be.buffer.receiveMana(move, false);
                    if (accepted > 0) {
                        ManaStorageItem.setStoredMana(be.storageItem,
                                ManaStorageItem.getStoredMana(be.storageItem) - accepted);
                        changed = true;
                    }
                }
            }
            // 2) 缓冲 → 邻居 (canReceive)
            if (be.buffer.getManaStored() > 0) {
                changed |= pushToNeighbors(level, pos, be, PER_TICK);
            }
        }

        if (changed) be.setChanged();
    }

    /** 单 tick 从所有可抽取相邻 IManaHandler 中拉取魔力到缓冲，预算分摊。 */
    private static boolean pullFromNeighbors(Level level, BlockPos pos, ManaDockBlockEntity be, int budget) {
        boolean any = false;
        for (Direction dir : Direction.values()) {
            if (budget <= 0) break;
            BlockEntity nb = level.getBlockEntity(pos.relative(dir));
            if (nb == null || nb == be) continue;
            IManaHandler h = nb.getCapability(ManaHandlerCapability.MANA_HANDLER, dir.getOpposite()).resolve().orElse(null);
            if (h == null || !h.canExtract()) continue;
            int sim = h.extractMana(budget, true);
            if (sim <= 0) continue;
            int accepted = be.buffer.receiveMana(sim, false);
            if (accepted > 0) {
                h.extractMana(accepted, false);
                budget -= accepted;
                any = true;
            }
        }
        return any;
    }

    /** 单 tick 把缓冲推送到所有可接收相邻 IManaHandler，预算分摊。 */
    private static boolean pushToNeighbors(Level level, BlockPos pos, ManaDockBlockEntity be, int budget) {
        boolean any = false;
        for (Direction dir : Direction.values()) {
            if (budget <= 0 || be.buffer.getManaStored() <= 0) break;
            BlockEntity nb = level.getBlockEntity(pos.relative(dir));
            if (nb == null || nb == be) continue;
            IManaHandler h = nb.getCapability(ManaHandlerCapability.MANA_HANDLER, dir.getOpposite()).resolve().orElse(null);
            if (h == null || !h.canReceive()) continue;
            int avail = be.buffer.extractMana(budget, true);
            if (avail <= 0) continue;
            int accepted = h.receiveMana(avail, false);
            if (accepted > 0) {
                be.buffer.extractMana(accepted, false);
                budget -= accepted;
                any = true;
            }
        }
        return any;
    }

    // ============================================================
    // NBT
    // ============================================================

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Buffer", buffer.serializeNBT());
        tag.putByte("Mode", (byte) mode.ordinal());
        if (!storageItem.isEmpty()) {
            tag.put("Item", storageItem.save(new CompoundTag()));
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Buffer")) buffer.deserializeNBT(tag.getCompound("Buffer"));
        if (tag.contains("Mode")) {
            int idx = tag.getByte("Mode") & 0xFF;
            Mode[] vals = Mode.values();
            mode = idx < vals.length ? vals[idx] : Mode.CHARGE;
        }
        storageItem = tag.contains("Item") ? ItemStack.of(tag.getCompound("Item")) : ItemStack.EMPTY;
    }

    // ============================================================
    // Forge Capability
    // ============================================================

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ManaHandlerCapability.MANA_HANDLER) {
            return manaCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        manaCap.invalidate();
    }
}
