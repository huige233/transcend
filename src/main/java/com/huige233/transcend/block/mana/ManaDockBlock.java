package com.huige233.transcend.block.mana;

import com.huige233.transcend.items.ManaStorageItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import org.jetbrains.annotations.Nullable;

/** 魔力坞方块（输入/输出魔力的枢纽）。 */
public class ManaDockBlock extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Shapes.box(0.0625, 0.0, 0.0625, 0.9375, 0.875, 0.9375);

    public ManaDockBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(2.5F, 6.0F)
                .sound(SoundType.AMETHYST)
                .lightLevel(s -> 5)
                .noOcclusion()
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
        return new ManaDockBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level,
                                                                   @NotNull BlockState state,
                                                                   @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, p, st, be) -> {
            if (be instanceof ManaDockBlockEntity dock) {
                ManaDockBlockEntity.serverTick(lvl, p, st, dock);
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
        if (!(level.getBlockEntity(pos) instanceof ManaDockBlockEntity dock)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        boolean shift = player.isShiftKeyDown();

        if (held.isEmpty() && shift) {
            ManaDockBlockEntity.Mode next = dock.cycleMode();
            String key = switch (next) {
                case CHARGE -> "msg.transcend.mana_dock.mode_charge";
                case DRAIN  -> "msg.transcend.mana_dock.mode_drain";
                default     -> "msg.transcend.mana_dock.mode_off";
            };
            ChatFormatting color = switch (next) {
                case CHARGE -> ChatFormatting.AQUA;
                case DRAIN  -> ChatFormatting.GOLD;
                default     -> ChatFormatting.GRAY;
            };
            player.displayClientMessage(Component.translatable(key).withStyle(color), true);
            level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(),
                    SoundSource.BLOCKS, 0.5F, next == ManaDockBlockEntity.Mode.OFF ? 0.8F : 1.4F);
            return InteractionResult.CONSUME;
        }

        if (held.getItem() instanceof ManaStorageItem) {
            ItemStack swapped = dock.swapStorageItem(held.copy());
            if (!swapped.isEmpty()) {
                if (!player.getInventory().add(swapped)) {
                    player.drop(swapped, false);
                }
            }
            held.shrink(held.getCount());
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.BLOCKS, 0.7F, 1.2F);
            return InteractionResult.CONSUME;
        }

        if (held.isEmpty()) {
            ItemStack inDock = dock.takeStorageItem();
            if (!inDock.isEmpty()) {
                if (!player.getInventory().add(inDock)) {
                    player.drop(inDock, false);
                }
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK,
                        SoundSource.BLOCKS, 0.6F, 1.4F);
                return InteractionResult.CONSUME;
            }

            player.displayClientMessage(dock.statusLine(), true);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                         @NotNull BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof ManaDockBlockEntity dock) {
            ItemStack stored = dock.takeStorageItem();
            if (!stored.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level,
                        pos.getX(), pos.getY(), pos.getZ(), stored);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level,
                                      @Nullable BlockPos pos) {
        if (pos != null && level.getBlockEntity(pos) instanceof ManaDockBlockEntity dock) {
            return dock.getRedstoneSignal();
        }
        return 0;
    }
}
