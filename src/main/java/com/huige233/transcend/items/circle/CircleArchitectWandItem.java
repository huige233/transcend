package com.huige233.transcend.items.circle;

import com.huige233.transcend.block.circle.MagicCircleCoreBlock;
import com.huige233.transcend.block.circle.MagicCircleCoreBlockEntity;
import com.huige233.transcend.circle.CircleBlockPalette;
import com.huige233.transcend.circle.CircleStructurePattern;
import com.huige233.transcend.circle.CircleStructurePattern.BlockRole;
import com.huige233.transcend.circle.CircleStructurePattern.PatternEntry;
import com.huige233.transcend.circle.CircleTier;
import com.huige233.transcend.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CircleArchitectWandItem extends Item {

    private static final String NBT_TIER = "SelectedTier";

    public CircleArchitectWandItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE).durability(0));
        ModItems.ITEMS.add(this);
    }

    public static int getSelectedTier(ItemStack stack) {
        int t = stack.getOrCreateTag().getInt(NBT_TIER);
        return t >= 1 && t <= 5 ? t : 1;
    }

    private static void cycleSelectedTier(ItemStack stack) {
        int cur = getSelectedTier(stack);
        int next = cur >= 5 ? 1 : cur + 1;
        stack.getOrCreateTag().putInt(NBT_TIER, next);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                cycleSelectedTier(stack);
                int tier = getSelectedTier(stack);
                CircleTier ct = CircleTier.fromLevel(tier);
                player.displayClientMessage(
                        Component.translatable("msg.transcend.architect_wand.tier_selected",
                                Component.translatable(ct.getTranslationKey()))
                                .withStyle(ChatFormatting.GOLD), true);
                level.playSound(null, player.blockPosition(), SoundEvents.UI_STONECUTTER_SELECT_RECIPE,
                        SoundSource.PLAYERS, 0.6F, 1.0F + tier * 0.1F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof MagicCircleCoreBlock)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Player player = ctx.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof MagicCircleCoreBlockEntity coreBe)) {
            return InteractionResult.PASS;
        }
        if (!coreBe.claimOrAuthorize(serverPlayer)) {
            return InteractionResult.FAIL;
        }

        ItemStack wand = ctx.getItemInHand();
        int tier = getSelectedTier(wand);
        CircleTier ct = CircleTier.fromLevel(tier);

        if (player.isShiftKeyDown()) {
            return dryRun(serverLevel, player, pos, ct);
        }

        return build(serverLevel, player, pos, ct, wand);
    }

    private InteractionResult dryRun(ServerLevel level, Player player, BlockPos corePos, CircleTier tier) {
        BuildPlan plan = computePlan(level, corePos, tier);
        if (plan.toPlace.isEmpty() && plan.conflicts.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.architect_wand.already_built",
                            Component.translatable(tier.getTranslationKey()))
                            .withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.SUCCESS;
        }

        int totalNeeded = plan.toPlace.size();
        int conflictCount = plan.conflicts.size();
        Map<String, Integer> missingFromInv = computeInventoryShortfall(player, plan.requirements);
        int totalMissing = missingFromInv.values().stream().mapToInt(Integer::intValue).sum();

        player.displayClientMessage(
                Component.translatable("msg.transcend.architect_wand.dryrun_summary",
                        Component.translatable(tier.getTranslationKey()),
                        totalNeeded, conflictCount, totalMissing)
                        .withStyle(ChatFormatting.YELLOW), false);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult build(ServerLevel level, Player player, BlockPos corePos,
                                     CircleTier tier, ItemStack wand) {
        BuildPlan plan = computePlan(level, corePos, tier);
        boolean creative = player.isCreative();

        if (!plan.conflicts.isEmpty()) {

            player.displayClientMessage(
                    Component.translatable("msg.transcend.architect_wand.conflict",
                            plan.conflicts.size())
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        if (plan.toPlace.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.architect_wand.already_built",
                            Component.translatable(tier.getTranslationKey()))
                            .withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.SUCCESS;
        }

        if (!creative) {
            Map<String, Integer> missing = computeInventoryShortfall(player, plan.requirements);
            if (!missing.isEmpty()) {
                int totalMissing = missing.values().stream().mapToInt(Integer::intValue).sum();
                player.displayClientMessage(
                        Component.translatable("msg.transcend.architect_wand.no_materials", totalMissing)
                                .withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }

            consumeFromInventory(player, plan.requirements);
        }

        int placed = 0;
        for (Map.Entry<BlockPos, BlockState> e : plan.toPlace.entrySet()) {
            BlockPos p = e.getKey();
            BlockState s = e.getValue();

            level.setBlock(p, s, 3);
            placed++;
        }

        if (level.getBlockEntity(corePos) instanceof MagicCircleCoreBlockEntity coreBe) {
            coreBe.markStructureDirty();
        }

        level.playSound(null, corePos, SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS, 1.0F, 1.4F);
        player.displayClientMessage(
                Component.translatable("msg.transcend.architect_wand.built",
                        placed, Component.translatable(tier.getTranslationKey()))
                        .withStyle(ChatFormatting.GREEN), false);
        return InteractionResult.CONSUME;
    }

    private static BuildPlan computePlan(ServerLevel level, BlockPos corePos, CircleTier tier) {
        BuildPlan plan = new BuildPlan();
        List<PatternEntry> entries = CircleStructurePattern.getPatternForTier(tier);

        for (PatternEntry entry : entries) {
            if (entry.role() == BlockRole.CORE) continue;
            BlockPos target = corePos.offset(entry.dx(), entry.dy(), entry.dz());
            BlockState existing = level.getBlockState(target);
            BlockState desired = CircleBlockPalette.stateFor(entry.role(), entry.minBlockTier());
            if (desired == null) continue;

            if (existing.getBlock() == desired.getBlock()) continue;

            boolean canReplace = existing.isAir()
                    || existing.canBeReplaced()
                    || existing.getFluidState().isSource() == false && existing.getFluidState().isEmpty();
            if (!canReplace) {
                plan.conflicts.add(target);
                continue;
            }

            plan.toPlace.put(target.immutable(), desired);
            String key = registryNameOf(desired);
            plan.requirements.merge(key, 1, Integer::sum);
        }

        return plan;
    }

    private static Map<String, Integer> computeInventoryShortfall(Player player,
                                                                   Map<String, Integer> requirements) {
        Map<String, Integer> shortfall = new HashMap<>();
        Inventory inv = player.getInventory();

        for (Map.Entry<String, Integer> req : requirements.entrySet()) {
            String key = req.getKey();
            int needed = req.getValue();
            int found = 0;
            for (ItemStack s : inv.items) {
                if (s.isEmpty()) continue;
                if (registryNameOf(s).equals(key)) {
                    found += s.getCount();
                    if (found >= needed) break;
                }
            }
            if (found < needed) {
                shortfall.put(key, needed - found);
            }
        }

        return shortfall;
    }

    private static void consumeFromInventory(Player player, Map<String, Integer> requirements) {
        Inventory inv = player.getInventory();
        for (Map.Entry<String, Integer> req : requirements.entrySet()) {
            String key = req.getKey();
            int remaining = req.getValue();
            for (int i = 0; i < inv.items.size() && remaining > 0; i++) {
                ItemStack s = inv.items.get(i);
                if (s.isEmpty()) continue;
                if (!registryNameOf(s).equals(key)) continue;
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
            }
        }
    }

    private static String registryNameOf(BlockState state) {
        return net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
    }

    private static String registryNameOf(ItemStack stack) {
        return net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return getSelectedTier(stack) >= 3;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.transcend.architect_wand.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.transcend.architect_wand.shift_use")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.transcend.architect_wand.use_core")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.transcend.architect_wand.shift_use_core")
                .withStyle(ChatFormatting.DARK_GRAY));
        int tier = getSelectedTier(stack);
        tooltip.add(Component.translatable("tooltip.transcend.architect_wand.selected_tier",
                Component.translatable(CircleTier.fromLevel(tier).getTranslationKey()))
                .withStyle(ChatFormatting.GOLD));
    }

    private static final class BuildPlan {
        final Map<BlockPos, BlockState> toPlace = new HashMap<>();
        final java.util.List<BlockPos> conflicts = new java.util.ArrayList<>();
        final Map<String, Integer> requirements = new HashMap<>();
    }
}
