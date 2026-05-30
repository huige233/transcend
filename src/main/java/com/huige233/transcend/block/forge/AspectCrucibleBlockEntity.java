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

/**
 * R82: 坩埚预炼方块实体（state machine + NBT + 24 aspect 解析）。
 *
 * <h2>5 槽 + 状态机</h2>
 * <ul>
 *   <li>slot 0：装备槽（仅接受 {@code GearForgeData.isEligibleForPipeline} 装备且未写入 CRUCIBLE）</li>
 *   <li>slot 1..4：4 个 catalyst 槽（仅接受 {@link CatalystItem}，每槽 1 个）</li>
 * </ul>
 *
 * <h2>状态</h2>
 * <pre>
 * EMPTY                ─ rclick 装备 → ITEM_LOADED
 * ITEM_LOADED          ─ rclick catalyst → CATALYST_FILLING（slot 1..4 任填一个）
 * CATALYST_FILLING     ─ 4 槽全满 → READY
 * READY                ─ rclick 空手 → IGNITE → 写入 GearForgeData → 弹出装备 → EMPTY
 * 任意状态 ─ shift+rclick 空手 → 退还所有物品 → EMPTY
 * </pre>
 *
 * <h2>不可逆门</h2>
 * 仅在 {@link GearForgeData#canEnterStage}(stack, CRUCIBLE) 通过时才接受装备。
 * 已写入 CRUCIBLE 的装备会被拒绝（保留 R80 的不可逆约束）。
 */
public class AspectCrucibleBlockEntity extends BlockEntity {

    public static final int ITEM_SLOT = 0;
    public static final int CATALYST_SLOT_START = 1;
    public static final int CATALYST_COUNT = 4;
    public static final int TOTAL_SLOTS = 1 + CATALYST_COUNT; // 5

    /** slot 0 = item, slot 1..4 = catalysts。 */
    private final NonNullList<ItemStack> slots = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    public AspectCrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASPECT_CRUCIBLE_BE.get(), pos, state);
    }

    // ─── 状态机 ─────────────────────────────────────────────────────

    public ItemStack getItemStack() { return slots.get(ITEM_SLOT); }

    public ItemStack getCatalyst(int idx) {
        if (idx < 0 || idx >= CATALYST_COUNT) return ItemStack.EMPTY;
        return slots.get(CATALYST_SLOT_START + idx);
    }

    public boolean hasItem() { return !slots.get(ITEM_SLOT).isEmpty(); }

    /** 4 槽全部装满 catalyst 即视为 READY。 */
    public boolean isReady() {
        if (!hasItem()) return false;
        for (int i = 0; i < CATALYST_COUNT; i++) {
            if (slots.get(CATALYST_SLOT_START + i).isEmpty()) return false;
        }
        return true;
    }

    /** 当前 catalyst 已填数（用于 tooltip / particle）。 */
    public int filledCatalystCount() {
        int n = 0;
        for (int i = 0; i < CATALYST_COUNT; i++) {
            if (!slots.get(CATALYST_SLOT_START + i).isEmpty()) n++;
        }
        return n;
    }

    // ─── 写入路径 ───────────────────────────────────────────────────

    /**
     * 玩家把装备塞进坩埚。
     * @return 0=成功；1=已有装备；2=装备不合格；3=装备已写入 CRUCIBLE
     */
    public int tryInsertItem(Player player, ItemStack heldStack) {
        if (hasItem()) return 1;
        if (heldStack.isEmpty()) return 2;
        if (!GearForgeData.isEligibleForPipeline(heldStack)) return 2;
        if (!GearForgeData.canEnterStage(heldStack, com.huige233.transcend.gear.ForgeStage.CRUCIBLE)) return 3;

        // 取走 1 个（玩家手中只能 stacksTo(1)，但保险起见还是 split 1）
        ItemStack one = heldStack.split(1);
        slots.set(ITEM_SLOT, one);
        markUpdated();
        return 0;
    }

    /**
     * 玩家把 catalyst 塞进任意一个空 catalyst 槽。
     * @return 0=成功；1=没有装备先入坩埚；2=catalyst 槽全满；3=不是 catalyst
     */
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

    /**
     * 引燃：仅在 READY 时调用。解析 4 catalyst → AspectDef，写 GearForgeData，弹出装备，清空槽位。
     *
     * @return 解析得到的 AspectDef（含 INDETERMINATE 回退）；若 !isReady → null
     */
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
        // process_id：唯一标识本次坩埚行为，用于 R86 战斗 hook 抗重复
        String processId = UUID.randomUUID().toString();
        boolean ok = GearForgeData.writeCrucible(itemStack, def.id(), def.offset(), processId);
        if (!ok) {
            // 不应发生（tryInsertItem 已校验），但保险起见退还
            return null;
        }

        // 弹出装备
        if (level != null) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, itemStack);
        }
        // 清空所有槽（catalyst 被消耗）
        for (int i = 0; i < TOTAL_SLOTS; i++) slots.set(i, ItemStack.EMPTY);
        markUpdated();
        return def;
    }

    /**
     * 取消：弹出装备 + 全部 catalyst 到坩埚上方。
     */
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

    /** 当方块被破坏时调用，掉落所有内容。 */
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

    // ─── NBT ────────────────────────────────────────────────────────

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

    // ─── 同步给客户端（用于未来的渲染器读取槽位状态）────────────────

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
