package com.huige233.transcend.block.forge;

import com.huige233.transcend.gear.GearForgeData;
import com.huige233.transcend.gear.forge.AspectDef;
import com.huige233.transcend.gear.forge.AspectKind;
import com.huige233.transcend.gear.forge.AspectRegistry;
import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.items.forge.CatalystItem;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** 专属特性坩埚方块实体。 */
public class AspectCrucibleBlockEntity extends BlockEntity {

    public static final int ITEM_SLOT = 0;
    public static final int CATALYST_SLOT_START = 1;
    public static final int CATALYST_COUNT = 4;
    public static final int TOTAL_SLOTS = 1 + CATALYST_COUNT;

    private final NonNullList<ItemStack> slots = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    public AspectCrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASPECT_CRUCIBLE_BE.get(), pos, state);
    }

    public ItemStack getItemStack() { return slots.get(ITEM_SLOT); }

    public ItemStack getCatalyst(int idx) {
        if (idx < 0 || idx >= CATALYST_COUNT) return ItemStack.EMPTY;
        return slots.get(CATALYST_SLOT_START + idx);
    }

    public boolean hasItem() { return !slots.get(ITEM_SLOT).isEmpty(); }

    public boolean isReady() {
        if (!hasItem()) return false;
        for (int i = 0; i < CATALYST_COUNT; i++) {
            if (slots.get(CATALYST_SLOT_START + i).isEmpty()) return false;
        }
        return true;
    }

    public int filledCatalystCount() {
        int n = 0;
        for (int i = 0; i < CATALYST_COUNT; i++) {
            if (!slots.get(CATALYST_SLOT_START + i).isEmpty()) n++;
        }
        return n;
    }

    public int tryInsertItem(Player player, ItemStack heldStack) {
        if (hasItem()) return 1;
        if (heldStack.isEmpty()) return 2;
        if (!GearForgeData.isEligibleForPipeline(heldStack)) return 2;
        if (!GearForgeData.canEnterStage(heldStack, com.huige233.transcend.gear.ForgeStage.CRUCIBLE)) return 3;

        ItemStack one = heldStack.split(1);
        slots.set(ITEM_SLOT, one);
        markUpdated();
        return 0;
    }

    public int tryInsertCatalyst(Player player, ItemStack heldStack) {
        if (!hasItem()) return 1;
        if (heldStack.isEmpty() || !(heldStack.getItem() instanceof CatalystItem)) return 3;

        for (int i = 0; i < CATALYST_COUNT; i++) {
            int slotIdx = CATALYST_SLOT_START + i;
            if (slots.get(slotIdx).isEmpty()) {
                ItemStack one = heldStack.split(1);
                slots.set(slotIdx, one);
                markUpdated();
                return 0;
            }
        }
        return 2;
    }

    @Nullable
    public AspectDef tryIgnite(Player player) {
        if (!isReady()) return null;

        AspectKind[] kinds = new AspectKind[CATALYST_COUNT];
        for (int i = 0; i < CATALYST_COUNT; i++) {
            ItemStack s = slots.get(CATALYST_SLOT_START + i);
            if (s.getItem() instanceof CatalystItem c) kinds[i] = c.getKind();
        }
        AspectDef def = AspectRegistry.resolve(kinds);

        ItemStack itemStack = slots.get(ITEM_SLOT);

        String processId = UUID.randomUUID().toString();
        boolean ok = GearForgeData.writeCrucible(itemStack, def.id(), def.offset(), processId);
        if (!ok) {

            return null;
        }

        if (level != null) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, itemStack);
        }

        for (int i = 0; i < TOTAL_SLOTS; i++) slots.set(i, ItemStack.EMPTY);
        markUpdated();
        return def;
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
