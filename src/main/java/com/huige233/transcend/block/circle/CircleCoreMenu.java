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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/** 法阵核心容器菜单。 */
public class CircleCoreMenu extends AbstractContainerMenu {

    public static final String MENU_ID = "circle_core_menu";

    private final BlockPos corePos;

    private CircleCoreData data;

    private final Container container;

    public CircleCoreMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv, extraData.readBlockPos(), CircleCoreData.decode(extraData));
    }

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

    public CircleCoreMenu(int containerId, Inventory playerInv,
                          BlockPos corePos, int tier, int storedMana, int maxMana,
                          boolean active, boolean structureValid,
                          String functionId, float upkeepPerMin, int settingsCount) {
        this(containerId, playerInv, corePos, tier, storedMana, maxMana,
                active, structureValid, functionId, upkeepPerMin, settingsCount,
                false, 0, 0, 0);
    }

    public CircleCoreMenu(int containerId, Inventory playerInv,
                          BlockPos corePos, int tier, int storedMana, int maxMana,
                          boolean active, boolean structureValid,
                          String functionId, float upkeepPerMin) {
        this(containerId, playerInv, corePos, tier, storedMana, maxMana,
                active, structureValid, functionId, upkeepPerMin, 0,
                false, 0, 0, 0);
    }

    public CircleCoreMenu(int containerId, Inventory playerInv, BlockPos corePos, CircleCoreData data) {
        super(com.huige233.transcend.init.ModMenus.CIRCLE_CORE_MENU.get(), containerId);
        this.corePos = corePos;
        this.data = data;

        Container beContainer = null;
        if (playerInv.player.level() != null) {
            net.minecraft.world.level.block.entity.BlockEntity be =
                    playerInv.player.level().getBlockEntity(corePos);
            if (be instanceof MagicCircleCoreBlockEntity coreBe) {
                beContainer = coreBe.getGuiContainer();
            }
        }
        this.container = beContainer != null ? beContainer : new SimpleContainer(2);

        this.addSlot(new Slot(this.container, 0, 26, 47) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof MagicCrystalItem
                        || stack.getItem() instanceof ManaStorageItem;
            }
        });

        this.addSlot(new Slot(this.container, 1, 134, 47) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof ManaStorageItem;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 94 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 152));
        }
    }

    public BlockPos getCorePos() {
        return corePos;
    }

    public CircleCoreData getData() {
        return data;
    }

    public void updateData(CircleCoreData newData) {
        this.data = newData;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        if (!isAuthorized(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack returnStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            returnStack = slotStack.copy();

            if (index < 2) {
                if (!this.moveItemStackTo(slotStack, 2, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {

                if (slotStack.getItem() instanceof MagicCrystalItem
                        || slotStack.getItem() instanceof ManaStorageItem) {

                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) {

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

        return isAuthorized(player) && player.distanceToSqr(
                corePos.getX() + 0.5,
                corePos.getY() + 0.5,
                corePos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player) {
        if (isAuthorized(player)) {
            super.clicked(slotId, button, clickType, player);
        }
    }

    private boolean isAuthorized(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return player.level().isClientSide;
        }
        return player.level().getBlockEntity(corePos) instanceof MagicCircleCoreBlockEntity core
                && core.claimOrAuthorize(serverPlayer);
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);

    }

    public static final class CircleCoreData {
        public final int tier;
        public final int storedMana;
        public final int maxMana;
        public final boolean active;
        public final boolean structureValid;
        public final String functionId;
        public final float upkeepPerMin;
        public final int settingsCount;

        public final boolean sigilLocked;
        public final int missingBlockCount;
        public final int catalystCount;
        public final int catalystSatisfiedCount;

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

        public CircleCoreData(int tier, int storedMana, int maxMana,
                              boolean active, boolean structureValid,
                              String functionId, float upkeepPerMin,
                              int settingsCount) {
            this(tier, storedMana, maxMana, active, structureValid, functionId, upkeepPerMin,
                    settingsCount, false, 0, 0, 0);
        }

        public CircleCoreData(int tier, int storedMana, int maxMana,
                              boolean active, boolean structureValid,
                              String functionId, float upkeepPerMin) {
            this(tier, storedMana, maxMana, active, structureValid, functionId, upkeepPerMin,
                    0, false, 0, 0, 0);
        }

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
