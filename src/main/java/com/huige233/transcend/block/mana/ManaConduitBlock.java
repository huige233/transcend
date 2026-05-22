package com.huige233.transcend.block.mana;

import com.huige233.transcend.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.SoundType;
import org.jetbrains.annotations.Nullable;

/**
 * Round 21: Typed Mana Conduit — 类型化魔力输送方块。
 *
 * <p>4 aspect (Aether/Blood/Cosmic/Tainted) 各独立存储池 (每种最大 1000)。
 * 每 20 tick 与邻居 conduit 平衡 — 高浓度向低浓度流动差值的 10%。
 * 与 Ars Nouveau Source Network / Botania Mana Spreader 同等定位 — 可见、可链接、可控的法力管线。
 */
public class ManaConduitBlock extends Block implements EntityBlock {

    public ManaConduitBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(1.5F)
                .sound(SoundType.AMETHYST)
                .lightLevel(s -> 8)
                .requiresCorrectToolForDrops());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaConduitBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return type == ModBlockEntities.MANA_CONDUIT_BE.get()
                ? (lvl, pos, st, be) -> ManaConduitBlockEntity.serverTick(lvl, pos, st, (ManaConduitBlockEntity) be)
                : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ManaConduitBlockEntity conduit)) return InteractionResult.PASS;
        return conduit.onUse(player, hand);
    }
}
