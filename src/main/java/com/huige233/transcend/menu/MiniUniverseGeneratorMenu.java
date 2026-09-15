package com.huige233.transcend.menu;

import com.huige233.transcend.block.MiniUniverseGeneratorBlockEntity;
import com.huige233.transcend.init.ModMenus;
import com.huige233.transcend.util.MachineDataValues;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import java.math.BigInteger;

/** 管理微型宇宙发电机槽位并还原同步的能量、发电阶段、剩余储量与超频状态。 */
public class MiniUniverseGeneratorMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 2;
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int OUTPUT = 2;
    private static final int PHASE = 4;
    private static final int REMAINING = 5;
    private static final int CHARGED = REMAINING + MachineDataValues.BIG_WORDS;
    private static final int STATUS = CHARGED + MachineDataValues.BIG_WORDS;
    static final int OVERCLOCK = STATUS + 1;
    static final int DATA_COUNT = OVERCLOCK + 1;
    public static final int WIDTH = 240, HEIGHT = 232;
    public static final int SLOT_X = 36, SLOT_Y = 50;
    public static final int INVENTORY_X = 39, INVENTORY_Y = 150, HOTBAR_Y = 208;
    private final ContainerData data;
    private final BlockPos machinePos;
    private final MiniUniverseGeneratorBlockEntity be;

    public MiniUniverseGeneratorMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, null, new ItemStackHandler(MACHINE_SLOTS), new SimpleContainerData(DATA_COUNT), buf.readBlockPos());
    }

    public MiniUniverseGeneratorMenu(int id, Inventory inv, MiniUniverseGeneratorBlockEntity be) {
        this(id, inv, be, be.itemHandler(), new MiniUniverseGeneratorData(be), be.getBlockPos());
    }

    private MiniUniverseGeneratorMenu(int id, Inventory inv, MiniUniverseGeneratorBlockEntity be,
                                      IItemHandler machineItems, ContainerData data, BlockPos pos) {
        super(ModMenus.MINI_UNIVERSE_GENERATOR.get(), id);
        this.be = be;
        this.data = data;
        this.machinePos = pos.immutable();
        addDataSlots(data);
        
        addSlot(new SlotItemHandler(machineItems, INPUT_SLOT, SLOT_X, SLOT_Y));
        addSlot(new SlotItemHandler(machineItems, OUTPUT_SLOT, SLOT_X + 108, SLOT_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, INVENTORY_X + c * 18, INVENTORY_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, INVENTORY_X + c * 18, HOTBAR_Y));
    }

    
    
    public int inputEnergy() { return MachineDataValues.readInt(data::get, 0); }
    public int energy() { return MachineDataValues.readInt(data::get, 2); }
    public int capacity() { return MiniUniverseGeneratorBlockEntity.BUFFER; }
    public boolean active() { return data.get(PHASE) == 2; }
    public String phase() { return switch (data.get(PHASE)) { case 1 -> "CHARGING"; case 2 -> "GENERATING"; default -> "IDLE"; }; }
    public BigInteger charged() { return MachineDataValues.readBig(data::get, CHARGED); }
    public BigInteger remaining() { return MachineDataValues.readBig(data::get, REMAINING); }
    public int statusId() { int value = data.get(STATUS); return value >= 0 && value < MiniUniverseGeneratorBlockEntity.Status.values().length ? value : 0; }
    public String status() { return MiniUniverseGeneratorBlockEntity.Status.values()[statusId()].name(); }
    public BlockPos machinePos() { return machinePos; }
    public boolean overclocked() { return data.get(OVERCLOCK) != 0; }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(); ItemStack original = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    @Override public boolean stillValid(Player player) {
        return player.isAlive() && player.distanceToSqr(machinePos.getX() + .5, machinePos.getY() + .5, machinePos.getZ() + .5) <= 64
                && (player.level().isClientSide || (be != null && !be.isRemoved() && player.level().getBlockEntity(machinePos) == be));
    }
}
