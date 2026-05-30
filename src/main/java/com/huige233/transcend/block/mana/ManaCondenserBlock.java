package com.huige233.transcend.block.mana;

import com.huige233.transcend.world.mana.ChunkManaSavedData;
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

/**
 * Mana Condenser — 把 mana_well 输出转换为网络可用的 IManaHandler 节点。
 *
 * <p>设计稿 D3 兑现：放在 mana_well 6 邻面任一方向，well 检测到此方块时不再
 * 在世界生成水晶物品实体，而是直接注入此方块的内部缓冲；
 * 缓冲通过 {@link ManaHandlerCapability} 暴露，可被 spreader / reservoir 拉走。
 */
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

    /**
     * 空手右键 → actionbar 显示缓冲量与该区块 tier。
     */
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
            ChunkManaSavedData data = ChunkManaSavedData.get(sl);
            ChunkPos cp = new ChunkPos(pos);
            ChunkManaSavedData.Tier tier = data.getTier(cp);
            chunkInfo = " §7| §o" + tier.name();
        }
        player.displayClientMessage(
                Component.literal(String.format("§b[Condenser] §3%d§7/§3%d CM%s",
                        cond.getStoredMana(), cond.getCapacity(), chunkInfo))
                        .withStyle(ChatFormatting.AQUA), true);
        return InteractionResult.CONSUME;
    }

    /**
     * 提供 0-15 的比较器输出 = 缓冲百分比，用于自动化检测溢出。
     */
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
