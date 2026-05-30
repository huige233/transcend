package com.huige233.transcend.block.mana;

import com.huige233.transcend.world.mana.ChunkManaSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
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

/**
 * 地脉稳定器（Leyline Stabilizer）— 区块级"地脉缓冲"基础设施。
 *
 * <p>设计稿 D5 兑现：每个区块最多 1 个稳定器，存在时：
 * <ul>
 *   <li>抽取下限从 1000 → 750（mana well 等可在更低浓度下工作）</li>
 *   <li>{@link ChunkManaSavedData.Tier#WEAK} 级抽取惩罚从 -20% 减半为 -10%</li>
 *   <li>按设计每 1200 tick (1 分钟) 消耗 1 mana 维持运转 — 由 BE 实现</li>
 * </ul>
 *
 * <p>放置时检查"区块唯一性"：若该区块已注册稳定器则放置失败。
 * 自身不产 mana，纯粹是抽取下限放宽器。
 */
public class LeylineStabilizerBlock extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Shapes.box(0.0625, 0.0, 0.0625, 0.9375, 0.75, 0.9375);

    public LeylineStabilizerBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .strength(3.0F, 8.0F)
                .sound(SoundType.AMETHYST)
                .lightLevel(s -> 7)
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
        return new LeylineStabilizerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level,
                                                                   @NotNull BlockState state,
                                                                   @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, p, st, be) -> {
            if (be instanceof LeylineStabilizerBlockEntity stab) {
                LeylineStabilizerBlockEntity.serverTick(lvl, p, st, stab);
            }
        };
    }

    /**
     * 放置回调：检查区块唯一性。若该区块已存在稳定器则立刻自毁并提示玩家。
     */
    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state,
                            LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(level instanceof ServerLevel serverLevel)) return;

        ChunkManaSavedData data = ChunkManaSavedData.get(serverLevel);
        ChunkPos cp = new ChunkPos(pos);
        if (!data.addStabilizer(cp)) {
            // 该区块已有稳定器 → 自毁 + 掉落 + 提示
            level.destroyBlock(pos, true);
            if (placer instanceof Player p) {
                p.displayClientMessage(
                        Component.translatable("msg.transcend.leyline_stabilizer.duplicate")
                                .withStyle(ChatFormatting.RED), true);
            }
        } else {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.BLOCKS, 0.8F, 0.9F);
        }
    }

    /**
     * 方块被破坏时注销稳定器。
     */
    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                         @NotNull BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            ChunkManaSavedData.get(serverLevel).removeStabilizer(new ChunkPos(pos));
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /**
     * 空手右键 → 显示当前区块分级状态。
     */
    @SuppressWarnings("deprecation")
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level,
                                           @NotNull BlockPos pos, @NotNull Player player,
                                           @NotNull InteractionHand hand,
                                           @NotNull BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

        ChunkManaSavedData data = ChunkManaSavedData.get(serverLevel);
        ChunkPos cp = new ChunkPos(pos);
        float mana = data.getMana(cp);
        ChunkManaSavedData.Tier tier = data.getTier(cp);
        float multiplier = data.getExtractMultiplier(cp);
        float floor = data.getExtractFloor(cp);

        ChatFormatting tierColor = switch (tier) {
            case EXHAUSTED -> ChatFormatting.DARK_RED;
            case WEAK -> ChatFormatting.GOLD;
            case STABLE -> ChatFormatting.GREEN;
            case RICH -> ChatFormatting.AQUA;
        };
        String tierKey = "tier.transcend.chunk_mana." + tier.name().toLowerCase();

        player.displayClientMessage(
                Component.translatable("msg.transcend.leyline_stabilizer.status",
                        Component.translatable(tierKey).withStyle(tierColor),
                        String.format("%.0f", mana),
                        String.format("%.0f", floor),
                        String.format("%.0f%%", multiplier * 100))
                        .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return InteractionResult.CONSUME;
    }
}
