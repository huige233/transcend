package com.huige233.transcend.block.mana;

import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.items.circle.BoundAetherPearlItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 魔力散发器（Mana Spreader）— 类植物魔法的魔力流动方块。
 *
 * <p>行为：
 * <ul>
 *   <li>每 10 tick 从 6 个相邻方块中具备 IManaHandler 且可抽取的容器拉取魔力。</li>
 *   <li>若已绑定目标且有魔力，按目标方向推送魔力到目标的 IManaHandler。</li>
 *   <li>视觉：每次推送在散发器与目标之间画一条粒子轨迹。</li>
 * </ul>
 *
 * <p>交互：
 * <ul>
 *   <li>右键散发器 + 持有已绑定的 {@link BoundAetherPearlItem}：将珠子上记录的坐标写入散发器作为目标。</li>
 *   <li>Shift + 右键散发器（空手）：清除目标绑定。</li>
 *   <li>普通右键散发器（空手）：显示当前状态（魔力 / 目标坐标）。</li>
 *   <li>放置时朝向取决于玩家面向（FACING）— 与目标无关，仅做视觉。</li>
 * </ul>
 */
public class ManaSpreaderBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final VoxelShape SHAPE = Shapes.box(0.125, 0.0, 0.125, 0.875, 0.875, 0.875);

    public ManaSpreaderBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_MAGENTA)
                .strength(2.5F, 6.0F)
                .sound(SoundType.AMETHYST)
                .lightLevel(s -> 6)
                .noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // 朝向玩家头顶方向（视线投放）的反向：放在地上脸朝上、放在墙上朝水平、放在天花板朝下
        Direction dir = ctx.getNearestLookingDirection().getOpposite();
        return this.defaultBlockState().setValue(FACING, dir);
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state,
                            @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                         @NotNull BlockPos pos, @NotNull CollisionContext ctx) {
        return SHAPE;
    }

    // ============================================================
    // EntityBlock
    // ============================================================

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ManaSpreaderBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level,
                                                                   @NotNull BlockState state,
                                                                   @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, p, st, be) -> {
            if (be instanceof ManaSpreaderBlockEntity sp) {
                ManaSpreaderBlockEntity.serverTick(lvl, p, st, sp);
            }
        };
    }

    // ============================================================
    // 右键交互
    // ============================================================

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level,
                                           @NotNull BlockPos pos, @NotNull Player player,
                                           @NotNull InteractionHand hand,
                                           @NotNull BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof ManaSpreaderBlockEntity be)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        // 持有已绑定的以太珠 → 把绑定坐标写入散发器
        if (held.getItem() instanceof BoundAetherPearlItem) {
            if (BoundAetherPearlItem.isBound(held)) {
                BlockPos target = BoundAetherPearlItem.getBoundPos(held).orElse(null);
                if (target == null || target.equals(pos)) {
                    player.displayClientMessage(
                            Component.translatable("msg.transcend.spreader.invalid_target")
                                    .withStyle(ChatFormatting.RED), true);
                    return InteractionResult.FAIL;
                }
                int distSq = (int) target.distSqr(pos);
                int maxRange = ManaSpreaderBlockEntity.MAX_RANGE;
                if (distSq > maxRange * maxRange) {
                    player.displayClientMessage(
                            Component.translatable("msg.transcend.spreader.out_of_range",
                                            (int) Math.sqrt(distSq), maxRange)
                                    .withStyle(ChatFormatting.RED), true);
                    return InteractionResult.FAIL;
                }
                be.setBoundTarget(target);
                player.displayClientMessage(
                        Component.translatable("msg.transcend.spreader.bound",
                                target.getX(), target.getY(), target.getZ())
                                .withStyle(ChatFormatting.GREEN), true);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                        SoundSource.BLOCKS, 0.7F, 1.4F);
                return InteractionResult.CONSUME;
            } else {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.spreader.pearl_unbound")
                                .withStyle(ChatFormatting.YELLOW), true);
                return InteractionResult.FAIL;
            }
        }

        // 空手 + 蹲下 → 清除目标
        if (held.isEmpty() && player.isShiftKeyDown()) {
            be.setBoundTarget(null);
            player.displayClientMessage(
                    Component.translatable("msg.transcend.spreader.unbound")
                            .withStyle(ChatFormatting.GOLD), true);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK,
                    SoundSource.BLOCKS, 0.5F, 1.6F);
            return InteractionResult.CONSUME;
        }

        // 空手 → 显示状态
        if (held.isEmpty()) {
            BlockPos target = be.getBoundTarget();
            String status = target == null
                    ? Component.translatable("msg.transcend.spreader.no_target").getString()
                    : "→ " + target.getX() + "," + target.getY() + "," + target.getZ();
            player.displayClientMessage(
                    Component.literal("§b" + be.getStoredMana() + "/" + be.getCapacity()
                            + " CM §7| " + status), true);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public @NotNull BlockState rotate(@NotNull BlockState state, @NotNull net.minecraft.world.level.block.Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public @NotNull BlockState mirror(@NotNull BlockState state, @NotNull net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
