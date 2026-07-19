package com.huige233.transcend.block.forge;

import com.huige233.transcend.gear.GearForgeData;
import com.huige233.transcend.gear.forge.BlessingDef;
import com.huige233.transcend.gear.forge.BlessingRegistry;
import com.huige233.transcend.gear.forge.CelestialKind;
import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.items.forge.CelestialFragmentItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CelestialAltarBlockEntity extends BlockEntity {

    public static final int ITEM_SLOT = 0;
    public static final int FRAGMENT_SLOT_START = 1;
    public static final int FRAGMENT_COUNT = 4;
    public static final int TOTAL_SLOTS = 1 + FRAGMENT_COUNT;

    private final NonNullList<ItemStack> slots = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    public CelestialAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CELESTIAL_ALTAR_BE.get(), pos, state);
    }

    public ItemStack getItemStack() { return slots.get(ITEM_SLOT); }
    public ItemStack getFragment(int i) {
        if (i < 0 || i >= FRAGMENT_COUNT) return ItemStack.EMPTY;
        return slots.get(FRAGMENT_SLOT_START + i);
    }
    public boolean hasItem() { return !slots.get(ITEM_SLOT).isEmpty(); }

    public boolean isReady() {
        if (!hasItem()) return false;
        for (int i = 0; i < FRAGMENT_COUNT; i++) {
            if (slots.get(FRAGMENT_SLOT_START + i).isEmpty()) return false;
        }
        return true;
    }

    public int filledFragmentCount() {
        int n = 0;
        for (int i = 0; i < FRAGMENT_COUNT; i++) {
            if (!slots.get(FRAGMENT_SLOT_START + i).isEmpty()) n++;
        }
        return n;
    }

    public int tryInsertItem(Player player, ItemStack held) {
        if (hasItem()) return 1;
        if (held.isEmpty()) return 2;
        if (!GearForgeData.isEligibleForPipeline(held)) return 2;
        if (!GearForgeData.canEnterStage(held, com.huige233.transcend.gear.ForgeStage.CELESTIAL)) return 2;

        ItemStack one = held.split(1);
        slots.set(ITEM_SLOT, one);
        markUpdated();
        return 0;
    }

    public int tryInsertFragment(Player player, ItemStack held) {
        if (!hasItem()) return 1;
        if (held.isEmpty() || !(held.getItem() instanceof CelestialFragmentItem)) return 3;
        for (int i = 0; i < FRAGMENT_COUNT; i++) {
            int slotIdx = FRAGMENT_SLOT_START + i;
            if (slots.get(slotIdx).isEmpty()) {
                ItemStack one = held.split(1);
                slots.set(slotIdx, one);
                markUpdated();
                return 0;
            }
        }
        return 2;
    }

    @Nullable
    public BlessingDef tryCoronate(Player player) {
        if (!isReady() || level == null) return null;

        CelestialKind[] kinds = new CelestialKind[FRAGMENT_COUNT];
        for (int i = 0; i < FRAGMENT_COUNT; i++) {
            ItemStack s = slots.get(FRAGMENT_SLOT_START + i);
            if (s.getItem() instanceof CelestialFragmentItem f) kinds[i] = f.getKind();
        }
        BlessingDef def = BlessingRegistry.resolve(kinds);

        ItemStack gear = slots.get(ITEM_SLOT);
        int moonPhase = level.getMoonPhase();
        String biomeClass = currentBiomeId(level, getBlockPos());

        boolean ok = GearForgeData.writeCelestial(gear, def.id(), moonPhase, biomeClass);
        if (!ok) return null;

        Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, gear);
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

    private static String currentBiomeId(Level level, BlockPos pos) {
        Biome biome = level.getBiome(pos).value();
        ResourceLocation rl = level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                .getKey(biome);
        return rl == null ? "minecraft:plains" : rl.toString();
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
