package com.huige233.transcend.block;

import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.mana.IManaHandler;
import com.huige233.transcend.mana.ManaHandlerCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 魔力炎露的方块实体：每秒从下方岩浆抽取一份热能并转化为 1 mana，
 * 然后注入 {@link #INJECT_RADIUS} 球范围内最近的可接收 mana 容器。
 */
public class ManaDewBlockEntity extends BlockEntity {

    private static final int PRODUCE_INTERVAL = 20;
    private static final int PRODUCE_AMOUNT = 1;
    private static final int INJECT_RADIUS = 4;

    private int tickCounter = 0;

    public ManaDewBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_DEW_BE.get(), pos, state);
    }

    /**
     * 服务端 tick：累计到 {@link #PRODUCE_INTERVAL} 后产 mana 一次。
     * 找不到接收方时静默跳过；接收成功额外触发岩浆+火焰粒子。
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaDewBlockEntity be) {
        be.tickCounter++;
        if (be.tickCounter < PRODUCE_INTERVAL) return;
        be.tickCounter = 0;

        IManaHandler target = findNearestSink(level, pos);
        if (target == null) return;

        int injected = target.receiveMana(PRODUCE_AMOUNT, false);
        if (injected > 0 && level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LAVA,
                    pos.getX() + 0.5, pos.getY() + 0.35, pos.getZ() + 0.5,
                    1, 0.05, 0.02, 0.05, 0.0);
            sl.sendParticles(ParticleTypes.FLAME,
                    pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
                    2, 0.15, 0.05, 0.15, 0.005);
        }
    }

    /** 球形扫描 {@link #INJECT_RADIUS} 块内的 mana 容器，返回距离最近且仍可接收的那一个。 */
    private static IManaHandler findNearestSink(Level level, BlockPos origin) {
        IManaHandler best = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -INJECT_RADIUS; dx <= INJECT_RADIUS; dx++) {
            for (int dy = -INJECT_RADIUS; dy <= INJECT_RADIUS; dy++) {
                for (int dz = -INJECT_RADIUS; dz <= INJECT_RADIUS; dz++) {
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
