package com.huige233.transcend.block.mana;

import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.mana.IManaHandler;
import com.huige233.transcend.mana.ManaHandlerCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class LeylineStabilizerBlockEntity extends BlockEntity {

    public static final String BE_ID = "leyline_stabilizer_be";

    private static final int UPKEEP_INTERVAL = 1200;

    private static final int UPKEEP_COST = 1;

    private int tickCounter = 0;
    private boolean fueled = true;

    public LeylineStabilizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LEYLINE_STABILIZER_BE.get(), pos, state);
    }

    public boolean isFueled() { return fueled; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LeylineStabilizerBlockEntity be) {
        be.tickCounter++;
        if (be.tickCounter < UPKEEP_INTERVAL) return;
        be.tickCounter = 0;

        boolean paid = false;
        for (Direction dir : Direction.values()) {
            BlockEntity nb = level.getBlockEntity(pos.relative(dir));
            if (nb == null || nb == be) continue;
            IManaHandler h = nb.getCapability(
                    ManaHandlerCapability.MANA_HANDLER, dir.getOpposite()).resolve().orElse(null);
            if (h == null || !h.canExtract()) continue;
            int got = h.extractMana(UPKEEP_COST, false);
            if (got > 0) {
                paid = true;
                break;
            }
        }

        boolean wasFueled = be.fueled;
        be.fueled = paid;
        if (be.fueled != wasFueled) be.setChanged();

        if (paid && level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                    8, 0.4, 0.1, 0.4, 0.0);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("Fueled", fueled);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Fueled")) fueled = tag.getBoolean("Fueled");
    }
}
