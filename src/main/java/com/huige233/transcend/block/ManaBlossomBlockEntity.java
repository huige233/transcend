package com.huige233.transcend.block;

import com.huige233.transcend.block.data.BlossomTransform;
import com.huige233.transcend.block.data.BlossomTransformRegistry;
import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.mana.IManaHandler;
import com.huige233.transcend.mana.ManaHandlerCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * 魔力花苞的方块实体：周期扫描相邻方块，在 {@link BlossomTransformRegistry} 中
 * 命中输入方块后从周围 mana 容器扣代价并就地置换为输出方块。一次 tick 最多置换一格邻居。
 *
 * <p>R58: 转化表完全数据驱动，规则定义在 {@code data/<ns>/blossom_transforms/*.json}。
 */
public class ManaBlossomBlockEntity extends BlockEntity {

    public static final int SCAN_INTERVAL = 40;
    public static final int RESERVOIR_SEARCH_RADIUS = 8;

    private int tickCounter = 0;

    public ManaBlossomBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_BLOSSOM_BE.get(), pos, state);
    }

    /**
     * 服务端 tick：累积到 {@link #SCAN_INTERVAL} 后扫一次六邻居，
     * 命中第一个可转化邻居即扣 mana、置换方块、播放粒子与音效，然后返回。
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaBlossomBlockEntity be) {
        be.tickCounter++;
        if (be.tickCounter < SCAN_INTERVAL) return;
        be.tickCounter = 0;
        if (!(level instanceof ServerLevel sl)) return;

        BlossomTransformRegistry registry = BlossomTransformRegistry.getInstance();
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlossomTransform transform = registry.get(level.getBlockState(neighborPos).getBlock());
            if (transform == null) continue;

            IManaHandler reservoir = findNearestReservoir(level, pos, transform.manaCost());
            if (reservoir == null) continue;

            int extracted = reservoir.extractMana(transform.manaCost(), false);
            if (extracted < transform.manaCost()) {
                reservoir.receiveMana(extracted, false);
                continue;
            }

            level.setBlockAndUpdate(neighborPos, transform.output().defaultBlockState());
            spawnTransformParticles(sl, neighborPos);
            sl.playSound(null, neighborPos, SoundEvents.AZALEA_PLACE, SoundSource.BLOCKS, 0.7F, 1.4F);
            return;
        }
    }

    /**
     * 在 {@link #RESERVOIR_SEARCH_RADIUS} 球范围内寻找存量不低于 {@code minMana} 的 mana 容器。
     * 返回第一个满足条件的 capability；不存在则返回 null。
     */
    @Nullable
    private static IManaHandler findNearestReservoir(Level level, BlockPos blossomPos, int minMana) {
        int r = RESERVOIR_SEARCH_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r * r) continue;
                    BlockPos check = blossomPos.offset(dx, dy, dz);
                    BlockEntity be = level.getBlockEntity(check);
                    if (be == null) continue;
                    IManaHandler handler = be.getCapability(ManaHandlerCapability.MANA_HANDLER).orElse(null);
                    if (handler != null && handler.getManaStored() >= minMana) {
                        return handler;
                    }
                }
            }
        }
        return null;
    }

    /** 在置换发生处释放 20 颗绿色光尘 + 8 颗村民开心粒子，作为玩家可见的成功反馈。 */
    private static void spawnTransformParticles(ServerLevel sl, BlockPos pos) {
        for (int i = 0; i < 20; i++) {
            double x = pos.getX() + 0.5 + (sl.random.nextDouble() - 0.5) * 1.0;
            double y = pos.getY() + 0.5 + (sl.random.nextDouble() - 0.5) * 1.0;
            double z = pos.getZ() + 0.5 + (sl.random.nextDouble() - 0.5) * 1.0;
            sl.sendParticles(new DustParticleOptions(
                            new Vector3f(0.4F + sl.random.nextFloat() * 0.3F,
                                    1.0F,
                                    0.5F + sl.random.nextFloat() * 0.3F),
                            1.5F),
                    x, y, z, 1, 0, 0.05, 0, 0.0);
        }
        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                8, 0.3, 0.3, 0.3, 0.0);
    }
}
