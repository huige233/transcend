package com.huige233.transcend.block.mana;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ManaReservoirBlock extends Block implements EntityBlock {
    private final int capacity;
    private final int transferRate;

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 4, 15),
            Block.box(3, 4, 3, 13, 12, 13),
            Block.box(1, 12, 1, 15, 16, 15)
    );

    public ManaReservoirBlock(int capacity, int transferRate) {
        super(Properties.of()
                .mapColor(MapColor.ICE)
                .requiresCorrectToolForDrops()
                .strength(4.0F, 8.0F)
                .sound(SoundType.GLASS)
                .lightLevel(state -> 8)
                .noOcclusion());
        this.capacity = capacity;
        this.transferRate = transferRate;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getTransferRate() {
        return transferRate;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaReservoirBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {

        if (level.isClientSide) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof ManaReservoirBlockEntity reservoir) {
                ManaReservoirBlockEntity.serverTick(lvl, pos, st, reservoir);
            }
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof ManaReservoirBlockEntity reservoir)) {
            return InteractionResult.PASS;
        }

        InteractionResult chargeResult = reservoir.onUse(player, hand);
        if (chargeResult.consumesAction()) {
            return chargeResult;
        }

        int stored = reservoir.getManaStorage().getManaStored();
        int max = reservoir.getCapacity();
        player.displayClientMessage(Component.translatable("msg.transcend.reservoir.header"), false);
        player.displayClientMessage(Component.translatable("msg.transcend.reservoir.mana", stored, max), false);
        player.displayClientMessage(Component.translatable("msg.transcend.reservoir.transfer_rate",
                reservoir.getTransferRate()), false);
        return InteractionResult.SUCCESS;
    }
}
