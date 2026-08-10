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

/** 调谐凿子物品（法阵）。 */
public class AttunementChiselItem extends Item {

    private static final String SELECTED_TIER_TAG = "SelectedCircleTier";
    private static final int GHOST_DURATION_TICKS = 200;

    public AttunementChiselItem() {
        super(new Properties().stacksTo(1).durability(256));
        ModItems.ITEMS.add(this);
    }

    public static int getSelectedTier(ItemStack stack) {
        int tier = stack.getOrCreateTag().getInt(SELECTED_TIER_TAG);
        return tier >= 1 && tier <= 5 ? tier : 1;
    }

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

        if (!(ctx.getPlayer() instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof MagicCircleCoreBlockEntity coreBe)) {
            return InteractionResult.PASS;
        }
        if (!coreBe.claimOrAuthorize(serverPlayer)) {
            return InteractionResult.FAIL;
        }
        ItemStack chisel = ctx.getItemInHand();

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

        ServerLevel serverLevel = (ServerLevel) level;
        int selectedTierLevel = getSelectedTier(chisel);

        CircleStructureCache cache = CircleStructureValidator.validate(serverLevel, pos);

        chisel.hurtAndBreak(1, ctx.getPlayer(), p -> p.broadcastBreakEvent(ctx.getHand()));

        coreBe.markStructureDirty();

        if (cache.isValid() && cache.getTier() != null) {
            int detectedLevel = cache.getTier().getLevel();

            if (detectedLevel >= selectedTierLevel) {

                level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE,
                        SoundSource.BLOCKS, 1.0F, 1.2F);
                ctx.getPlayer().displayClientMessage(
                        Component.translatable("msg.transcend.chisel.valid",
                                Component.translatable(cache.getTier().getTranslationKey()))
                                .withStyle(ChatFormatting.GREEN), true);

                if (ctx.getPlayer() instanceof ServerPlayer sp) {
                    NetworkHandler.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> sp),
                            S2CCircleGhostBlocks.clear(pos));
                }
            } else {

                showMissingForHigherTier(serverLevel, pos, selectedTierLevel, ctx.getPlayer());
            }
        } else {

            showMissingForHigherTier(serverLevel, pos, selectedTierLevel, ctx.getPlayer());
        }

        return InteractionResult.CONSUME;
    }

    private void showMissingForHigherTier(ServerLevel level, BlockPos corePos,
                                           int targetTierLevel,
                                           net.minecraft.world.entity.player.Player player) {

        CircleTier targetTier = CircleTier.fromLevel(targetTierLevel);
        CircleStructureCache targetCache = CircleStructureValidator.validateForTier(level, corePos, targetTier);
        var missingEntries = targetCache.getMissingEntries();
        if (missingEntries == null) {
            missingEntries = java.util.Collections.emptyList();
        }

        level.playSound(null, corePos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5F, 1.5F);
        player.displayClientMessage(
                Component.translatable("msg.transcend.chisel.missing",
                        Component.translatable(targetTier.getTranslationKey()),
                        missingEntries.size())
                        .withStyle(ChatFormatting.RED), true);

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
