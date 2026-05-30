package com.huige233.transcend.block.forge;

import com.huige233.transcend.gear.forge.BlessingDef;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
 * R86: 加冕祭坛（造物之道 D 阶段入口方块，5 阶段中的终极一步）。
 *
 * <p>交互流程与 R82 坩埚对称，但写入路径是 {@code GearForgeData.writeCelestial} 而非 writeCrucible。
 *
 * <ol>
 *   <li>空祭坛 + 装备（CRUCIBLE 已完成，CELESTIAL 未写入）→ 装备入 slot 0</li>
 *   <li>有装备 + 天命碎片 → 碎片入 slot 1..4 第一个空位</li>
 *   <li>4 碎片满 + 空手右键 → 加冕（依据当前月相 + biome 写入装备 NBT）</li>
 *   <li>shift + 空手右键 → 取消并退还所有</li>
 * </ol>
 */
public class CelestialAltarBlock extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

    public CelestialAltarBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(6.0F, 16.0F)
                .sound(SoundType.METAL)
                .lightLevel(s -> 12)
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
        return new CelestialAltarBlockEntity(pos, state);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level,
                                           @NotNull BlockPos pos, @NotNull Player player,
                                           @NotNull InteractionHand hand,
                                           @NotNull BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof CelestialAltarBlockEntity be)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        // shift + 空手 → 取消
        if (player.isShiftKeyDown() && held.isEmpty()) {
            be.cancelAndDropAll();
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 0.8F);
            player.displayClientMessage(
                    Component.translatable("msg.transcend.celestial_altar.cancelled")
                            .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.CONSUME;
        }

        // 空手 + ready → 加冕
        if (held.isEmpty() && be.isReady()) {
            BlessingDef def = be.tryCoronate(player);
            if (def == null) return InteractionResult.PASS;

            // 终极仪式 VFX
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                    60, 0.5, 0.6, 0.5, 0.08);
            serverLevel.sendParticles(ParticleTypes.GLOW,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    40, 0.4, 0.4, 0.4, 0.05);
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                    50, 0.4, 0.6, 0.4, 1.0);
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 1.5F);

            Component name = Component.translatable(def.nameKey())
                    .withStyle(def.dominant().color, ChatFormatting.BOLD);
            player.displayClientMessage(
                    Component.translatable("msg.transcend.celestial_altar.coronated", name)
                            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), false);
            return InteractionResult.CONSUME;
        }

        // 空手但未就绪
        if (held.isEmpty()) {
            if (!be.hasItem()) {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.celestial_altar.empty")
                                .withStyle(ChatFormatting.GRAY), true);
            } else {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.celestial_altar.need_fragments",
                                be.filledFragmentCount(), CelestialAltarBlockEntity.FRAGMENT_COUNT)
                                .withStyle(ChatFormatting.AQUA), true);
            }
            return InteractionResult.CONSUME;
        }

        // 碎片优先尝试
        if (held.getItem() instanceof com.huige233.transcend.items.forge.CelestialFragmentItem) {
            int rc = be.tryInsertFragment(player, held);
            switch (rc) {
                case 0 -> {
                    level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 0.9F, 1.6F);
                    if (be.isReady()) {
                        player.displayClientMessage(
                                Component.translatable("msg.transcend.celestial_altar.ready")
                                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
                    }
                }
                case 1 -> player.displayClientMessage(
                        Component.translatable("msg.transcend.celestial_altar.no_item")
                                .withStyle(ChatFormatting.RED), true);
                case 2 -> player.displayClientMessage(
                        Component.translatable("msg.transcend.celestial_altar.full")
                                .withStyle(ChatFormatting.RED), true);
                default -> {}
            }
            return InteractionResult.CONSUME;
        }

        // 装备投入
        int rc = be.tryInsertItem(player, held);
        switch (rc) {
            case 0 -> {
                level.playSound(null, pos, SoundEvents.NETHERITE_BLOCK_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
                player.displayClientMessage(
                        Component.translatable("msg.transcend.celestial_altar.item_loaded")
                                .withStyle(ChatFormatting.AQUA), true);
            }
            case 1 -> player.displayClientMessage(
                    Component.translatable("msg.transcend.celestial_altar.has_item")
                            .withStyle(ChatFormatting.RED), true);
            case 2 -> player.displayClientMessage(
                    Component.translatable("msg.transcend.celestial_altar.invalid_item")
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
            if (level.getBlockEntity(pos) instanceof CelestialAltarBlockEntity be) {
                be.dropAllOnRemove();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
