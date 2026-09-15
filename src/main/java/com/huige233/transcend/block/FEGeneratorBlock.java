package com.huige233.transcend.block;

import com.huige233.transcend.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

/** 定义燃料、风力及创造发电机方块，处理直接充能、端口切换和菜单交互。 */
public class FEGeneratorBlock extends BaseEntityBlock {
    private final int production;
    private final FEGeneratorBlockEntity.Mode mode;
    public FEGeneratorBlock(FEGeneratorBlockEntity.Mode mode, int production, MapColor color) { super(Properties.of().mapColor(color).strength(4.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()); this.mode=mode; this.production=production; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof FEGeneratorBlockEntity be) be.dropContents();
        super.onRemove(state, level, pos, newState, moving);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FEGeneratorBlockEntity.Generator(type(), pos, state, production, mode); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof FEGeneratorBlockEntity be)) return InteractionResult.PASS;
        if (player.getItemInHand(hand).getItem() instanceof com.huige233.transcend.items.tech.PhantomEnergyBlockItem) {
            int accepted = be.receiveExternalEnergy(com.huige233.transcend.items.tech.PhantomEnergyBlockItem.ENERGY);
            if (accepted > 0 && !player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
            player.displayClientMessage(Component.translatable("msg.transcend.phantom_energy_block.used", accepted), true);
            return InteractionResult.CONSUME;
        }
        if (player.getItemInHand(hand).getItem() instanceof com.huige233.transcend.items.tools.TranscendWrenchItem) {
            be.cycleFace(hit.getDirection());
            player.displayClientMessage(Component.translatable("msg.transcend.wrench.face", hit.getDirection().getName(), be.faceMode(hit.getDirection()).name()), true);
            return InteractionResult.CONSUME;
        }
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        NetworkHooks.openScreen(sp, new net.minecraft.world.MenuProvider() {
            public Component getDisplayName() { return Component.translatable("block.transcend." + mode.name().toLowerCase() + "_generator"); }
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, Player p) { return new com.huige233.transcend.menu.GeneratorMenu(id, inv, be); }
        }, pos);
        return InteractionResult.CONSUME;
    }
    private net.minecraft.world.level.block.entity.BlockEntityType<?> type() { return switch(mode) { case FIRE -> ModBlockEntities.FIRE_GENERATOR.get(); case WIND -> ModBlockEntities.WIND_GENERATOR.get(); case CREATIVE -> ModBlockEntities.CREATIVE_GENERATOR.get(); }; }
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) { if (type != type()) return null; return (lvl, pos, blockState, blockEntity) -> FEGeneratorBlockEntity.tick(lvl, pos, blockState, (FEGeneratorBlockEntity) blockEntity); }
}
