package com.huige233.transcend.menu;

import com.huige233.transcend.block.LongStorageBlockEntity;
import com.huige233.transcend.init.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 分段同步储能设备的长整型能量与容量，并管理玩家背包和快捷栏转移。 */
public class LongStorageMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 9;
    private final LongStorageBlockEntity storage;
    private final ContainerData data;

    public LongStorageMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, (LongStorageBlockEntity) null);
        buf.readBlockPos();
    }

    public LongStorageMenu(int id, Inventory inventory, LongStorageBlockEntity storage) {
        super(ModMenus.LONG_STORAGE.get(), id);
        this.storage = storage;
        this.data = storage == null ? new SimpleContainerData(DATA_COUNT) : new ContainerData() {
            public int get(int index) {
                if (index >= 0 && index < 4) return (int) (storage.stored() >>> (index * 16)) & 0xffff;
                if (index >= 4 && index < 8) return (int) (storage.capacity() >>> ((index - 4) * 16)) & 0xffff;
                return index == 8 ? level() : 0;
            }
            public void set(int index, int value) { }
            public int getCount() { return DATA_COUNT; }
            private int level() { long c = storage.capacity(); return c >= 1_000_000_000_000_000L ? 4 : c >= 10_000_000_000L ? 3 : c >= 100_000_000L ? 2 : 1; }
        };
        addDataSlots(data);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
    }
    private long value(int start) {
        long value = 0;
        for (int word = 0; word < 4; word++) value |= (data.get(start + word) & 0xffffL) << (word * 16);
        return value;
    }
    public long stored() { return value(0); }
    public long capacity() { return value(4); }
    public int level() { return data.get(8); }
    @Override public boolean stillValid(Player player) { return storage != null && storage.getLevel() != null && storage.getLevel().getBlockEntity(storage.getBlockPos()) == storage && player.distanceToSqr(storage.getBlockPos().getX()+.5, storage.getBlockPos().getY()+.5, storage.getBlockPos().getZ()+.5) <= 64; }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot sourceSlot = getSlot(index);
        if (!sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = sourceSlot.getItem();
        ItemStack copy = source.copy();
        
        boolean moved = index < 27
                ? moveItemStackTo(source, 27, 36, false)
                : moveItemStackTo(source, 0, 27, false);
        if (!moved) return ItemStack.EMPTY;
        if (source.isEmpty()) sourceSlot.set(ItemStack.EMPTY); else sourceSlot.setChanged();
        return copy;
    }
}
