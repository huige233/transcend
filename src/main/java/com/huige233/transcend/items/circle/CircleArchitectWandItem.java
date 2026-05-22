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

/**
 * 法阵建造杖 — 一键搭建多方块法阵。
 *
 * <p>交互：
 * <ul>
 *   <li><b>Shift + 右键空中</b>：循环选择 Tier (T1→T2→T3→T4→T5→T1)。</li>
 *   <li><b>右键法环核心方块</b>：按选定 Tier 一次性搭建所有缺失方块。
 *       创造模式免费；生存模式从玩家背包消耗对应方块物品。</li>
 *   <li><b>Shift + 右键法环核心方块</b>：干跑 (dry-run)，只汇报需要的方块清单不实际建造。</li>
 * </ul>
 *
 * <p>不会替换非空的非可替换方块，遇到冲突时整个建造取消并提示玩家。
 */
public class CircleArchitectWandItem extends Item {

    private static final String NBT_TIER = "SelectedTier";

    public CircleArchitectWandItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE).durability(0));
        ModItems.ITEMS.add(this);
    }

    /** 读取选定 tier (1-5)。 */
    public static int getSelectedTier(ItemStack stack) {
        int t = stack.getOrCreateTag().getInt(NBT_TIER);
        return t >= 1 && t <= 5 ? t : 1;
    }

    private static void cycleSelectedTier(ItemStack stack) {
        int cur = getSelectedTier(stack);
        int next = cur >= 5 ? 1 : cur + 1;
        stack.getOrCreateTag().putInt(NBT_TIER, next);
    }

    // ============================================================
    // Shift + 右键空中：循环 Tier
    // ============================================================

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

    // ============================================================
    // 右键核心方块：建造
    // ============================================================

    @Override
    public @NotNull InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);

        // 必须点击一个法环核心方块
        if (!(state.getBlock() instanceof MagicCircleCoreBlock)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

        ItemStack wand = ctx.getItemInHand();
        int tier = getSelectedTier(wand);
        CircleTier ct = CircleTier.fromLevel(tier);

        // Shift + 右键核心 → dry-run
        if (player.isShiftKeyDown()) {
            return dryRun(serverLevel, player, pos, ct);
        }

        // 普通右键核心 → 建造
        return build(serverLevel, player, pos, ct, wand);
    }

    /**
     * 干跑：列出当前 tier 需要建造哪些方块、与玩家背包的差距，但不修改世界。
     */
    private InteractionResult dryRun(ServerLevel level, Player player, BlockPos corePos, CircleTier tier) {
        BuildPlan plan = computePlan(level, corePos, tier);
        if (plan.toPlace.isEmpty() && plan.conflicts.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.architect_wand.already_built",
                            Component.translatable(tier.getTranslationKey()))
                            .withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.SUCCESS;
        }

        // 计数需求
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

    /**
     * 实际建造：先校验冲突，再消耗材料，再放置方块。
     */
    private InteractionResult build(ServerLevel level, Player player, BlockPos corePos,
                                     CircleTier tier, ItemStack wand) {
        BuildPlan plan = computePlan(level, corePos, tier);
        boolean creative = player.isCreative();

        if (!plan.conflicts.isEmpty()) {
            // 有冲突方块阻碍建造
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

        // 生存模式：检查并消耗材料
        if (!creative) {
            Map<String, Integer> missing = computeInventoryShortfall(player, plan.requirements);
            if (!missing.isEmpty()) {
                int totalMissing = missing.values().stream().mapToInt(Integer::intValue).sum();
                player.displayClientMessage(
                        Component.translatable("msg.transcend.architect_wand.no_materials", totalMissing)
                                .withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
            // 实际消耗
            consumeFromInventory(player, plan.requirements);
        }

        // 放置方块
        int placed = 0;
        for (Map.Entry<BlockPos, BlockState> e : plan.toPlace.entrySet()) {
            BlockPos p = e.getKey();
            BlockState s = e.getValue();
            // 三方位旋转中性，直接放
            level.setBlock(p, s, 3);
            placed++;
        }

        // 通知核心重新校验结构
        if (level.getBlockEntity(corePos) instanceof MagicCircleCoreBlockEntity coreBe) {
            coreBe.markStructureDirty();
        }

        // 音效
        level.playSound(null, corePos, SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS, 1.0F, 1.4F);
        player.displayClientMessage(
                Component.translatable("msg.transcend.architect_wand.built",
                        placed, Component.translatable(tier.getTranslationKey()))
                        .withStyle(ChatFormatting.GREEN), false);
        return InteractionResult.CONSUME;
    }

    // ============================================================
    // 计划构造
    // ============================================================

    /**
     * 编译本次建造的计划：
     * - toPlace：需要 setBlock 的位置 → BlockState 映射
     * - conflicts：被非空非可替换方块占据的位置（阻塞建造）
     * - requirements：物品 → 数量（生存模式消耗清单）
     */
    private static BuildPlan computePlan(ServerLevel level, BlockPos corePos, CircleTier tier) {
        BuildPlan plan = new BuildPlan();
        List<PatternEntry> entries = CircleStructurePattern.getPatternForTier(tier);

        for (PatternEntry entry : entries) {
            if (entry.role() == BlockRole.CORE) continue; // 核心已存在
            BlockPos target = corePos.offset(entry.dx(), entry.dy(), entry.dz());
            BlockState existing = level.getBlockState(target);
            BlockState desired = CircleBlockPalette.stateFor(entry.role(), entry.minBlockTier());
            if (desired == null) continue;

            // 已经满足 → 跳过
            if (existing.getBlock() == desired.getBlock()) continue;

            // 是否可替换
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

    /** 计算玩家背包还差多少各项材料。返回 {物品注册名: 缺口数量}。 */
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

    /** 实际从玩家背包扣除材料（仅在 computeInventoryShortfall 返回空时调用）。 */
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

    // ============================================================
    // Tooltip
    // ============================================================

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

    // ============================================================
    // 辅助容器
    // ============================================================

    private static final class BuildPlan {
        final Map<BlockPos, BlockState> toPlace = new HashMap<>();
        final java.util.List<BlockPos> conflicts = new java.util.ArrayList<>();
        final Map<String, Integer> requirements = new HashMap<>();
    }
}
