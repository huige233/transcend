package com.huige233.transcend.block.augment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/** 强化符文方块（法阵强化组件）。 */
public class AugmentRuneBlock extends Block {

    public enum AugmentType {
        HASTE(MapColor.COLOR_RED),
        EFFICIENCY(MapColor.COLOR_GREEN),
        PRESERVATION(MapColor.COLOR_BLUE);

        public final MapColor mapColor;

        AugmentType(MapColor mapColor) {
            this.mapColor = mapColor;
        }
    }

    private static final VoxelShape SHAPE = Shapes.box(0.1875, 0.0, 0.1875, 0.8125, 0.5, 0.8125);

    private final AugmentType type;

    public AugmentRuneBlock(AugmentType type) {
        super(Properties.of()
                .mapColor(type.mapColor)
                .strength(2.5F, 5.0F)
                .sound(SoundType.AMETHYST_CLUSTER)
                .lightLevel(s -> 9)
                .noOcclusion()
                .randomTicks());
        this.type = type;
    }

    public AugmentType getAugmentType() {
        return type;
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                         @NotNull BlockPos pos, @NotNull CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public void animateTick(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull RandomSource rand) {
        if (rand.nextInt(4) != 0) return;
        ParticleOptions particle = switch (type) {
            case HASTE -> ParticleTypes.CRIT;
            case EFFICIENCY -> ParticleTypes.HAPPY_VILLAGER;
            case PRESERVATION -> ParticleTypes.ENCHANT;
        };
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.6;
        double cz = pos.getZ() + 0.5;
        double angle = rand.nextDouble() * Math.PI * 2;
        double r = 0.25 + rand.nextDouble() * 0.15;
        level.addParticle(particle,
                cx + Math.cos(angle) * r, cy, cz + Math.sin(angle) * r,
                0, 0.04, 0);
    }
}
