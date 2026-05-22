package com.huige233.transcend.block.circle;

import com.huige233.transcend.items.MagicCrystalItem;
import com.huige233.transcend.items.ManaStorageItem;
import com.huige233.transcend.items.circle.FunctionSigilItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 法环核心容器菜单。
 *
 * <p>菜单包含两个槽位：
 * <ul>
 *     <li>槽位 0：魔力源输入槽（接受 {@link MagicCrystalItem} 或 {@link ManaStorageItem}，用于充能核心）</li>
 *     <li>槽位 1：魔力抽取槽（仅接受 {@link ManaStorageItem}，核心自动将魔力注入其中）</li>
 * </ul>
 *
 * <p>菜单初始化时通过 {@link FriendlyByteBuf} 接收来自服务端的法环状态快照
 * （{@link CircleCoreData}），用于在客户端 GUI 中显示信息。
 * 实时状态由 S2CCircleStatus 数据包推送刷新。
 */
public class CircleCoreMenu extends AbstractContainerMenu {

    /** 菜单类型 ID，将在 ModMenus 注册时使用。 */
    public static final String MENU_ID = "circle_core_menu";

    /** 法环核心方块坐标。 */
    private final BlockPos corePos;

    /** 最近一次同步的法环状态快照。 */
    private CircleCoreData data;

    /** 容器：两个槽位（魔力源输入 + 魔力抽取输出）。 */
    private final Container container;

    // ============================================================
    // 客户端构造器（通过 FriendlyByteBuf 解码服务端发送的数据）
    // ============================================================
    public CircleCoreMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv, extraData.readBlockPos(), CircleCoreData.decode(extraData));
    }

    // ============================================================
    // 服务端构造器（原始参数 — 全字段）
    // ============================================================
    public CircleCoreMenu(int containerId, Inventory playerInv,
                          BlockPos corePos, int tier, int storedMana, int maxMana,
                          boolean active, boolean structureValid,
                          String functionId, float upkeepPerMin, int settingsCount,
                          boolean sigilLocked, int missingBlockCount,
                          int catalystCount, int catalystSatisfiedCount) {
        this(containerId, playerInv, corePos,
                new CircleCoreData(tier, storedMana, maxMana, active, structureValid,
                        functionId, upkeepPerMin, settingsCount,
                        sigilLocked, missingBlockCount, catalystCount, catalystSatisfiedCount));
    }

    /** 兼容签名：仅传 settingsCount，新字段填默认值。 */
    public CircleCoreMenu(int containerId, Inventory playerInv,
                          BlockPos corePos, int tier, int storedMana, int maxMana,
                          boolean active, boolean structureValid,
                          String functionId, float upkeepPerMin, int settingsCount) {
        this(containerId, playerInv, corePos, tier, storedMana, maxMana,
                active, structureValid, functionId, upkeepPerMin, settingsCount,
                false, 0, 0, 0);
    }

    /** 兼容旧签名（不带 settingsCount，全部新字段填 0/false）。 */
    public CircleCoreMenu(int containerId, Inventory playerInv,
                          BlockPos corePos, int tier, int storedMana, int maxMana,
                          boolean active, boolean structureValid,
                          String functionId, float upkeepPerMin) {
        this(containerId, playerInv, corePos, tier, storedMana, maxMana,
                active, structureValid, functionId, upkeepPerMin, 0,
                false, 0, 0, 0);
    }

    // ============================================================
    // 服务端构造器（数据对象）
    // ============================================================
    public CircleCoreMenu(int containerId, Inventory playerInv, BlockPos corePos, CircleCoreData data) {
        super(com.huige233.transcend.init.ModMenus.CIRCLE_CORE_MENU.get(), containerId);
        this.corePos = corePos;
        this.data = data;

        // 优先使用方块实体的真实容器（服务端）；客户端使用占位容器
        Container beContainer = null;
        if (playerInv.player.level() != null) {
            net.minecraft.world.level.block.entity.BlockEntity be =
                    playerInv.player.level().getBlockEntity(corePos);
            if (be instanceof MagicCircleCoreBlockEntity coreBe) {
                beContainer = coreBe.getGuiContainer();
            }
        }
        this.container = beContainer != null ? beContainer : new SimpleContainer(2);

        // 槽位 0：魔力源输入槽（MagicCrystalItem 或 ManaStorageItem）
        this.addSlot(new Slot(this.container, 0, 26, 47) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof MagicCrystalItem
                        || stack.getItem() instanceof ManaStorageItem;
            }
        });

        // 槽位 1：魔力抽取输出槽（仅 ManaStorageItem）
        this.addSlot(new Slot(this.container, 1, 134, 47) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof ManaStorageItem;
            }
        });

        // 玩家主背包 9x3
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 94 + row * 18));
            }
        }

        // 玩家快捷栏
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 152));
        }
    }

    // ============================================================
    // Getter
    // ============================================================

    public BlockPos getCorePos() {
        return corePos;
    }

    public CircleCoreData getData() {
        return data;
    }

    /** 由客户端 S2CCircleStatus 包处理器调用，更新菜单状态。 */
    public void updateData(CircleCoreData newData) {
        this.data = newData;
    }

    // ============================================================
    // 菜单基础行为
    // ============================================================

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack returnStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            returnStack = slotStack.copy();

            // 0-1 为容器槽，其后为玩家背包
            if (index < 2) {
                if (!this.moveItemStackTo(slotStack, 2, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家 → 容器
                if (slotStack.getItem() instanceof MagicCrystalItem
                        || slotStack.getItem() instanceof ManaStorageItem) {
                    // 先尝试输入槽
                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                        // 如果是 ManaStorageItem，也可以尝试输出槽
                        if (slotStack.getItem() instanceof ManaStorageItem) {
                            if (!this.moveItemStackTo(slotStack, 1, 2, false)) {
                                return ItemStack.EMPTY;
                            }
                        } else {
                            return ItemStack.EMPTY;
                        }
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == returnStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotStack);
        }

        return returnStack;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        // 玩家必须在法环核心 8 格范围内
        return player.distanceToSqr(
                corePos.getX() + 0.5,
                corePos.getY() + 0.5,
                corePos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        // 物品留在方块实体中，不弹回玩家（容器属于 BlockEntity）
    }

    // ============================================================
    // CircleCoreData：法环状态快照
    // ============================================================

    /**
     * 法环核心状态的简易快照，用于客户端 GUI 显示。
     * 通过 FriendlyByteBuf 编解码，可经由菜单初始化或 S2CCircleStatus 包传输。
     *
     * <p>编解码字段顺序（仅追加，永不修改前缀）：
     * <pre>
     *  tier (varInt)
     *  storedMana (varInt)
     *  maxMana (varInt)
     *  active (boolean)
     *  structureValid (boolean)
     *  functionId (utf)
     *  upkeepPerMin (float)
     *  settingsCount (varInt)
     *  sigilLocked (boolean)               -- v2 追加
     *  missingBlockCount (varInt)          -- v2 追加
     *  catalystCount (varInt)              -- v2 追加
     *  catalystSatisfiedCount (varInt)     -- v2 追加
     * </pre>
     */
    public static final class CircleCoreData {
        public final int tier;             // 0 表示未知 / 无效结构
        public final int storedMana;
        public final int maxMana;
        public final boolean active;
        public final boolean structureValid;
        public final String functionId;    // 空串表示无功能
        public final float upkeepPerMin;
        public final int settingsCount;    // 当前功能拥有的可配置项数量

        // v2 追加字段
        public final boolean sigilLocked;          // 符印是否已锁定（一次性安装）
        public final int missingBlockCount;        // 距离当前 tier 还缺多少结构方块
        public final int catalystCount;            // 周围催化基座数量
        public final int catalystSatisfiedCount;   // 已放入催化剂的数量

        public CircleCoreData(int tier, int storedMana, int maxMana,
                              boolean active, boolean structureValid,
                              String functionId, float upkeepPerMin,
                              int settingsCount,
                              boolean sigilLocked, int missingBlockCount,
                              int catalystCount, int catalystSatisfiedCount) {
            this.tier = tier;
            this.storedMana = storedMana;
            this.maxMana = maxMana;
            this.active = active;
            this.structureValid = structureValid;
            this.functionId = functionId == null ? "" : functionId;
            this.upkeepPerMin = upkeepPerMin;
            this.settingsCount = Math.max(0, settingsCount);
            this.sigilLocked = sigilLocked;
            this.missingBlockCount = Math.max(0, missingBlockCount);
            this.catalystCount = Math.max(0, catalystCount);
            this.catalystSatisfiedCount = Math.max(0, Math.min(catalystSatisfiedCount, this.catalystCount));
        }

        /** 兼容签名（旧版 8 字段）。 */
        public CircleCoreData(int tier, int storedMana, int maxMana,
                              boolean active, boolean structureValid,
                              String functionId, float upkeepPerMin,
                              int settingsCount) {
            this(tier, storedMana, maxMana, active, structureValid, functionId, upkeepPerMin,
                    settingsCount, false, 0, 0, 0);
        }

        /** 兼容签名（旧版 7 字段）。 */
        public CircleCoreData(int tier, int storedMana, int maxMana,
                              boolean active, boolean structureValid,
                              String functionId, float upkeepPerMin) {
            this(tier, storedMana, maxMana, active, structureValid, functionId, upkeepPerMin,
                    0, false, 0, 0, 0);
        }

        /** 空状态（用于占位）。 */
        public static CircleCoreData empty() {
            return new CircleCoreData(0, 0, 0, false, false, "", 0f,
                    0, false, 0, 0, 0);
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(tier);
            buf.writeVarInt(storedMana);
            buf.writeVarInt(maxMana);
            buf.writeBoolean(active);
            buf.writeBoolean(structureValid);
            buf.writeUtf(functionId);
            buf.writeFloat(upkeepPerMin);
            buf.writeVarInt(settingsCount);
            buf.writeBoolean(sigilLocked);
            buf.writeVarInt(missingBlockCount);
            buf.writeVarInt(catalystCount);
            buf.writeVarInt(catalystSatisfiedCount);
        }

        public static CircleCoreData decode(FriendlyByteBuf buf) {
            int tier = buf.readVarInt();
            int storedMana = buf.readVarInt();
            int maxMana = buf.readVarInt();
            boolean active = buf.readBoolean();
            boolean structureValid = buf.readBoolean();
            String functionId = buf.readUtf();
            float upkeepPerMin = buf.readFloat();
            int settingsCount = buf.readVarInt();
            // v2 字段（如果上游不写则 buffer 会读 underflow；
            // 现在所有发送点都已升级，安全读取）
            boolean sigilLocked = buf.readBoolean();
            int missingBlockCount = buf.readVarInt();
            int catalystCount = buf.readVarInt();
            int catalystSatisfiedCount = buf.readVarInt();
            return new CircleCoreData(tier, storedMana, maxMana, active, structureValid,
                    functionId, upkeepPerMin, settingsCount,
                    sigilLocked, missingBlockCount, catalystCount, catalystSatisfiedCount);
        }
    }
}
