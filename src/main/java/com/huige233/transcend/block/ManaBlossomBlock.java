package com.huige233.transcend.block;

import com.huige233.transcend.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
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
 * Round 50: Mana Blossom — Pure Daisy 风魔化花。
 *
 * <p>种植在草方块/泥土上，自动从 8 格内 reservoir 拉 mana，转化邻居建筑方块。
 * 灵感 1:1 来自 Botania Pure Daisy。
 *
 * <p>可被破坏掉落自身 — 持续工作直到玩家移除。
 */
public class ManaBlossomBlock extends BushBlock implements EntityBlock {

    private static final VoxelShape SHAPE = Shapes.box(0.25, 0.0, 0.25, 0.75, 0.75, 0.75);

    public ManaBlossomBlock() {
        super(Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .lightLevel(s -> 7)
                .noOcclusion());
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                         @NotNull BlockPos pos, @NotNull CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return state.is(net.minecraft.tags.BlockTags.DIRT) || state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ManaBlossomBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level,
                                                                    @NotNull BlockState state,
                                                                    @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof ManaBlossomBlockEntity blossom) {
                ManaBlossomBlockEntity.serverTick(lvl, pos, st, blossom);
            }
        };
    }
}
