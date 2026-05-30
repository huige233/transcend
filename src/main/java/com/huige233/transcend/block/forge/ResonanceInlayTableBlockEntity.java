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

/**
 * R83: 共鸣镶嵌台方块实体（state machine）。
 *
 * <h2>2 槽位</h2>
 * <ul>
 *   <li>slot 0：装备槽（仅接受 CRUCIBLE 已完成 + sockets &lt; 4 的装备）</li>
 *   <li>（共鸣水晶不存储 — 每投入 1 颗立即写入 socket 并消耗）</li>
 * </ul>
 *
 * <h2>状态</h2>
 * <pre>
 * EMPTY            ─ rclick 装备 → ITEM_LOADED
 * ITEM_LOADED      ─ rclick 共鸣水晶 → 立即 GearForgeData.addResonanceSocket(stack, kind.id, 1)
 *                                     消耗 1 水晶；若 sockets == 4 → 反馈"已满"
 * ITEM_LOADED      ─ rclick 空手 → 退还装备 → EMPTY
 * 任意状态         ─ shift+rclick 空手 → 退还全部 → EMPTY
 * </pre>
 *
 * <h2>R80 不可逆门</h2>
 * 装备必须已写入 CRUCIBLE（E 必先）；socket 列表只能增不能减。
 */
public class ResonanceInlayTableBlockEntity extends BlockEntity {

    public static final int ITEM_SLOT = 0;
    public static final int TOTAL_SLOTS = 1;

    private final NonNullList<ItemStack> slots = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    public ResonanceInlayTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESONANCE_INLAY_TABLE_BE.get(), pos, state);
    }

    public ItemStack getItemStack() { return slots.get(ITEM_SLOT); }
    public boolean hasItem() { return !slots.get(ITEM_SLOT).isEmpty(); }

    /**
     * 玩家把装备塞进镶嵌台。
     * @return 0=成功；1=已有装备；2=装备不合格 / 未过 E 坩埚 / 已满 socket
     */
    public int tryInsertItem(Player player, ItemStack held) {
        if (hasItem()) return 1;
        if (held.isEmpty()) return 2;
        if (!GearForgeData.isEligibleForPipeline(held)) return 2;
        // E 必先门：未过 CRUCIBLE 不可镶嵌
        if (!GearForgeData.isStageWritten(held, com.huige233.transcend.gear.ForgeStage.CRUCIBLE)) return 2;
        // 已满 socket 也不再接受（避免误操作）
        if (GearForgeData.getSockets(held).size() >= GearForgeData.MAX_RESONANCE_SOCKETS) return 2;

        ItemStack one = held.split(1);
        slots.set(ITEM_SLOT, one);
        markUpdated();
        return 0;
    }

    /**
     * 玩家把共鸣水晶投入。立即在装备 NBT 写入一个 socket。
     * @return 0=成功；1=没有装备；2=不是共鸣水晶；3=socket 已满（4/4）
     */
    public int tryInsertCrystal(Player player, ItemStack held) {
        if (!hasItem()) return 1;
        if (held.isEmpty() || !(held.getItem() instanceof ResonanceCrystalItem crystal)) return 2;

        ItemStack itemInTable = slots.get(ITEM_SLOT);
        if (GearForgeData.getSockets(itemInTable).size() >= GearForgeData.MAX_RESONANCE_SOCKETS) return 3;

        boolean ok = GearForgeData.addResonanceSocket(itemInTable, crystal.getKind().id, 1);
        if (!ok) return 3;
        // 消耗 1 颗水晶
        held.shrink(1);
        markUpdated();
        return 0;
    }

    /** 玩家空手取回装备。 */
    public void takeBack(Player player) {
        if (!hasItem() || level == null) return;
        ItemStack stack = slots.get(ITEM_SLOT);
        slots.set(ITEM_SLOT, ItemStack.EMPTY);
        // 优先放进玩家背包，放不下再掉地上
        if (!player.getInventory().add(stack)) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, stack);
        }
        markUpdated();
    }

    /** 取消：弹出所有内容到方块上方。 */
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

    // ── NBT ─────────────────────────────────────────────────────────

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
