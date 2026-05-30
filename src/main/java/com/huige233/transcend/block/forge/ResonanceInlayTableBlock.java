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

/**
 * R83: 共鸣镶嵌台（造物之道 B 阶段入口方块）。
 *
 * <p>交互流程：
 * <ol>
 *   <li>空台 + 手持装备（CRUCIBLE 已完成、sockets &lt; 4） → 装备入 slot 0</li>
 *   <li>有装备 + 手持共鸣水晶 → 立即在 NBT 写入一个 socket（消耗 1 水晶）</li>
 *   <li>空手右键 → 取回装备</li>
 *   <li>shift + 右键空手 → 取消并掉落</li>
 * </ol>
 *
 * <p>与坩埚不同：镶嵌没有"引燃"步骤，每颗水晶即写即生效，玩家可连续投。
 */
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

        // shift + 空手：取消
        if (player.isShiftKeyDown() && held.isEmpty()) {
            be.cancelAndDropAll();
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 0.8F);
            player.displayClientMessage(
                    Component.translatable("msg.transcend.resonance_inlay.cancelled")
                            .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.CONSUME;
        }

        // 空手：取回装备
        if (held.isEmpty()) {
            if (!be.hasItem()) {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.resonance_inlay.empty")
                                .withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.CONSUME;
            }
            // 反馈当前 socket 数后取回
            int sockets = GearForgeData.getSockets(be.getItemStack()).size();
            be.takeBack(player);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.0F);
            player.displayClientMessage(
                    Component.translatable("msg.transcend.resonance_inlay.taken_back",
                            sockets, GearForgeData.MAX_RESONANCE_SOCKETS)
                            .withStyle(ChatFormatting.AQUA), true);
            return InteractionResult.CONSUME;
        }

        // 共鸣水晶：尝试镶嵌
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

        // 装备投入
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
