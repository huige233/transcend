package com.huige233.transcend.menu;

import com.huige233.transcend.block.ResearchPluginBlock;
import com.huige233.transcend.block.ResearchStationBlockEntity;
import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.init.ModMenus;
import com.huige233.transcend.network.S2CResearchStationPacket;
import com.huige233.transcend.tech.research.ResearchCapabilities;
import com.huige233.transcend.tech.research.ResearchNode;
import com.huige233.transcend.tech.research.ResearchRegistry;
import com.huige233.transcend.tech.research.ResearchService;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.network.NetworkDirection;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** 管理研究站材料与插件槽位，发送研究快照并据此判断客户端研究操作的可用性。 */
public final class ResearchStationMenu extends AbstractContainerMenu {
    private static final int STATION_SLOTS = ResearchStationBlockEntity.INPUT_SLOTS + ResearchStationBlockEntity.PLUGIN_SLOTS;
    
    public static final int INPUT_X = 18, INPUT_Y = 102, INPUT_SPACING = 24;
    public static final int PLUGIN_X = 94, PLUGIN_Y = 142;
    public static final int INVENTORY_X = 16, INVENTORY_Y = 184, HOTBAR_Y = 242;
    public static final int SLOT_SPACING = 18;
    private final ResearchStationBlockEntity station;
    private final Player player;
    private final ItemStackHandler inputs;
    private final List<ResearchNode> nodes = List.copyOf(ResearchRegistry.all());
    private CompoundState state = new CompoundState();
    private boolean receivedSnapshot;
    private int syncTicks;

    public ResearchStationMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, inventory.player.level().getBlockEntity(buffer.readBlockPos()) instanceof ResearchStationBlockEntity s ? s : null);
    }

    public ResearchStationMenu(int id, Inventory inventory, ResearchStationBlockEntity station) {
        super(ModMenus.RESEARCH_STATION.get(), id);
        this.station = station;
        this.player = inventory.player;
        inputs = station != null ? station.inputs() : new ItemStackHandler(ResearchStationBlockEntity.INPUT_SLOTS) {
            @Override public int getSlotLimit(int slot) { return slot == 0 ? 1 : 64; }
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return slot == 0 ? stack.is(ModItems.blank_research_component.get()) : stack.is(ModItems.research_assist_unit.get());
            }
        };
        ItemStackHandler plugins = station != null ? station.plugins() : new ItemStackHandler(ResearchStationBlockEntity.PLUGIN_SLOTS) {
            @Override public int getSlotLimit(int slot) { return 1; }
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof ResearchPluginBlock;
            }
        };
        for (int i = 0; i < ResearchStationBlockEntity.INPUT_SLOTS; i++)
            addSlot(new SlotItemHandler(inputs, i, INPUT_X + i * INPUT_SPACING, INPUT_Y));
        for (int i = 0; i < ResearchStationBlockEntity.PLUGIN_SLOTS; i++)
            addSlot(new SlotItemHandler(plugins, i, PLUGIN_X + i * SLOT_SPACING, PLUGIN_Y));
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * SLOT_SPACING, INVENTORY_Y + row * SLOT_SPACING));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, INVENTORY_X + col * SLOT_SPACING, HOTBAR_Y));
    }

    public ResearchStationBlockEntity station() { return station; }
    public List<ResearchNode> nodes() { return nodes; }
    public CompoundState clientState() { return state; }
    public void setClientState(CompoundState state) {
        this.state = state;
        receivedSnapshot = true;
    }
    public int pageCount() { return Math.max(1, (nodes.size() + 8) / 9); }

    private Set<String> completed() {
        Set<String> done = new HashSet<>();
        var list = state.progress().getList("Completed", 8);
        for (int i = 0; i < list.size(); i++) done.add(list.getString(i));
        return done;
    }

    public boolean isAvailable(String id) {
        String active = state.progress().getString("Active");
        return ResearchService.canStart(completed(), id, active.isEmpty() ? null : active);
    }

    public boolean canStart(String id) {
        if (!receivedSnapshot || station == null || state.researcher() != null) return false;
        String active = state.progress().getString("Active");
        
        if (!active.isEmpty()) return active.equals(id);
        try {
            return new BigInteger(state.points()).signum() > 0 && isAvailable(id);
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public boolean canPause() {
        return receivedSnapshot && station != null && player.getUUID().equals(state.researcher())
                && !state.progress().getString("Active").isEmpty();
    }

    public boolean canPrepare() {
        return receivedSnapshot && station != null
                && (state.researcher() == null || player.getUUID().equals(state.researcher()))
                && state.energy() >= ResearchStationBlockEntity.RF_PER_RESEARCH_POINT * 10L
                && inputs.getStackInSlot(0).is(ModItems.blank_research_component.get())
                && inputs.getStackInSlot(1).is(ModItems.research_assist_unit.get());
    }

    public Component status(String id) {
        if (id.equals(state.progress().getString("Active")))
            return Component.translatable(state.researcher() == null ? "gui.transcend.research.paused" : "gui.transcend.research.in_progress");
        if (completed().contains(id)) return Component.translatable("gui.transcend.research.completed");
        return Component.translatable(isAvailable(id) ? "gui.transcend.research.available" : "gui.transcend.research.locked");
    }

    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (player instanceof ServerPlayer serverPlayer && syncTicks++ % 10 == 0) sendSnapshot(serverPlayer);
    }

    
    public void refreshSnapshot() {
        broadcastChanges();
        if (player instanceof ServerPlayer serverPlayer) sendSnapshot(serverPlayer);
    }

    private void sendSnapshot(ServerPlayer player) {
        if (player != this.player || !stillValid(player)) return;
        CompoundTag tag = new CompoundTag();
        player.getCapability(ResearchCapabilities.PROGRESS).ifPresent(progress -> progress.save(tag));
        NetworkHandler.CHANNEL.sendTo(new S2CResearchStationPacket(containerId, station.getBlockPos(), tag,
                station.energyStored(), station.inputRate(), station.researchPoints().toString(),
                station.conversionRemainder(), station.researcher()), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot source = slots.get(index);
        if (!source.hasItem() || !source.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = source.getItem();
        ItemStack original = stack.copy();
        if (!moveItemStackTo(stack, index < STATION_SLOTS ? STATION_SLOTS : 0,
                index < STATION_SLOTS ? slots.size() : STATION_SLOTS, index < STATION_SLOTS)) return ItemStack.EMPTY;
        if (stack.isEmpty()) source.set(ItemStack.EMPTY);
        else source.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        source.onTake(player, stack);
        
        if (!player.level().isClientSide) station.setChanged();
        return original;
    }

    @Override public boolean stillValid(Player player) {
        if (player != this.player || station == null || station.isRemoved() || station.getLevel() != player.level()) return false;
        BlockPos pos = station.getBlockPos();
        return player.level().hasChunkAt(pos) && player.level().getBlockEntity(pos) == station
                && player.level().getBlockState(pos).is(station.getBlockState().getBlock())
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64;
    }

    /** 承载研究站菜单同步所需的研究进度、能量、研究点、研究者和方块位置快照。 */
    public record CompoundState(CompoundTag progress, long energy, long rate, String points, long remainder, UUID researcher, BlockPos pos) {
        public CompoundState() { this(new CompoundTag(), 0, 0, "0", 0, null, BlockPos.ZERO); }
    }
}
