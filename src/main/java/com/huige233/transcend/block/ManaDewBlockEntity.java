package com.huige233.transcend.block;

import com.huige233.transcend.block.data.DewProductionConfig;
import com.huige233.transcend.block.data.DewProductionRegistry;
import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.mana.IManaHandler;
import com.huige233.transcend.mana.ManaHandlerCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ManaDewBlockEntity extends BlockEntity {

    private int tickCounter = 0;

    public ManaDewBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_DEW_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaDewBlockEntity be) {
        DewProductionConfig cfg = DewProductionRegistry.getInstance().getDefault();

        be.tickCounter++;
        if (be.tickCounter < cfg.produceInterval()) return;
        be.tickCounter = 0;

        if (cfg.produceAmount() <= 0) return;

        IManaHandler target = findNearestSink(level, pos, cfg.injectRadius());
        if (target == null) return;

        int injected = target.receiveMana(cfg.produceAmount(), false);
        if (injected > 0 && level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LAVA,
                    pos.getX() + 0.5, pos.getY() + 0.35, pos.getZ() + 0.5,
                    1, 0.05, 0.02, 0.05, 0.0);
            sl.sendParticles(ParticleTypes.FLAME,
                    pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
                    2, 0.15, 0.05, 0.15, 0.005);
        }
    }

    private static IManaHandler findNearestSink(Level level, BlockPos origin, int radius) {
        IManaHandler best = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    m.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockEntity nb = level.getBlockEntity(m);
                    if (nb == null) continue;
                    IManaHandler cap = nb.getCapability(ManaHandlerCapability.MANA_HANDLER).orElse(null);
                    if (cap == null || !cap.canReceive()) continue;
                    if (cap.getManaStored() >= cap.getMaxManaStored()) continue;
                    double d = origin.distSqr(m);
                    if (d < bestDist) {
                        bestDist = d;
                        best = cap;
                    }
                }
            }
        }
        return best;
    }
}
