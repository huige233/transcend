package com.huige233.transcend.block.mana;

import com.huige233.transcend.block.circle.MagicCircleCoreBlockEntity;
import com.huige233.transcend.block.circle.CircleRuneBlock;
import com.huige233.transcend.items.MagicCrystalItem;
import com.huige233.transcend.mana.IManaHandler;
import com.huige233.transcend.mana.ManaHandlerCapability;
import com.huige233.transcend.mana.SimpleManaStorage;
import com.huige233.transcend.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ManaReservoirBlockEntity extends BlockEntity {

    public static final String BE_ID = "mana_reservoir_be";

    private final SimpleManaStorage manaStorage;
    private final int capacity;
    private final int transferRate;

    private int tickCounter = 0;

    private final LazyOptional<IManaHandler> manaCapability;

    public ManaReservoirBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_RESERVOIR_BE.get(), pos, state);

        int cap = 2048;
        int rate = 16;
        if (state.getBlock() instanceof ManaReservoirBlock rb) {
            cap = rb.getCapacity();
            rate = rb.getTransferRate();
        }
        this.capacity = cap;
        this.transferRate = rate;
        this.manaStorage = new SimpleManaStorage(capacity, transferRate, transferRate);
        this.manaCapability = LazyOptional.of(() -> this.manaStorage);
    }

    public SimpleManaStorage getManaStorage() {
        return manaStorage;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getTransferRate() {
        return transferRate;
    }

    public InteractionResult onUse(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !(stack.getItem() instanceof MagicCrystalItem crystal)) {
            return InteractionResult.PASS;
        }

        int value = crystal.getCrystalValue();
        int accepted = manaStorage.receiveMana(value, false);
        if (accepted <= 0) {
            return InteractionResult.PASS;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        setChanged();
        return InteractionResult.sidedSuccess(level == null || level.isClientSide);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaReservoirBlockEntity be) {
        be.tickCounter++;
        if (be.tickCounter < 20) {
            return;
        }
        be.tickCounter = 0;

        boolean changed = false;

        if (be.manaStorage.getManaStored() > 0) {
            int perPush = Math.max(1, be.transferRate / 3);
            for (Direction dir : Direction.values()) {
                if (be.manaStorage.getManaStored() <= 0) break;
                BlockPos neighborPos = pos.relative(dir);
                BlockEntity neighbor = level.getBlockEntity(neighborPos);
                if (neighbor instanceof MagicCircleCoreBlockEntity core) {
                    int available = be.manaStorage.extractMana(perPush, true);
                    if (available <= 0) continue;
                    int inserted = core.insertMana(available);
                    if (inserted > 0) {
                        be.manaStorage.extractMana(inserted, false);
                        changed = true;
                    }
                }
            }
        }

        if (be.manaStorage.getManaStored() < be.capacity) {
            int perPull = Math.max(1, be.transferRate / 4);
            int space = be.capacity - be.manaStorage.getManaStored();
            int pullAmount = Math.min(perPull, space);

            for (Direction dir : Direction.values()) {
                if (pullAmount <= 0) break;
                BlockPos runePos = pos.relative(dir);

                if (!(level.getBlockState(runePos).getBlock() instanceof CircleRuneBlock)) continue;

                BlockPos scanPos = runePos;
                for (int depth = 0; depth < 3; depth++) {
                    scanPos = scanPos.relative(dir);
                    if (level.getBlockState(scanPos).getBlock() instanceof CircleRuneBlock) {
                        continue;
                    }

                    BlockEntity endBe = level.getBlockEntity(scanPos);
                    if (endBe instanceof MagicCircleCoreBlockEntity core) {
                        if (core.getStoredMana() > 0) {
                            int toExtract = Math.min(pullAmount, core.getStoredMana());
                            int accepted = be.manaStorage.receiveMana(toExtract, false);
                            if (accepted > 0) {
                                core.extractMana(accepted);
                                pullAmount -= accepted;
                                changed = true;
                            }
                        }
                    }
                    break;
                }
            }
        }

        if (changed) {
            be.setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("ManaStorage", manaStorage.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("ManaStorage")) {
            manaStorage.deserializeNBT(tag.getCompound("ManaStorage"));
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ManaHandlerCapability.MANA_HANDLER) {
            return manaCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        manaCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
    }
}
