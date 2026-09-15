package com.huige233.transcend.menu;

import com.huige233.transcend.block.FEGeneratorBlockEntity;
import com.huige233.transcend.init.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/** 管理发电机燃料槽与背包转移，并同步发电模式、储能、燃烧进度和配置产能。 */
public class GeneratorMenu extends AbstractContainerMenu {
    private final FEGeneratorBlockEntity generator;
    private final FEGeneratorBlockEntity.Mode mode;
    private final ContainerData data;
    private final IItemHandler handler;
    private final net.minecraft.core.BlockPos machinePos;
    private static final int DATA_COUNT = 8;

    public GeneratorMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, null, readPosition(buf), FEGeneratorBlockEntity.Mode.FIRE);
    }
    private static net.minecraft.core.BlockPos readPosition(FriendlyByteBuf buf) {
        return buf.readBlockPos();
    }
    public GeneratorMenu(int id, Inventory inv, FEGeneratorBlockEntity generator) {
        this(id, inv, generator, generator.getBlockPos(), generator.getMode());
    }
    private GeneratorMenu(int id, Inventory inv, FEGeneratorBlockEntity generator, net.minecraft.core.BlockPos machinePos, FEGeneratorBlockEntity.Mode mode) {
        super(ModMenus.GENERATOR.get(), id);
        this.generator = generator;
        this.machinePos = machinePos;
        this.mode = mode;
        this.handler = generator != null ? generator.fuelHandler() : new ItemStackHandler(1);
        this.data = generator != null ? new ContainerData() {
            public int get(int i) { return switch (i) { case 0 -> generator.getEnergy() & 0xffff; case 1 -> generator.getEnergy() >>> 16; case 2 -> generator.getBurnTime(); case 3 -> generator.getBurnDuration(); case 4 -> generator.getStatus().ordinal(); case 5 -> generator.getMode().ordinal(); case 6 -> generator.getConfiguredOutput() & 0xffff; case 7 -> generator.getConfiguredOutput() >>> 16; default -> 0; }; }
            public void set(int i, int v) {}
            public int getCount() { return DATA_COUNT; }
        } : new net.minecraft.world.inventory.SimpleContainerData(DATA_COUNT);
        addDataSlots(data);
        addSlot(new SlotItemHandler(handler, 0, 80, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return mode == FEGeneratorBlockEntity.Mode.FIRE && handler.isItemValid(0, stack); }
            @Override public boolean mayPickup(Player player) { return mode == FEGeneratorBlockEntity.Mode.FIRE; }
        });
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
    }
    public FEGeneratorBlockEntity.Mode mode() {
        if (generator == null) {
            int value = data.get(5);
            FEGeneratorBlockEntity.Mode[] values = FEGeneratorBlockEntity.Mode.values();
            if (value >= 0 && value < values.length) return values[value];
        }
        return mode;
    }
    public net.minecraft.core.BlockPos machinePos() { return machinePos; }
    public int energy() { return (data.get(0) & 0xffff) | ((data.get(1) & 0xffff) << 16); }
    public int burnTime() { return data.get(2); }
    public int burnDuration() { return data.get(3); }
    public int configuredOutput() { return (data.get(6) & 0xffff) | ((data.get(7) & 0xffff) << 16); }
    public FEGeneratorBlockEntity.Status status() { int i = data.get(4); return i >= 0 && i < FEGeneratorBlockEntity.Status.values().length ? FEGeneratorBlockEntity.Status.values()[i] : FEGeneratorBlockEntity.Status.IDLE; }
    @Override public boolean stillValid(Player player) { return generator != null && generator.getLevel() != null && generator.getLevel().getBlockEntity(generator.getBlockPos()) == generator && player.distanceToSqr(generator.getBlockPos().getX()+.5, generator.getBlockPos().getY()+.5, generator.getBlockPos().getZ()+.5) <= 64; }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = getSlot(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem(); ItemStack copy = source.copy();
        if (index == 0) { if (!moveItemStackTo(source, 1, slots.size(), true)) return ItemStack.EMPTY; }
        else if (mode == FEGeneratorBlockEntity.Mode.FIRE && handler.isItemValid(0, source) && !moveItemStackTo(source, 0, 1, false)) return ItemStack.EMPTY;
        else if (index < 28) { if (!moveItemStackTo(source, 28, slots.size(), false)) return ItemStack.EMPTY; }
        else if (!moveItemStackTo(source, 1, 28, false)) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }
}
