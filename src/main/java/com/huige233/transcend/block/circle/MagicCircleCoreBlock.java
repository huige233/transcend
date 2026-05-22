package com.huige233.transcend.block.circle;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import com.huige233.transcend.items.MagicCrystalItem;
import com.huige233.transcend.items.circle.FunctionSigilItem;

/**
 * 法环核心 — 法阵多方块结构的中心方块。
 * 不同 coreType 对应不同形态：
 *   dormant（沉眠）/ wellspring（涌泉）/ sanctuary（圣域）
 *   dominion（统御）/ waystone（界石）/ convergence（汇聚）/ primordial（始源）
 *
 * 实现 EntityBlock 以绑定 MagicCircleCoreBlockEntity。
 */
public class MagicCircleCoreBlock extends Block implements EntityBlock {
    // 核心形态标识
    private final String coreType;

    public MagicCircleCoreBlock(String coreType) {
        super(Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .requiresCorrectToolForDrops()
            .strength(5.0F, 12.0F)
            .sound(SoundType.AMETHYST)
            // 核心常态自发紫光
            .lightLevel(state -> 5)
            // 核心采用异形模型，需要透光渲染
            .noOcclusion());
        this.coreType = coreType;
    }

    public String getCoreType() {
        return coreType;
    }

    // ── EntityBlock 实现 ──────────────────────────────────────────

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MagicCircleCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof MagicCircleCoreBlockEntity coreBe) {
                MagicCircleCoreBlockEntity.serverTick(lvl, pos, st, coreBe);
            }
        };
    }

    // ── 右键交互 ─────────────────────────────────────────────────

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof MagicCircleCoreBlockEntity coreBe)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        // 魔力水晶充能
        if (held.getItem() instanceof MagicCrystalItem crystal) {
            int value = crystal.isRefined() ? 3 : 1;
            int inserted = coreBe.insertMana(value);
            if (inserted > 0) {
                if (!player.isCreative()) held.shrink(1);
                level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6F, 1.2F);
                player.displayClientMessage(
                        Component.literal("§b+" + inserted + " CM §7(" + coreBe.getStoredMana() + "/" + coreBe.getMaxMana() + ")")
                                .withStyle(ChatFormatting.AQUA), true);
                return InteractionResult.CONSUME;
            }
            player.displayClientMessage(
                    Component.literal("§c魔力已满").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        // 功能符印安装（一次性，安装后不可替换）
        if (held.getItem() instanceof FunctionSigilItem sigil) {
            if (coreBe.isSigilLocked() && coreBe.getActiveFunction() != null) {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.circle.sigil_locked")
                                .withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
            coreBe.setFunction(sigil.getFunctionType(), held.copy());
            if (!player.isCreative()) held.shrink(1);
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            
            // 尝试自动激活
            boolean activated = coreBe.activate();
            if (activated) {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.circle.sigil_activated",
                                Component.translatable(sigil.getFunctionType().getTranslationKey()))
                                .withStyle(ChatFormatting.GREEN), true);
            } else {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.circle.sigil_set",
                                Component.translatable(sigil.getFunctionType().getTranslationKey()))
                                .withStyle(ChatFormatting.GOLD), true);
            }
            return InteractionResult.CONSUME;
        }

        // 空手右键：打开GUI（蹲下则显示简要状态）
        if (held.isEmpty()) {
            if (player.isShiftKeyDown()) {
                // 蹲下+空手：简要状态
                String tierStr = coreBe.getDetectedTier() != null ? "T" + coreBe.getDetectedTier().getLevel() : "??";
                String stateStr = coreBe.getCircleState().name();
                player.displayClientMessage(
                        Component.literal("§7[" + tierStr + "] §d" + stateStr +
                                " §7| §b" + coreBe.getStoredMana() + "/" + coreBe.getMaxMana() + " CM"), true);
            } else {
                // 空手右键：打开法环核心GUI
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    net.minecraftforge.network.NetworkHooks.openScreen(sp, coreBe, buf -> {
                        buf.writeBlockPos(pos);
                        buf.writeVarInt(coreBe.getDetectedTier() != null ? coreBe.getDetectedTier().getLevel() : 0);
                        buf.writeVarInt(coreBe.getStoredMana());
                        buf.writeVarInt(coreBe.getMaxMana());
                        buf.writeBoolean(coreBe.isActive());
                        buf.writeBoolean(coreBe.isStructureValid());
                        buf.writeUtf(coreBe.getActiveFunction() != null ? coreBe.getActiveFunction().getId() : "");
                        buf.writeFloat(coreBe.getActiveFunction() != null ? coreBe.getActiveFunction().getBaseUpkeepPerMinute() : 0f);
                        // settingsCount — 与 CircleCoreData.decode() 对齐
                        int settingsCount = coreBe.getActiveFunction() != null
                                ? com.huige233.transcend.circle.CircleFunctionSettings.getSettingsFor(coreBe.getActiveFunction()).size()
                                : 0;
                        buf.writeVarInt(settingsCount);
                        // v2 字段：sigilLocked / missingBlockCount / catalystCount / catalystSatisfiedCount
                        buf.writeBoolean(coreBe.isSigilLocked());
                        buf.writeVarInt(coreBe.getMissingBlockCount());
                        buf.writeVarInt(coreBe.getCatalystCount());
                        buf.writeVarInt(coreBe.getCatalystSatisfiedCount());
                    });
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
