package com.huige233.transcend.block;

import com.huige233.transcend.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

/** 定义黑洞种子培育机方块，连接培育更新、操作菜单和拆除掉落。 */
public class BlackHoleSeedBreederBlock extends BaseEntityBlock {
    public BlackHoleSeedBreederBlock() { super(Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BlackHoleSeedBreederBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.BLACK_HOLE_SEED_BREEDER.get(), BlackHoleSeedBreederBlockEntity::tick);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof BlackHoleSeedBreederBlockEntity be) || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        NetworkHooks.openScreen(serverPlayer, new net.minecraft.world.MenuProvider() {
            @Override public Component getDisplayName() { return Component.translatable("block.transcend.black_hole_seed_breeder"); }
            @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player p) { return new com.huige233.transcend.menu.BlackHoleSeedBreederMenu(id, inventory, be); }
        }, pos);
        return InteractionResult.CONSUME;
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BlackHoleSeedBreederBlockEntity be) be.dropContents();
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
