package com.huige233.transcend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;


/** 定义可配置长整型容量的储能方块，并连接储能菜单和服务端输电更新。 */
public class LongStorageBlock extends BaseEntityBlock {
    private final long capacity;
    public LongStorageBlock(long capacity, MapColor color) {
        super(Properties.of().mapColor(color).strength(4.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
        this.capacity = capacity;
    }
    public long capacity() { return capacity; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public InteractionResult use(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof LongStorageBlockEntity storage) {
            NetworkHooks.openScreen(serverPlayer, storage, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LongStorageBlockEntity(pos, state, capacity);
    }
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            net.minecraft.world.level.Level level, BlockState state,
            net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, blockState, entity) -> {
            if (entity instanceof LongStorageBlockEntity storage) LongStorageBlockEntity.tick(lvl, storage);
        };
    }
}
