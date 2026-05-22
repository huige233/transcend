package com.huige233.transcend.block;

import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.PlayerAscensionData;
import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.world.nexus.NexusManager;
import com.huige233.transcend.world.nexus.NexusType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * 法则枢纽核心方块 — 发光、高硬度、需要飞升阶段3+才能破坏。
 * 被破坏时永久改变一条游戏规则。
 */
public class NexusCoreBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = Block.box(2, 2, 2, 14, 14, 14);

    public NexusCoreBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(50.0F, 1200.0F)
                .sound(SoundType.AMETHYST)
                .lightLevel(state -> 15)
                .requiresCorrectToolForDrops()
                .pushReaction(PushReaction.BLOCK)
                .noOcclusion()
                .isViewBlocking((s, g, p) -> false)
                .isSuffocating((s, g, p) -> false));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NexusCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide) {
            return createTickerHelper(type, ModBlockEntities.NEXUS_CORE_BE.get(),
                    NexusCoreBlockEntity::clientTick);
        }
        return createTickerHelper(type, ModBlockEntities.NEXUS_CORE_BE.get(),
                NexusCoreBlockEntity::serverTick);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        // Check lock state
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof NexusCoreBlockEntity coreBE && coreBE.isLocked()) {
            return 0.0F; // Locked after crystal revert
        }
        // Check ascension stage — must be Stage 3+
        PlayerAscensionData data = AscensionCapability.get(player);
        if (data.getStage() < 3) {
            return 0.0F; // Cannot break
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, @Nullable BlockGetter level,
                                 java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.transcend.nexus_core.lore"));
        tooltip.add(Component.translatable("tooltip.transcend.nexus_core.lore2"));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!oldState.is(newState.getBlock()) && !level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof NexusCoreBlockEntity coreBE) {
                NexusType type = NexusType.getById(coreBE.getNexusTypeId());
                // Note: actual destruction handling is done in NexusDimensionEvents.onBlockBreak
            }
        }
        super.onRemove(oldState, level, pos, newState, moved);
    }

    /**
     * 玩家攻击时显示提示。
     */
    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof NexusCoreBlockEntity coreBE && coreBE.isLocked()) {
                int seconds = coreBE.getLockTicks() / 20;
                player.sendSystemMessage(Component.translatable("msg.transcend.nexus_locked", seconds)
                        .withStyle(ChatFormatting.RED));
                return;
            }
            PlayerAscensionData data = AscensionCapability.get(player);
            if (data.getStage() < 3) {
                player.sendSystemMessage(Component.translatable("msg.transcend.nexus_too_weak")
                        .withStyle(ChatFormatting.RED));
            }
        }
    }
}
