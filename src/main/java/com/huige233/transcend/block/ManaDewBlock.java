package com.huige233.transcend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 魔力炎露：寄生于岩浆之上的凝结结晶。
 * 下方必须为岩浆流体方块，否则在世界中无法存在并立刻自毁。
 */
public class ManaDewBlock extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Shapes.box(0.25, 0.0, 0.25, 0.75, 0.25, 0.75);

    public ManaDewBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .strength(0.2F, 1.0F)
                .sound(SoundType.AMETHYST_CLUSTER)
                .lightLevel(s -> 12)
                .noOcclusion()
                .noLootTable());
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                         @NotNull BlockPos pos, @NotNull CollisionContext ctx) {
        return SHAPE;
    }

    /** 仅当下方为岩浆（source 或 flow）时可存在。 */
    @Override
    public boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        return level.getFluidState(pos.below()).is(FluidTags.LAVA);
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction dir,
                                            @NotNull BlockState neighbor, @NotNull LevelAccessor level,
                                            @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
        if (dir == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ManaDewBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state,
                                                                    @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof ManaDewBlockEntity dew) {
                ManaDewBlockEntity.serverTick(lvl, pos, st, dew);
            }
        };
    }
}
