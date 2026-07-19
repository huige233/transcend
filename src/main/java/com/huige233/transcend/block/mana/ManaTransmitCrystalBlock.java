package com.huige233.transcend.block.mana;

import com.huige233.transcend.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
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

public class ManaTransmitCrystalBlock extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Shapes.box(0.3125, 0.0, 0.3125, 0.6875, 0.625, 0.6875);

    public ManaTransmitCrystalBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .strength(2.0F, 4.0F)
                .sound(SoundType.AMETHYST)
                .lightLevel(s -> 8)
                .noOcclusion());
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                         @NotNull BlockPos pos, @NotNull CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction,
                                            @NotNull BlockState neighborState, @NotNull net.minecraft.world.level.LevelAccessor level,
                                            @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {

        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ManaTransmitCrystalBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level,
                                                                    @NotNull BlockState state,
                                                                    @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) {
            return (lvl, pos, st, be) -> {
                if (be instanceof ManaTransmitCrystalBlockEntity crystal) {
                    ManaTransmitCrystalBlockEntity.clientTick(lvl, pos, st, crystal);
                }
            };
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof ManaTransmitCrystalBlockEntity crystal) {
                ManaTransmitCrystalBlockEntity.serverTick(lvl, pos, st, crystal);
            }
        };
    }
}
