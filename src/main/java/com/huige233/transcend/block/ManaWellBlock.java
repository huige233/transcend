package com.huige233.transcend.block;

import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.world.mana.ChunkManaSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** 魔力井方块：生成/存储魔力。 */
public class ManaWellBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 2, 15),
            Block.box(3, 2, 3, 13, 10, 13),
            Block.box(2, 10, 2, 14, 12, 14)
    );

    public ManaWellBlock() {
        super(Properties.of()
                .mapColor(MapColor.LAPIS)
                .strength(3.0F, 6.0F)
                .sound(net.minecraft.world.level.block.SoundType.AMETHYST)
                .lightLevel(state -> 7)
                .noOcclusion());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaWellBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return createTickerHelper(type, ModBlockEntities.MANA_WELL_BE.get(), ManaWellBlockEntity::clientTick);
        }
        return createTickerHelper(type, ModBlockEntities.MANA_WELL_BE.get(), ManaWellBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ManaWellBlockEntity well) {
            Optional<Float> chunkMana = well.getChunkManaDisplay();
            float buffer = well.getManaBuffer();
            int crystals = well.getStoredCrystals();
            boolean working = well.isWorking();

            player.sendSystemMessage(Component.translatable("msg.transcend.mana_well.status"));
            if (chunkMana.isPresent()) {
                player.sendSystemMessage(Component.translatable("msg.transcend.mana_well.chunk_mana",
                        String.format("%.1f", chunkMana.orElseThrow()),
                        String.format("%.0f", ChunkManaSavedData.DEFAULT_MANA)));
            } else {
                player.sendSystemMessage(Component.literal("Chunk mana: UNKNOWN"));
            }
            player.sendSystemMessage(Component.translatable("msg.transcend.mana_well.buffer",
                    String.format("%.1f", buffer)));
            if (crystals > 0) {
                player.sendSystemMessage(Component.translatable("msg.transcend.mana_well.crystals",
                        crystals));
            }
            player.sendSystemMessage(Component.translatable(
                    working ? "msg.transcend.mana_well.working" : "msg.transcend.mana_well.stopped"));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ManaWellBlockEntity well) {
                well.dropContents();
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction dir) {
        return false;
    }
}
