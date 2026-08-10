package com.huige233.transcend.block.circle;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 法阵符文方块（符文组件）。 */
public class CircleRuneBlock extends Block {

    private final int tier;

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 2, 14),
            Block.box(4, 2, 4, 12, 14, 12),
            Block.box(2, 14, 2, 14, 16, 14)
    );

    public CircleRuneBlock(int tier) {
        super(Properties.of()
            .mapColor(MapColor.LAPIS)
            .requiresCorrectToolForDrops()
            .strength(2.5F, 5.0F)
            .sound(SoundType.AMETHYST)

            .lightLevel(state -> tier >= 2 ? 4 : 1)
            .noOcclusion());
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
