package com.huige233.transcend.block.mana;

import com.huige233.transcend.world.mana.ChunkManaSavedData;
import com.huige233.transcend.world.mana.ChunkManaObservation;
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

/** 地脉稳定器方块（稳定地脉）。 */
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

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state,
                            LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(level instanceof ServerLevel serverLevel)) return;

        ChunkManaSavedData data = ChunkManaSavedData.get(serverLevel);
        ChunkPos cp = new ChunkPos(pos);
        if (!data.addStabilizer(cp)) {

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

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                         @NotNull BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            ChunkManaSavedData.get(serverLevel).removeStabilizer(new ChunkPos(pos));
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level,
                                           @NotNull BlockPos pos, @NotNull Player player,
                                           @NotNull InteractionHand hand,
                                           @NotNull BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

        ChunkPos cp = new ChunkPos(pos);
        ChunkManaObservation.Sample observation = ChunkManaObservation.observe(serverLevel, cp);
        if (!observation.known()) {
            player.displayClientMessage(Component.literal("[Leyline Stabilizer] UNKNOWN")
                    .withStyle(ChatFormatting.GRAY), false);
            return InteractionResult.CONSUME;
        }
        float mana = observation.mana().orElseThrow();
        ChunkManaSavedData.Tier tier = observation.tier();
        float multiplier = observation.extractMultiplier().orElseThrow();
        float floor = observation.stabilized()
                ? ChunkManaSavedData.STABILIZED_EXTRACT_FLOOR : ChunkManaSavedData.TIER_WEAK_FLOOR;

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
