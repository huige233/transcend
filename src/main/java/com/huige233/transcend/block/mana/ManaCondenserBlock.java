package com.huige233.transcend.block.mana;

import com.huige233.transcend.world.mana.ChunkManaSavedData;
import com.huige233.transcend.world.mana.ChunkManaObservation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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

/** 魔力冷凝器方块（浓缩魔力）。 */
public class ManaCondenserBlock extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Shapes.box(0.125, 0.0, 0.125, 0.875, 0.875, 0.875);

    public ManaCondenserBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .strength(2.5F, 6.0F)
                .sound(SoundType.AMETHYST)
                .lightLevel(s -> 6)
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
        return new ManaCondenserBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level,
                                                                   @NotNull BlockState state,
                                                                   @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, p, st, be) -> {
            if (be instanceof ManaCondenserBlockEntity cond) {
                ManaCondenserBlockEntity.serverTick(lvl, p, st, cond);
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
        if (!(level.getBlockEntity(pos) instanceof ManaCondenserBlockEntity cond)) return InteractionResult.PASS;

        String chunkInfo = "";
        if (level instanceof ServerLevel sl) {
            ChunkPos cp = new ChunkPos(pos);
            ChunkManaObservation.Sample observation = ChunkManaObservation.observe(sl, cp);
            chunkInfo = " §7| §o" + (observation.known() ? observation.tier().name() : "UNKNOWN");
        }
        player.displayClientMessage(
                Component.literal(String.format("§b[Condenser] §3%d§7/§3%d CM%s",
                        cond.getStoredMana(), cond.getCapacity(), chunkInfo))
                        .withStyle(ChatFormatting.AQUA), true);
        return InteractionResult.CONSUME;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level,
                                      @NotNull BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ManaCondenserBlockEntity cond) {
            int max = cond.getCapacity();
            return max <= 0 ? 0 : Math.min(15, (int) ((long) cond.getStoredMana() * 15L / max));
        }
        return 0;
    }
}
