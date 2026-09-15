package com.huige233.transcend.menu;

import com.huige233.transcend.block.BlackHoleSeedBreederBlockEntity;
import com.huige233.transcend.init.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/** 管理培育机输入输出槽位，锁定加工中的容器并同步进度、能量和运行状态。 */
public class BlackHoleSeedBreederMenu extends AbstractContainerMenu {
    public static final int MACHINE_SLOTS = 4;
    public static final int INVENTORY_Y = 132;
    public static final int HOTBAR_Y = 190;
    private static final int DATA_COUNT = 8;
    private final ContainerData data;
    private final BlockPos machinePos;
    private final Player owner;
    private final Level level;
    private final BlackHoleSeedBreederBlockEntity blockEntity;

    public BlackHoleSeedBreederMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, null, buffer.readBlockPos(), new ItemStackHandler(MACHINE_SLOTS),
                new SimpleContainerData(DATA_COUNT));
    }

    public BlackHoleSeedBreederMenu(int id, Inventory inventory, BlackHoleSeedBreederBlockEntity be) {
        this(id, inventory, be, be.getBlockPos(), be.items(), new ContainerData() {
            @Override
            public int get(int index) {
                int value = switch (index / 2) {
                    case 0 -> be.progress();
                    case 1 -> be.energyStored();
                    case 2 -> be.matterConsumed();
                    case 3 -> be.status();
                    default -> 0;
                };
                return (value >>> ((index & 1) * 16)) & 0xffff;
            }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return DATA_COUNT; }
        });
    }

    private BlackHoleSeedBreederMenu(int id, Inventory inventory, BlackHoleSeedBreederBlockEntity be,
                                    BlockPos pos, ItemStackHandler items, ContainerData data) {
        super(ModMenus.BLACK_HOLE_SEED_BREEDER.get(), id);
        this.blockEntity = be;
        this.machinePos = pos.immutable();
        this.owner = inventory.player;
        this.level = owner.level();
        this.data = data;
        checkContainerDataCount(data, DATA_COUNT);
        addDataSlots(data);
        addSlot(new SlotItemHandler(items, 0, 36, 38) {
            @Override public boolean mayPlace(ItemStack stack) {
                return progress() == 0 && BlackHoleSeedBreederBlockEntity.isLoadedContainer(stack);
            }
            @Override public boolean mayPickup(Player player) { return progress() == 0 && super.mayPickup(player); }
            @Override public int getMaxStackSize() { return 1; }
            @Override public int getMaxStackSize(ItemStack stack) { return 1; }
        });
        addSlot(new SlotItemHandler(items, 1, 72, 38) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.is(BlackHoleSeedBreederBlockEntity.SINGULARITY_MATTER);
            }
        });
        for (int slot = 2; slot < MACHINE_SLOTS; slot++) {
            addSlot(new SlotItemHandler(items, slot, slot == 2 ? 132 : 168, 38) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 26 + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 26 + col * 18, HOTBAR_Y));
        }
    }

    private int value(int index) {
        return (data.get(index * 2) & 0xffff) | ((data.get(index * 2 + 1) & 0xffff) << 16);
    }
    public int progress() { return value(0); }
    public int energy() { return value(1); }
    public int matterConsumed() { return value(2); }
    public int status() { return value(3); }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (stillValid(player)) super.clicked(slot, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            if (getSlot(0).mayPlace(stack)) moved = moveItemStackTo(stack, 0, 1, false);
            else if (getSlot(1).mayPlace(stack)) moved = moveItemStackTo(stack, 1, 2, false);
            if (!moved) {
                if (index < MACHINE_SLOTS + 27) {
                    if (!moveItemStackTo(stack, MACHINE_SLOTS + 27, slots.size(), false)) return ItemStack.EMPTY;
                } else if (!moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 27, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player != owner || player.level() != level || !player.isAlive()
                || player.distanceToSqr(machinePos.getX() + 0.5, machinePos.getY() + 0.5, machinePos.getZ() + 0.5) > 64) {
            return false;
        }
        if (level.isClientSide) return true;
        return blockEntity != null && !blockEntity.isRemoved() && blockEntity.getLevel() == level
                && level.hasChunkAt(machinePos) && level.getBlockEntity(machinePos) == blockEntity;
    }
}
