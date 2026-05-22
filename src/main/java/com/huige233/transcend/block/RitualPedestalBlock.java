package com.huige233.transcend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class RitualPedestalBlock extends BaseEntityBlock {

    public RitualPedestalBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.DEEPSLATE)
                .strength(4.0F)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .lightLevel(s -> 3));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RitualPedestalBlockEntity pedestal)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        ItemStack onPedestal = pedestal.getItem();

        if (onPedestal.isEmpty() && !held.isEmpty()) {
            pedestal.setItem(held.split(1));
            return InteractionResult.CONSUME;
        } else if (!onPedestal.isEmpty()) {
            if (!player.getInventory().add(onPedestal.copy())) {
                player.drop(onPedestal.copy(), false);
            }
            pedestal.setItem(ItemStack.EMPTY);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RitualPedestalBlockEntity pedestal && !pedestal.getItem().isEmpty()) {
                Block.popResource(level, pos, pedestal.getItem());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RitualPedestalBlockEntity(pos, state);
    }
}
