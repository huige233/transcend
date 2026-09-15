package com.huige233.transcend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;


/** 定义研究站方块并连接服务端研究更新和玩家操作菜单。 */
public final class ResearchStationBlock extends BaseEntityBlock {
    public ResearchStationBlock() { super(Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(4.0F).sound(SoundType.METAL)); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ResearchStationBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> { if (be instanceof ResearchStationBlockEntity station) ResearchStationBlockEntity.tick(lvl, station); };
    }
    @Override public InteractionResult use(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof ResearchStationBlockEntity station && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, station, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
