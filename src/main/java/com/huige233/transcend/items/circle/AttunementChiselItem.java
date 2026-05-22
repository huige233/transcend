package com.huige233.transcend.items.circle;

import com.huige233.transcend.block.circle.MagicCircleCoreBlock;
import com.huige233.transcend.block.circle.MagicCircleCoreBlockEntity;
import com.huige233.transcend.circle.CircleStructureCache;
import com.huige233.transcend.circle.CircleStructureValidator;
import com.huige233.transcend.circle.CircleTier;
import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.network.S2CCircleGhostBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 调律刻刀 — 用于校验法阵多方块结构。
 *
 * <p>Shift + 右键核心方块：循环选择要检查的法阵等级（T1-T5）。
 * <p>普通右键核心方块：按选定等级验证结构，若有缺失方块则通过
 * shader 在客户端渲染 ghost 方块预览。
 */
public class AttunementChiselItem extends Item {

    private static final String SELECTED_TIER_TAG = "SelectedCircleTier";
    private static final int GHOST_DURATION_TICKS = 200;

    public AttunementChiselItem() {
        super(new Properties().stacksTo(1).durability(256));
        ModItems.ITEMS.add(this);
    }

    /** 从物品 NBT 读取选定等级（1-5，默认 1） */
    public static int getSelectedTier(ItemStack stack) {
        int tier = stack.getOrCreateTag().getInt(SELECTED_TIER_TAG);
        return tier >= 1 && tier <= 5 ? tier : 1;
    }

    /** 循环到下一个等级 */
    private static void cycleSelectedTier(ItemStack stack) {
        int current = getSelectedTier(stack);
        int next = current >= 5 ? 1 : current + 1;
        stack.getOrCreateTag().putInt(SELECTED_TIER_TAG, next);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof MagicCircleCoreBlock)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (ctx.getPlayer() == null) return InteractionResult.PASS;
        ItemStack chisel = ctx.getItemInHand();

        // Shift + 右键：切换选定等级
        if (ctx.getPlayer().isShiftKeyDown()) {
            cycleSelectedTier(chisel);
            int tier = getSelectedTier(chisel);
            CircleTier circleTier = CircleTier.fromLevel(tier);
            ctx.getPlayer().displayClientMessage(
                    Component.translatable("msg.transcend.chisel.tier_selected",
                            Component.translatable(circleTier.getTranslationKey()))
                            .withStyle(ChatFormatting.GOLD), true);
            level.playSound(null, pos, SoundEvents.UI_STONECUTTER_SELECT_RECIPE,
                    SoundSource.BLOCKS, 0.8F, 1.0F + tier * 0.1F);
            return InteractionResult.CONSUME;
        }

        // 普通右键：按选定等级验证结构
        ServerLevel serverLevel = (ServerLevel) level;
        int selectedTierLevel = getSelectedTier(chisel);

        // 执行结构校验
        CircleStructureCache cache = CircleStructureValidator.validate(serverLevel, pos);

        // 扣除耐久
        chisel.hurtAndBreak(1, ctx.getPlayer(), p -> p.broadcastBreakEvent(ctx.getHand()));

        // 更新方块实体
        if (level.getBlockEntity(pos) instanceof MagicCircleCoreBlockEntity coreBe) {
            coreBe.markStructureDirty();
        }

        if (cache.isValid() && cache.getTier() != null) {
            int detectedLevel = cache.getTier().getLevel();

            if (detectedLevel >= selectedTierLevel) {
                // 当前结构已满足选定等级
                level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE,
                        SoundSource.BLOCKS, 1.0F, 1.2F);
                ctx.getPlayer().displayClientMessage(
                        Component.translatable("msg.transcend.chisel.valid",
                                Component.translatable(cache.getTier().getTranslationKey()))
                                .withStyle(ChatFormatting.GREEN), true);

                // 清除 ghost 预览
                if (ctx.getPlayer() instanceof ServerPlayer sp) {
                    NetworkHandler.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> sp),
                            S2CCircleGhostBlocks.clear(pos));
                }
            } else {
                // 结构有效但未达到选定等级 → 显示缺失方块
                showMissingForHigherTier(serverLevel, pos, selectedTierLevel, ctx.getPlayer());
            }
        } else {
            // 结构完全无效 → 尝试显示选定等级的缺失
            showMissingForHigherTier(serverLevel, pos, selectedTierLevel, ctx.getPlayer());
        }

        return InteractionResult.CONSUME;
    }

    /**
     * 针对选定的更高等级重新验证，将缺失方块发送给客户端渲染 ghost 预览。
     */
    private void showMissingForHigherTier(ServerLevel level, BlockPos corePos,
                                           int targetTierLevel,
                                           net.minecraft.world.entity.player.Player player) {
        // 对目标等级做一次验证以获取缺失位置
        CircleTier targetTier = CircleTier.fromLevel(targetTierLevel);
        CircleStructureCache targetCache = CircleStructureValidator.validateForTier(level, corePos, targetTier);
        var missingEntries = targetCache.getMissingEntries();

        level.playSound(null, corePos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5F, 1.5F);
        player.displayClientMessage(
                Component.translatable("msg.transcend.chisel.missing",
                        Component.translatable(targetTier.getTranslationKey()),
                        missingEntries.size())
                        .withStyle(ChatFormatting.RED), true);

        // 发送 ghost 方块数据给客户端（携带角色信息）
        if (player instanceof ServerPlayer sp && !missingEntries.isEmpty()) {
            NetworkHandler.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    S2CCircleGhostBlocks.fromMissingEntries(corePos, targetTierLevel,
                            GHOST_DURATION_TICKS, missingEntries));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getSelectedTier(stack) > 1;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.transcend.chisel.desc")
                .withStyle(ChatFormatting.GRAY));

        int tier = getSelectedTier(stack);
        CircleTier circleTier = CircleTier.fromLevel(tier);
        tooltip.add(Component.translatable("tooltip.transcend.chisel.selected_tier",
                Component.translatable(circleTier.getTranslationKey()))
                .withStyle(ChatFormatting.GOLD));
    }
}
