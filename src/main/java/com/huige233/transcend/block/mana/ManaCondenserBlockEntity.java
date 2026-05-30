package com.huige233.transcend.block.mana;

import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.mana.IManaHandler;
import com.huige233.transcend.mana.ManaHandlerCapability;
import com.huige233.transcend.mana.SimpleManaStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mana Condenser 方块实体：mana_well 直连输出适配器。
 *
 * <p>设计稿 D3 兑现：放在 mana_well 旁边时，well 不再产水晶而是把 item mana
 * 直接注入此 BE 的内部缓冲，再由相邻 IManaHandler（reservoir / spreader / dock）拉走。
 *
 * <p>Capability 暴露：仅 receive（{@code canExtract = false} 是不行的，
 * 因为 spreader 拉取依赖 canExtract — 所以这里也允许 extract，
 * 让上游网络节点能像普通缓冲一样从 condenser 抽走 mana）。
 *
 * <p>tick 行为：每 5 tick 主动向相邻 IManaHandler 推送，加速冷凝器对网络的填充。
 */
public class ManaCondenserBlockEntity extends BlockEntity {

    public static final String BE_ID = "mana_condenser_be";

    /** 内部缓冲（小：仅作 well → 网络的中转）。 */
    public static final int CAPACITY = 256;
    /** 单次推送上限。 */
    public static final int PER_PUSH = 8;
    /** 主动推送的 tick 间隔。 */
    private static final int PUSH_INTERVAL = 5;

    private final SimpleManaStorage buffer;
    private final LazyOptional<IManaHandler> manaCap;
    private int tickCounter = 0;

    public ManaCondenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_CONDENSER_BE.get(), pos, state);
        this.buffer = new SimpleManaStorage(CAPACITY, PER_PUSH, PER_PUSH);
        this.manaCap = LazyOptional.of(() -> this.buffer);
    }

    public int getStoredMana() { return buffer.getManaStored(); }

    public int getCapacity() { return buffer.getMaxManaStored(); }

    /**
     * Mana Well 的入口：直接把 item mana 写入缓冲。
     * @return 实际接受量
     */
    public int receiveFromWell(int amount) {
        int accepted = buffer.receiveMana(amount, false);
        if (accepted > 0) setChanged();
        return accepted;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaCondenserBlockEntity be) {
        be.tickCounter++;
        if (be.tickCounter < PUSH_INTERVAL) return;
        be.tickCounter = 0;
        if (be.buffer.getManaStored() <= 0) return;

        // 主动推送到相邻可接收的 IManaHandler（不含 mana well 本身）
        boolean changed = false;
        int budget = PER_PUSH;
        for (Direction dir : Direction.values()) {
            if (budget <= 0 || be.buffer.getManaStored() <= 0) break;
            BlockEntity nb = level.getBlockEntity(pos.relative(dir));
            if (nb == null || nb == be) continue;
            // 不向 well 回灌
            if (nb instanceof com.huige233.transcend.block.ManaWellBlockEntity) continue;
            IManaHandler h = nb.getCapability(
                    ManaHandlerCapability.MANA_HANDLER, dir.getOpposite()).resolve().orElse(null);
            if (h == null || !h.canReceive()) continue;
            int avail = be.buffer.extractMana(budget, true);
            if (avail <= 0) continue;
            int accepted = h.receiveMana(avail, false);
            if (accepted > 0) {
                be.buffer.extractMana(accepted, false);
                budget -= accepted;
                changed = true;
            }
        }
        if (changed) be.setChanged();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Buffer", buffer.serializeNBT());
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Buffer")) buffer.deserializeNBT(tag.getCompound("Buffer"));
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ManaHandlerCapability.MANA_HANDLER) {
            return manaCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        manaCap.invalidate();
    }
}
