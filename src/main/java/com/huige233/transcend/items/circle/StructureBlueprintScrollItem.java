package com.huige233.transcend.items.circle;

import com.huige233.transcend.circle.CircleBlockPalette;
import com.huige233.transcend.circle.CircleStructurePattern;
import com.huige233.transcend.circle.CircleStructurePattern.BlockRole;
import com.huige233.transcend.circle.CircleStructurePattern.PatternEntry;
import com.huige233.transcend.circle.CircleStructureCache;
import com.huige233.transcend.circle.CircleTier;
import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.network.S2CCircleGhostBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 法阵结构拓印卷轴 — 携带某等级法阵的结构图样，用于幽灵预览或一键搭建。
 *
 * <p>NBT：
 * <ul>
 *   <li><b>Tier</b> (int) — 卷轴记录的法阵 tier (1-5)</li>
 * </ul>
 *
 * <p>交互：
 * <ul>
 *   <li><b>右键空地或方块</b>：在被点击位置上方将选定 tier 的整个结构以幽灵方块形式预览
 *       (持续 200 tick)。结构基准为玩家右键的方块上方一格作为核心位置。</li>
 *   <li><b>Shift + 右键</b>：从玩家背包消耗对应方块一次性建造（生存模式）；创造模式免费。</li>
 * </ul>
 */
public class StructureBlueprintScrollItem extends Item {

    private static final String TAG_TIER = "Tier";

    /** Ghost 预览持续 tick。 */
    private static final int GHOST_TICKS = 200;

    public StructureBlueprintScrollItem() {
        super(new Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
        ModItems.ITEMS.add(this);
    }

    /** 创建一份指定 tier 的拓印卷轴。 */
    public static ItemStack of(int tier, Item self) {
        ItemStack stack = new ItemStack(self);
        stack.getOrCreateTag().putInt(TAG_TIER, Math.max(1, Math.min(5, tier)));
        return stack;
    }

    /** 读取卷轴记录的 tier (1-5)，默认 1。 */
    public static int getTier(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return 1;
        int t = tag.getInt(TAG_TIER);
        return t >= 1 && t <= 5 ? t : 1;
    }

    /** 直接绑定 tier 到任意 stack（用于刻印台合成结果）。 */
    public static void setTier(ItemStack stack, int tier) {
        stack.getOrCreateTag().putInt(TAG_TIER, Math.max(1, Math.min(5, tier)));
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;

        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

        ItemStack scroll = ctx.getItemInHand();
        int tier = getTier(scroll);
        CircleTier ct = CircleTier.fromLevel(tier);

        // 以点击方块的上方一格作为"核心位置"
        BlockPos corePos = ctx.getClickedPos().above();

        if (player.isShiftKeyDown()) {
            return autoBuild(serverLevel, (ServerPlayer) player, corePos, ct, scroll);
        } else {
            return showGhostPreview(serverLevel, (ServerPlayer) player, corePos, ct);
        }
    }

    /**
     * 普通右键：发送幽灵预览给玩家。
     */
    private InteractionResult showGhostPreview(ServerLevel level, ServerPlayer player,
                                                BlockPos corePos, CircleTier tier) {
        // 复用 chisel 的预览管道：把整个 tier 当作"待补全"列表发出去
        List<PatternEntry> entries = CircleStructurePattern.getPatternForTier(tier);
        var ghostEntries = new java.util.ArrayList<S2CCircleGhostBlocks.GhostEntry>(entries.size());
        for (PatternEntry e : entries) {
            BlockPos absolute = corePos.offset(e.dx(), e.dy(), e.dz());
            ghostEntries.add(new S2CCircleGhostBlocks.GhostEntry(absolute, e.role(), e.minBlockTier()));
        }
        // 也包含核心本身位置 (CORE 角色)
        ghostEntries.add(new S2CCircleGhostBlocks.GhostEntry(corePos, BlockRole.CORE, tier.getLevel()));

        NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new S2CCircleGhostBlocks(corePos, tier.getLevel(), GHOST_TICKS, ghostEntries));

        player.displayClientMessage(
                Component.translatable("msg.transcend.blueprint_scroll.preview",
                        Component.translatable(tier.getTranslationKey()))
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        level.playSound(null, corePos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 0.5F, 1.6F);
        return InteractionResult.CONSUME;
    }

    /**
     * Shift + 右键：从玩家背包消耗材料一键建造（含核心方块）。
     */
    private InteractionResult autoBuild(ServerLevel level, ServerPlayer player,
                                         BlockPos corePos, CircleTier tier, ItemStack scroll) {
        boolean creative = player.isCreative();
        List<PatternEntry> entries = CircleStructurePattern.getPatternForTier(tier);

        // 计算 to-place 与 conflicts
        Map<BlockPos, BlockState> toPlace = new HashMap<>();
        java.util.List<BlockPos> conflicts = new java.util.ArrayList<>();
        Map<String, Integer> requirements = new HashMap<>();

        // 核心方块本身
        BlockState coreState = CircleBlockPalette.stateFor(BlockRole.CORE, tier.getLevel());
        addPlacementCandidate(level, corePos, coreState, toPlace, conflicts, requirements);

        for (PatternEntry e : entries) {
            if (e.role() == BlockRole.CORE) continue;
            BlockPos target = corePos.offset(e.dx(), e.dy(), e.dz());
            BlockState desired = CircleBlockPalette.stateFor(e.role(), e.minBlockTier());
            if (desired == null) continue;
            addPlacementCandidate(level, target, desired, toPlace, conflicts, requirements);
        }

        if (!conflicts.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.architect_wand.conflict",
                            conflicts.size())
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        if (toPlace.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.architect_wand.already_built",
                            Component.translatable(tier.getTranslationKey()))
                            .withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.SUCCESS;
        }

        // 生存模式：检查并消耗
        if (!creative) {
            Map<String, Integer> shortfall = computeShortfall(player, requirements);
            if (!shortfall.isEmpty()) {
                int total = shortfall.values().stream().mapToInt(Integer::intValue).sum();
                player.displayClientMessage(
                        Component.translatable("msg.transcend.architect_wand.no_materials", total)
                                .withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
            consumeFromInventory(player, requirements);
        }

        for (Map.Entry<BlockPos, BlockState> e : toPlace.entrySet()) {
            level.setBlock(e.getKey(), e.getValue(), 3);
        }
        level.playSound(null, corePos, SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS, 1.0F, 1.4F);

        player.displayClientMessage(
                Component.translatable("msg.transcend.blueprint_scroll.built",
                        toPlace.size(), Component.translatable(tier.getTranslationKey()))
                        .withStyle(ChatFormatting.GREEN), false);

        // 卷轴消耗
        if (!creative) {
            scroll.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    private static void addPlacementCandidate(ServerLevel level, BlockPos pos, BlockState desired,
                                               Map<BlockPos, BlockState> toPlace,
                                               java.util.List<BlockPos> conflicts,
                                               Map<String, Integer> requirements) {
        if (desired == null) return;
        BlockState existing = level.getBlockState(pos);
        if (existing.getBlock() == desired.getBlock()) return;
        boolean canReplace = existing.isAir() || existing.canBeReplaced();
        if (!canReplace) {
            conflicts.add(pos);
            return;
        }
        toPlace.put(pos.immutable(), desired);
        String key = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(desired.getBlock()).toString();
        requirements.merge(key, 1, Integer::sum);
    }

    private static Map<String, Integer> computeShortfall(Player player, Map<String, Integer> req) {
        Map<String, Integer> shortfall = new HashMap<>();
        var inv = player.getInventory();
        for (var e : req.entrySet()) {
            int needed = e.getValue();
            int found = 0;
            for (ItemStack s : inv.items) {
                if (s.isEmpty()) continue;
                String key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.getItem()).toString();
                if (key.equals(e.getKey())) {
                    found += s.getCount();
                    if (found >= needed) break;
                }
            }
            if (found < needed) shortfall.put(e.getKey(), needed - found);
        }
        return shortfall;
    }

    private static void consumeFromInventory(Player player, Map<String, Integer> req) {
        var inv = player.getInventory();
        for (var e : req.entrySet()) {
            int remaining = e.getValue();
            for (int i = 0; i < inv.items.size() && remaining > 0; i++) {
                ItemStack s = inv.items.get(i);
                if (s.isEmpty()) continue;
                String key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.getItem()).toString();
                if (!key.equals(e.getKey())) continue;
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
            }
        }
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return getTier(stack) >= 3;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        int tier = getTier(stack);
        tooltip.add(Component.translatable("tooltip.transcend.blueprint_scroll.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.transcend.blueprint_scroll.tier",
                Component.translatable(CircleTier.fromLevel(tier).getTranslationKey()))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.transcend.blueprint_scroll.use_preview")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.transcend.blueprint_scroll.shift_use_build")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
