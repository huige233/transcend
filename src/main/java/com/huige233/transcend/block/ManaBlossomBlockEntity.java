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

public class ManaBlossomBlockEntity extends BlockEntity {

    public static final int SCAN_INTERVAL = 40;
    public static final int RESERVOIR_SEARCH_RADIUS = 8;

    private int tickCounter = 0;

    public ManaBlossomBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_BLOSSOM_BE.get(), pos, state);
    }

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
