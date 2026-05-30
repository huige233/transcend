package com.huige233.transcend.block.ascension;

import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.AscensionHandler;
import com.huige233.transcend.ascension.AscensionRitual;
import com.huige233.transcend.ascension.PlayerAscensionData;
import com.huige233.transcend.items.SoulMarkQuillItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

/**
 * R67: 进阶图案锚（Ascension Anchor）— 玩家在周围摆放正多边形水晶图案完成阶进。
 *
 * <p>4 个图案对应 4 个 {@link AscensionRitual}：
 * <ul>
 *   <li>3 水晶等边三角形 (R=3) → AWAKENING (stage 0→1)</li>
 *   <li>4 水晶正方形 (R=4) → TEMPERING (stage 1→2)</li>
 *   <li>5 水晶正五边形 (R=5) → PURIFICATION (stage 2→3)</li>
 *   <li>6 水晶正六边形 (R=6) → TRANSCENDENCE (stage 3→4)</li>
 * </ul>
 *
 * <p>玩家右键 → 服务端检测当前 stage 应做的仪式 → 校验图案 + 击杀/施法/Boss 前置 +
 * 物品 + 网络 mana → 5 秒动画 → 完成阶进。具体校验与触发流程见 {@link AscensionAnchorBlockEntity}。
 *
 * <p>UI 按钮触发路径（C2SAscensionAction）保留作为 fallback；此方块是星辉风的另一条进阶路径。
 */
public class AscensionAnchorBlock extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

    public AscensionAnchorBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(5.0F, 12.0F)
                .sound(SoundType.AMETHYST)
                .lightLevel(s -> 8)
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
        return new AscensionAnchorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level,
                                                                   @NotNull BlockState state,
                                                                   @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) {
            return (lvl, p, st, be) -> {
                if (be instanceof AscensionAnchorBlockEntity anchor) {
                    AscensionAnchorBlockEntity.clientTick(lvl, p, st, anchor);
                }
            };
        }
        return (lvl, p, st, be) -> {
            if (be instanceof AscensionAnchorBlockEntity anchor) {
                AscensionAnchorBlockEntity.serverTick(lvl, p, st, anchor);
            }
        };
    }

    /**
     * 右键交互：尝试启动当前阶段对应的进阶仪式。
     *
     * <p>处理顺序：
     * <ol>
     *   <li>仪式正在进行中 → 提示"仪式进行中，请稍候"</li>
     *   <li>玩家无 PlayerAscensionData → 提示（不应出现）</li>
     *   <li>当前 stage 已达 4 → 提示"你已飞升到极限"</li>
     *   <li>调用 BE 的 {@code tryStartRitual} 走完整校验链（图案 / 前置 / 物品 / mana）</li>
     * </ol>
     */
    @SuppressWarnings("deprecation")
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level,
                                           @NotNull BlockPos pos, @NotNull Player player,
                                           @NotNull InteractionHand hand,
                                           @NotNull BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof AscensionAnchorBlockEntity be)) return InteractionResult.PASS;

        // R76: 灵魂烙印之笔 — 优先分支
        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.getItem() instanceof SoulMarkQuillItem) {
            return handleSoulMarkBind(serverLevel, pos, player, be);
        }

        if (be.isRitualActive()) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.ascension_anchor.in_progress")
                            .withStyle(ChatFormatting.GOLD), true);
            return InteractionResult.CONSUME;
        }

        var dataOpt = player.getCapability(AscensionCapability.ASCENSION).resolve();
        if (dataOpt.isEmpty()) return InteractionResult.PASS;
        PlayerAscensionData data = dataOpt.get();

        AscensionRitual ritual = data.getPendingRitual();
        if (ritual == null) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.ascension_anchor.max_stage")
                            .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return InteractionResult.CONSUME;
        }

        be.tryStartRitual(serverLevel, player, data, ritual);
        return InteractionResult.CONSUME;
    }

    /**
     * R76: 灵魂烙印绑定 / 解绑分支。
     * <ul>
     *   <li>仪式进行中 → 拒绝</li>
     *   <li>玩家 stage &lt; 1 → 拒绝（需完成觉醒）</li>
     *   <li>此锚已被本人烙印 → 解绑</li>
     *   <li>此锚被他人烙印 → 拒绝</li>
     *   <li>未被任何人烙印 → 绑定（超过 stage 上限时 FIFO 淘汰最早一个）</li>
     * </ul>
     */
    private InteractionResult handleSoulMarkBind(ServerLevel level, BlockPos pos,
                                                  Player player, AscensionAnchorBlockEntity be) {
        if (be.isRitualActive()) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.soul_mark.cannot_bind_during_ritual")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        var dataOpt = player.getCapability(AscensionCapability.ASCENSION).resolve();
        if (dataOpt.isEmpty()) return InteractionResult.PASS;
        PlayerAscensionData data = dataOpt.get();

        if (data.getStage() <= 0) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.soul_mark.requires_stage_1")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        net.minecraft.resources.ResourceLocation dim = level.dimension().location();

        // 已被本人烙印 → 解绑
        if (player.getUUID().equals(be.getSoulMarkOwner())) {
            be.setSoulMarkOwner(null);
            data.removeSoulMark(dim, pos);
            if (player instanceof ServerPlayer sp) AscensionHandler.syncToClient(sp, data);
            player.displayClientMessage(
                    Component.translatable("msg.transcend.soul_mark.unbound")
                            .withStyle(ChatFormatting.GRAY), true);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK,
                    SoundSource.BLOCKS, 0.6F, 1.5F);
            return InteractionResult.CONSUME;
        }

        // 已被他人烙印 → 拒绝
        if (be.getSoulMarkOwner() != null) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.soul_mark.already_owned_by_other")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        // 未烙印 → 绑定
        boolean added = data.addSoulMark(dim, pos);
        if (!added) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.soul_mark.already_bound")
                            .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.CONSUME;
        }
        be.setSoulMarkOwner(player.getUUID());
        if (player instanceof ServerPlayer sp) AscensionHandler.syncToClient(sp, data);

        int count = data.getSoulMarks().size();
        int max = data.getMaxSoulMarks();
        player.displayClientMessage(
                Component.translatable("msg.transcend.soul_mark.bound", count, max)
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS, 1.0F, 1.5F);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                         @NotNull BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AscensionAnchorBlockEntity be) {
            // 仪式中途方块被破坏 → 播放失败音效（避免静默丢失）
            if (be.isRitualActive() && level instanceof ServerLevel sl) {
                sl.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 0.5F);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
