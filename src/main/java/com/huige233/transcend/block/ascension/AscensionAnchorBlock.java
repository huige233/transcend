package com.huige233.transcend.block.ascension;

import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.AscensionHandler;
import com.huige233.transcend.ascension.AscensionRitual;
import com.huige233.transcend.ascension.PlayerAscensionData;
import com.huige233.transcend.items.SoulMarkQuillItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/** 飞升锚点方块。 */
public class AscensionAnchorBlock extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

    public AscensionAnchorBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(5.0F, 12.0F)
                .sound(SoundType.AMETHYST)
                .lightLevel(s -> 8)
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
        return new AscensionAnchorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level,
                                                                   @NotNull BlockState state,
                                                                   @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) {
            return (lvl, p, st, be) -> {
                if (be instanceof AscensionAnchorBlockEntity anchor) {
                    AscensionAnchorBlockEntity.clientTick(lvl, p, st, anchor);
                }
            };
        }
        return (lvl, p, st, be) -> {
            if (be instanceof AscensionAnchorBlockEntity anchor) {
                AscensionAnchorBlockEntity.serverTick(lvl, p, st, anchor);
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
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof AscensionAnchorBlockEntity be)) return InteractionResult.PASS;

        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.getItem() instanceof SoulMarkQuillItem) {
            return handleSoulMarkBind(serverLevel, pos, player, be);
        }

        if (be.isRitualActive()) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.ascension_anchor.in_progress")
                            .withStyle(ChatFormatting.GOLD), true);
            return InteractionResult.CONSUME;
        }

        var dataOpt = player.getCapability(AscensionCapability.ASCENSION).resolve();
        if (dataOpt.isEmpty()) return InteractionResult.PASS;
        PlayerAscensionData data = dataOpt.get();

        AscensionRitual ritual = data.getPendingRitual();
        if (ritual == null) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.ascension_anchor.max_stage")
                            .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return InteractionResult.CONSUME;
        }

        be.tryStartRitual(serverLevel, player, data, ritual);
        return InteractionResult.CONSUME;
    }

    private InteractionResult handleSoulMarkBind(ServerLevel level, BlockPos pos,
                                                  Player player, AscensionAnchorBlockEntity be) {
        if (be.isRitualActive()) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.soul_mark.cannot_bind_during_ritual")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        var dataOpt = player.getCapability(AscensionCapability.ASCENSION).resolve();
        if (dataOpt.isEmpty()) return InteractionResult.PASS;
        PlayerAscensionData data = dataOpt.get();

        if (data.getRitualTier() <= 0) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.soul_mark.requires_stage_1")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        net.minecraft.resources.ResourceLocation dim = level.dimension().location();

        if (player.getUUID().equals(be.getSoulMarkOwner())) {
            be.setSoulMarkOwner(null);
            data.removeSoulMark(dim, pos);
            if (player instanceof ServerPlayer sp) AscensionHandler.syncToClient(sp, data);
            player.displayClientMessage(
                    Component.translatable("msg.transcend.soul_mark.unbound")
                            .withStyle(ChatFormatting.GRAY), true);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK,
                    SoundSource.BLOCKS, 0.6F, 1.5F);
            return InteractionResult.CONSUME;
        }

        if (be.getSoulMarkOwner() != null) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.soul_mark.already_owned_by_other")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        boolean added = data.addSoulMark(dim, pos);
        if (!added) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.soul_mark.already_bound")
                            .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.CONSUME;
        }
        be.setSoulMarkOwner(player.getUUID());
        if (player instanceof ServerPlayer sp) AscensionHandler.syncToClient(sp, data);

        int count = data.getSoulMarks().size();
        int max = data.getMaxSoulMarks();
        player.displayClientMessage(
                Component.translatable("msg.transcend.soul_mark.bound", count, max)
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS, 1.0F, 1.5F);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                         @NotNull BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AscensionAnchorBlockEntity be) {

            if (be.isRitualActive() && level instanceof ServerLevel sl) {
                sl.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 0.5F);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
