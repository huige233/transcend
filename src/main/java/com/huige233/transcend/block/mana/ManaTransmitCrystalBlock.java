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

/**
 * Round 42: 魔力传输水晶 — 灵感来自龙之研究 Energy Crystal。
 *
 * <p>玩家放置在 ManaReservoirBlock 上方 → 水晶链接下方储液池作为"端点"。
 * 用 {@link com.huige233.transcend.items.ManaCrystalBinderItem} 工具右键两个水晶 →
 * 双向绑定 → 自动从一池抽取 mana 推送到另一池（最大 64 格距离）。
 *
 * <p>不使用魔力发射器的飞射 bolt 视觉，而是仅在两端发出微弱脉冲粒子，
 * 强调"无线 P2P 传输"的简洁感。
 */
public class ManaTransmitCrystalBlock extends Block implements EntityBlock {

    /** 4x4x4 居中小水晶 voxel */
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
        // 水晶可放在三类基底之上：
        //   - 魔力储液池（直接读 capability）
        //   - 法环核心（直接读 capability）
        //   - 符文石（无 capability，通过附近 4 格球形扫描查找最近 mana 提供者）
        Block below = level.getBlockState(pos.below()).getBlock();
        return below instanceof ManaReservoirBlock
                || below instanceof com.huige233.transcend.block.circle.MagicCircleCoreBlock
                || below instanceof com.huige233.transcend.block.circle.CircleRuneBlock;
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction,
                                            @NotNull BlockState neighborState, @NotNull net.minecraft.world.level.LevelAccessor level,
                                            @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
        // 下方储液池被破坏 → 水晶自毁
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
