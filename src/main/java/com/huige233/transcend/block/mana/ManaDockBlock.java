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

/**
 * 魔力充能座（Mana Dock）— 持物自动充放电站。
 *
 * <p>设计稿 D2 节兑现：玩家把 {@link ManaStorageItem} 放入插槽，
 * 工作座自动在网络与物品之间双向传输魔力，免除手动右键水晶充能。
 *
 * <p>三种工作模式（潜行右键空手循环切换）：
 * <ul>
 *   <li>{@code CHARGE} — 网络 → 物品。从相邻 {@code IManaHandler} 拉取魔力，注入插槽中的便携储魔器。</li>
 *   <li>{@code DRAIN} — 物品 → 网络。从插槽便携储魔器抽取魔力，推送到相邻 {@code IManaHandler}。</li>
 *   <li>{@code OFF} — 不进行任何传输。</li>
 * </ul>
 *
 * <p>常规右键插入/取出便携储魔器；空手非潜行右键查看状态。
 * 详细行为参见 {@link ManaDockBlockEntity}。
 */
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

        // 潜行 + 空手 = 切换模式
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

        // 持有储魔器 → 插入或交换
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

        // 空手非潜行 → 取出或显示状态
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
            // 没有物品 → 显示状态（actionbar）
            player.displayClientMessage(dock.statusLine(), true);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    /**
     * 方块被破坏时把插槽内的便携储魔器掉落出来。
     */
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

    /**
     * 比较器输出 = 内部缓冲百分比 (0-15)，方便玩家自动化。
     */
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
