package com.huige233.transcend.block.forge;

import com.huige233.transcend.gear.GearForgeData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class ResonanceInlayTableBlock extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.875, 1.0);

    public ResonanceInlayTableBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_CYAN)
                .strength(4.5F, 10.0F)
                .sound(SoundType.AMETHYST)
                .lightLevel(s -> 5)
                .requiresCorrectToolForDrops());
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                         @NotNull BlockPos pos, @NotNull CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ResonanceInlayTableBlockEntity(pos, state);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level,
                                           @NotNull BlockPos pos, @NotNull Player player,
                                           @NotNull InteractionHand hand,
                                           @NotNull BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof ResonanceInlayTableBlockEntity be)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        if (player.isShiftKeyDown() && held.isEmpty()) {
            be.cancelAndDropAll();
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 0.8F);
            player.displayClientMessage(
                    Component.translatable("msg.transcend.resonance_inlay.cancelled")
                            .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.CONSUME;
        }

        if (held.isEmpty()) {
            if (!be.hasItem()) {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.resonance_inlay.empty")
                                .withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.CONSUME;
            }

            int sockets = GearForgeData.getSockets(be.getItemStack()).size();
            be.takeBack(player);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.0F);
            player.displayClientMessage(
                    Component.translatable("msg.transcend.resonance_inlay.taken_back",
                            sockets, GearForgeData.MAX_RESONANCE_SOCKETS)
                            .withStyle(ChatFormatting.AQUA), true);
            return InteractionResult.CONSUME;
        }

        if (held.getItem() instanceof com.huige233.transcend.items.forge.ResonanceCrystalItem) {
            int rc = be.tryInsertCrystal(player, held);
            switch (rc) {
                case 0 -> {
                    int sockets = GearForgeData.getSockets(be.getItemStack()).size();
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                            pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                            12, 0.25, 0.15, 0.25, 0.6);
                    level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.9F, 1.3F);
                    if (sockets >= GearForgeData.MAX_RESONANCE_SOCKETS) {
                        player.displayClientMessage(
                                Component.translatable("msg.transcend.resonance_inlay.maxed",
                                        sockets, GearForgeData.MAX_RESONANCE_SOCKETS)
                                        .withStyle(ChatFormatting.GOLD), true);
                    } else {
                        player.displayClientMessage(
                                Component.translatable("msg.transcend.resonance_inlay.inserted",
                                        sockets, GearForgeData.MAX_RESONANCE_SOCKETS)
                                        .withStyle(ChatFormatting.AQUA), true);
                    }
                }
                case 1 -> player.displayClientMessage(
                        Component.translatable("msg.transcend.resonance_inlay.no_item")
                                .withStyle(ChatFormatting.RED), true);
                case 3 -> player.displayClientMessage(
                        Component.translatable("msg.transcend.resonance_inlay.full")
                                .withStyle(ChatFormatting.RED), true);
                default -> {}
            }
            return InteractionResult.CONSUME;
        }

        int rc = be.tryInsertItem(player, held);
        switch (rc) {
            case 0 -> {
                level.playSound(null, pos, SoundEvents.NETHERITE_BLOCK_PLACE, SoundSource.BLOCKS, 0.7F, 1.0F);
                player.displayClientMessage(
                        Component.translatable("msg.transcend.resonance_inlay.item_loaded")
                                .withStyle(ChatFormatting.AQUA), true);
            }
            case 1 -> player.displayClientMessage(
                    Component.translatable("msg.transcend.resonance_inlay.has_item")
                            .withStyle(ChatFormatting.RED), true);
            case 2 -> player.displayClientMessage(
                    Component.translatable("msg.transcend.resonance_inlay.invalid_item")
                            .withStyle(ChatFormatting.RED), true);
            default -> {}
        }
        return InteractionResult.CONSUME;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                          @NotNull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof ResonanceInlayTableBlockEntity be) {
                be.dropAllOnRemove();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
