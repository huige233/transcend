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
 * <p><b>R66 重设计</b>：放开"必须放在 3 类专属基底"的硬约束，改为"任何能支撑物品的方块顶面"。
 * 这是对玩家反馈 "水晶应该可以独立放置" 与 "很多魔力机器没办法接入" 的直接响应：
 * <ul>
 *   <li>放在任何 mana 机器顶部 → resolveSource 直接读下方 capability（首选路径）</li>
 *   <li>放在装饰方块 / 实心方块顶部 → resolveSource 走 4 格球扫描找最近 capability</li>
 *   <li>不再限制为 ManaReservoir / MagicCircleCore / CircleRuneBlock 三类</li>
 * </ul>
 *
 * <p>用 {@link com.huige233.transcend.items.ManaCrystalBinderItem} 工具右键两个水晶 →
 * 双向追加绑定（R63 多链）→ 自动从一池抽取 mana 推送到对端（最大 64 格距离）。
 *
 * <p>R62 + R65 渲染器在两端之间渲染加性激光束（向量法，方向正确）。
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

    /**
     * R66: 放开放置约束。只要下方方块有支撑物品的顶面（与 vanilla 物品框 / 火把放置规则一致）即可。
     *
     * <p>用 {@link Block#canSupportCenter(LevelReader, BlockPos, Direction)} 检查下方方块的
     * 顶面中心是否能支撑 0.5 格的小物体。这与 Minecraft 内部 lever / button / repeater 等
     * 小型方块的放置约束一致，允许：
     * <ul>
     *   <li>任何完整方块（石/木/各种 mana 机器）</li>
     *   <li>顶半砖、楼梯顶面、灯笼等有完整顶面的</li>
     * </ul>
     * 拒绝：
     * <ul>
     *   <li>下方为空气</li>
     *   <li>下方为非完整方块且顶面不能支撑（火堆、围栏、铁砧侧面等）</li>
     * </ul>
     */
    @Override
    public boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
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
