package com.huige233.transcend.block;

import com.huige233.transcend.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/** 定义微型宇宙发电机方块的运行发光状态、充能交互、菜单和两端更新入口。 */
public class MiniUniverseGeneratorBlock extends BaseEntityBlock {
    public MiniUniverseGeneratorBlock() { super(Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(5.0F).sound(SoundType.METAL).lightLevel(s -> s.getValue(MiniUniverseGeneratorBlockEntity.ACTIVE) ? 10 : 0)); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) { builder.add(MiniUniverseGeneratorBlockEntity.ACTIVE); }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) { return defaultBlockState().setValue(MiniUniverseGeneratorBlockEntity.ACTIVE, false); }
    @Override public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (level.isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof MiniUniverseGeneratorBlockEntity be)) return net.minecraft.world.InteractionResult.PASS;
        if (player.getItemInHand(hand).getItem() instanceof com.huige233.transcend.items.tech.PhantomEnergyBlockItem) {
            int accepted = be.receiveExternalEnergy(com.huige233.transcend.items.tech.PhantomEnergyBlockItem.ENERGY);
            if (accepted > 0 && !player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
            player.displayClientMessage(Component.translatable("msg.transcend.phantom_energy_block.used", accepted), true);
            return net.minecraft.world.InteractionResult.CONSUME;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) return net.minecraft.world.InteractionResult.PASS;
        NetworkHooks.openScreen(serverPlayer, new net.minecraft.world.MenuProvider() {
            public Component getDisplayName() { return Component.translatable("block.transcend.mini_universe_generator"); }
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.entity.player.Player p) { return new com.huige233.transcend.menu.MiniUniverseGeneratorMenu(id, inventory, be); }
        }, pos);
        return net.minecraft.world.InteractionResult.CONSUME;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MiniUniverseGeneratorBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (type != ModBlockEntities.MINI_UNIVERSE_GENERATOR.get()) return null;
        if (level.isClientSide) {
            return (levelIn, posIn, stateIn, blockEntity) ->
                    MiniUniverseGeneratorBlockEntity.clientTick(levelIn, posIn, stateIn,
                            (MiniUniverseGeneratorBlockEntity) blockEntity);
        }
        return (levelIn, posIn, stateIn, blockEntity) ->
                MiniUniverseGeneratorBlockEntity.serverTick(levelIn, posIn, stateIn,
                        (MiniUniverseGeneratorBlockEntity) blockEntity);
    }
}
