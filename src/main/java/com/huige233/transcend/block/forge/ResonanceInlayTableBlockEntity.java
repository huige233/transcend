package com.huige233.transcend.block.forge;

import com.huige233.transcend.gear.GearForgeData;
import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.items.forge.ResonanceCrystalItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** 谐振镶嵌台方块实体。 */
public class ResonanceInlayTableBlockEntity extends BlockEntity {

    public static final int ITEM_SLOT = 0;
    public static final int TOTAL_SLOTS = 1;

    private final NonNullList<ItemStack> slots = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    public ResonanceInlayTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESONANCE_INLAY_TABLE_BE.get(), pos, state);
    }

    public ItemStack getItemStack() { return slots.get(ITEM_SLOT); }
    public boolean hasItem() { return !slots.get(ITEM_SLOT).isEmpty(); }

    public int tryInsertItem(Player player, ItemStack held) {
        if (hasItem()) return 1;
        if (held.isEmpty()) return 2;
        if (!GearForgeData.isEligibleForPipeline(held)) return 2;

        if (!GearForgeData.isStageWritten(held, com.huige233.transcend.gear.ForgeStage.CRUCIBLE)) return 2;

        if (GearForgeData.getSockets(held).size() >= GearForgeData.MAX_RESONANCE_SOCKETS) return 2;

        ItemStack one = held.split(1);
        slots.set(ITEM_SLOT, one);
        markUpdated();
        return 0;
    }

    public int tryInsertCrystal(Player player, ItemStack held) {
        if (!hasItem()) return 1;
        if (held.isEmpty() || !(held.getItem() instanceof ResonanceCrystalItem crystal)) return 2;

        ItemStack itemInTable = slots.get(ITEM_SLOT);
        if (GearForgeData.getSockets(itemInTable).size() >= GearForgeData.MAX_RESONANCE_SOCKETS) return 3;

        boolean ok = GearForgeData.addResonanceSocket(itemInTable, crystal.getKind().id, 1);
        if (!ok) return 3;

        held.shrink(1);
        markUpdated();
        return 0;
    }

    public void takeBack(Player player) {
        if (!hasItem() || level == null) return;
        ItemStack stack = slots.get(ITEM_SLOT);
        slots.set(ITEM_SLOT, ItemStack.EMPTY);

        if (!player.getInventory().add(stack)) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, stack);
        }
        markUpdated();
    }

    public void cancelAndDropAll() {
        if (level == null) return;
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            ItemStack s = slots.get(i);
            if (!s.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                        worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, s);
            }
            slots.set(i, ItemStack.EMPTY);
        }
        markUpdated();
    }

    public void dropAllOnRemove() {
        if (level == null || level.isClientSide) return;
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            ItemStack s = slots.get(i);
            if (!s.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                        worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, s);
            }
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        net.minecraft.world.ContainerHelper.saveAllItems(tag, slots, true);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        slots.clear();
        net.minecraft.world.ContainerHelper.loadAllItems(tag, slots);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        net.minecraft.world.ContainerHelper.saveAllItems(tag, slots, true);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(@NotNull Connection connection, @NotNull ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(connection, pkt);
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            slots.clear();
            net.minecraft.world.ContainerHelper.loadAllItems(tag, slots);
        }
    }

    private void markUpdated() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
