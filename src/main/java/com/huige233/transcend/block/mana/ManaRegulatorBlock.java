package com.huige233.transcend.block.mana;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
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

/** 魔力调节器方块（限制/调节流量）。 */
public class ManaRegulatorBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final VoxelShape SHAPE = Shapes.box(0.125, 0.125, 0.125, 0.875, 0.875, 0.875);

    public ManaRegulatorBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(2.0F, 6.0F)
                .sound(SoundType.AMETHYST)
                .lightLevel(s -> 4)
                .noOcclusion()
                .requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                         @NotNull BlockPos pos, @NotNull CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {

        Direction out = ctx.getNearestLookingDirection().getOpposite();
        return this.defaultBlockState().setValue(FACING, out);
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ManaRegulatorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level,
                                                                   @NotNull BlockState state,
                                                                   @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, p, st, be) -> {
            if (be instanceof ManaRegulatorBlockEntity reg) {
                ManaRegulatorBlockEntity.serverTick(lvl, p, st, reg);
            }
        };
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level,
                                           @NotNull BlockPos pos, @NotNull Player player,
                                           @NotNull InteractionHand hand,
                                           @NotNull BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof ManaRegulatorBlockEntity reg)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty()) return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {

            int p = reg.cyclePriority();
            int rateCm = ManaRegulatorBlockEntity.rateForPriority(p);
            player.displayClientMessage(
                    Component.translatable("msg.transcend.mana_regulator.priority", p, rateCm)
                            .withStyle(ChatFormatting.GOLD), true);
            level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(),
                    SoundSource.BLOCKS, 0.4F, 0.8F + 0.15F * p);
            return InteractionResult.CONSUME;
        }

        Direction cur = state.getValue(FACING);
        Direction next = cycleFacing(cur);
        level.setBlock(pos, state.setValue(FACING, next), 3);
        player.displayClientMessage(
                Component.translatable("msg.transcend.mana_regulator.facing",
                        Component.translatable("direction.transcend." + next.getName()))
                        .withStyle(ChatFormatting.AQUA), true);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.BLOCKS, 0.5F, 1.6F);
        return InteractionResult.CONSUME;
    }

    private static Direction cycleFacing(Direction d) {
        return switch (d) {
            case UP    -> Direction.DOWN;
            case DOWN  -> Direction.NORTH;
            case NORTH -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST  -> Direction.EAST;
            case EAST  -> Direction.UP;
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull BlockState rotate(@NotNull BlockState state, @NotNull Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull BlockState mirror(@NotNull BlockState state, @NotNull Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level,
                                      @NotNull BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ManaRegulatorBlockEntity reg) {
            return reg.getPriority() * 4;
        }
        return 0;
    }
}
