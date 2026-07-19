package com.huige233.transcend.block.forge;

import com.huige233.transcend.gear.forge.AspectDef;
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

public class AspectCrucibleBlock extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

    public AspectCrucibleBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .strength(5.0F, 12.0F)
                .sound(SoundType.METAL)
                .lightLevel(s -> 7)
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
        return new AspectCrucibleBlockEntity(pos, state);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level,
                                           @NotNull BlockPos pos, @NotNull Player player,
                                           @NotNull InteractionHand hand,
                                           @NotNull BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof AspectCrucibleBlockEntity be)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        if (player.isShiftKeyDown() && held.isEmpty()) {
            be.cancelAndDropAll();
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 0.8F);
            player.displayClientMessage(
                    Component.translatable("msg.transcend.aspect_crucible.cancelled")
                            .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.CONSUME;
        }

        if (held.isEmpty() && be.isReady()) {
            AspectDef def = be.tryIgnite(player);
            if (def == null) return InteractionResult.PASS;

            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    40, 0.3, 0.3, 0.3, 0.05);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    30, 0.3, 0.3, 0.3, 0.5);
            level.playSound(null, pos, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.0F, 1.2F);
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.5F);

            String resultKey = "msg.transcend.aspect_crucible.forged";
            Component aspectName = Component.translatable(def.nameKey())
                    .withStyle(def.dominant().color, ChatFormatting.BOLD);
            player.displayClientMessage(
                    Component.translatable(resultKey, aspectName)
                            .withStyle(ChatFormatting.GOLD), true);
            return InteractionResult.CONSUME;
        }

        if (held.isEmpty()) {
            if (!be.hasItem()) {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.aspect_crucible.empty")
                                .withStyle(ChatFormatting.GRAY), true);
            } else {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.aspect_crucible.need_catalysts",
                                be.filledCatalystCount(), AspectCrucibleBlockEntity.CATALYST_COUNT)
                                .withStyle(ChatFormatting.AQUA), true);
            }
            return InteractionResult.CONSUME;
        }

        if (held.getItem() instanceof com.huige233.transcend.items.forge.CatalystItem) {
            int rc = be.tryInsertCatalyst(player, held);
            switch (rc) {
                case 0 -> {
                    level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 0.8F, 1.4F);
                    if (be.isReady()) {
                        player.displayClientMessage(
                                Component.translatable("msg.transcend.aspect_crucible.ready")
                                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
                    }
                }
                case 1 -> player.displayClientMessage(
                        Component.translatable("msg.transcend.aspect_crucible.no_item")
                                .withStyle(ChatFormatting.RED), true);
                case 2 -> player.displayClientMessage(
                        Component.translatable("msg.transcend.aspect_crucible.full")
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
                        Component.translatable("msg.transcend.aspect_crucible.item_loaded")
                                .withStyle(ChatFormatting.AQUA), true);
            }
            case 1 -> player.displayClientMessage(
                    Component.translatable("msg.transcend.aspect_crucible.has_item")
                            .withStyle(ChatFormatting.RED), true);
            case 2 -> player.displayClientMessage(
                    Component.translatable("msg.transcend.aspect_crucible.invalid_item")
                            .withStyle(ChatFormatting.RED), true);
            case 3 -> player.displayClientMessage(
                    Component.translatable("msg.transcend.aspect_crucible.already_forged")
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
            if (level.getBlockEntity(pos) instanceof AspectCrucibleBlockEntity be) {
                be.dropAllOnRemove();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
