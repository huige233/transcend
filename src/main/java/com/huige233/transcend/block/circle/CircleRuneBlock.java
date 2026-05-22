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

/**
 * 法环符文 — 携带符文纹路的法阵节点方块。
 * 与基石配套，承担法阵主要的能量流转。
 * 也可作为魔力导管，让储液池通过符文石从远端法环核心拉取魔力。
 */
public class CircleRuneBlock extends Block {
    // 阶级（1-5），决定符文的复杂度与发光强度
    private final int tier;

    // 碰撞/选取形状：底座台阶 + 中部柱体 + 顶部台阶
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 2, 14),   // 底座
            Block.box(4, 2, 4, 12, 14, 12),   // 柱体
            Block.box(2, 14, 2, 14, 16, 14)   // 顶部
    );

    public CircleRuneBlock(int tier) {
        super(Properties.of()
            .mapColor(MapColor.LAPIS)
            .requiresCorrectToolForDrops()
            .strength(2.5F, 5.0F)
            .sound(SoundType.AMETHYST)
            // 2阶及以上符文光强提升，否则维持微弱辉光
            .lightLevel(state -> tier >= 2 ? 4 : 1)
            .noOcclusion());
        this.tier = tier;
    }

    // 获取阶级
    public int getTier() {
        return tier;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
