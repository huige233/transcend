package com.huige233.transcend.items;

import com.huige233.transcend.mana.IManaHandler;
import com.huige233.transcend.mana.ManaHandlerCapability;
import com.huige233.transcend.world.mana.ChunkManaSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 手持式 mana 探测器：右键带有 mana capability 的方块可读取其存量明细，
 * 玩家潜行 + 右键空气时改为扫描 {@link #SCAN_RADIUS} 球范围聚合统计。
 * 不消耗耐久，只读不写。
 */
public class ManaSensorItem extends Item {

    private static final int SCAN_RADIUS = 32;

    public ManaSensorItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    /**
     * 右键方块：若目标方块暴露 mana capability，向玩家发送单行 actionbar 详情；
     * 否则给出 "no mana data" 提示。客户端直接返回 SUCCESS 以避免重复触发。
     */
    @Override
    public @NotNull InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();

        // R61: ManaWell 不暴露 capability，但仍想看到 Resonance 状态 — 单独处理
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof com.huige233.transcend.block.ManaWellBlockEntity well && player != null) {
            float multCap = well.getCurrentMultiplierCap();
            int interval = well.getCurrentInterval();
            String resonanceTag = well.isResonance()
                    ? "§d§l✦ Resonance §r§7| "
                    : "§7";
            player.displayClientMessage(
                    Component.literal(String.format(
                            "§e[%d,%d,%d] §6Mana Well §7| %s上限 §b%.1f×§7, 间隔 §b%dt§7, %s",
                            pos.getX(), pos.getY(), pos.getZ(),
                            resonanceTag, multCap, interval,
                            well.isWorking() ? "§a运转中" : "§c停工")),
                    false);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.5F, 1.6F);
            return InteractionResult.CONSUME;
        }

        IManaHandler cap = readManaCapability(level, pos);
        if (cap == null) {
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.sensor.no_mana_here")
                                .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.CONSUME;
        }

        if (player != null) {
            int stored = cap.getManaStored();
            int max = cap.getMaxManaStored();
            int pct = max > 0 ? (int) (stored * 100.0 / max) : 0;
            String flow = (cap.canReceive() ? "§a↓" : "§7·") + (cap.canExtract() ? "§b↑" : "§7·");
            player.displayClientMessage(
                    Component.literal(String.format("§e[%d,%d,%d] §b%d§7/§b%d §e(%d%%) %s",
                            pos.getX(), pos.getY(), pos.getZ(), stored, max, pct, flow)),
                    false);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.5F, 1.6F);
        }
        return InteractionResult.CONSUME;
    }

    /**
     * 空手右键（包括潜行）。仅潜行触发网络扫描；普通点击仅给出操作提示。
     * 扫描结果汇总写入聊天框：节点数、总量/总容量、当前最大节点坐标与单点存量。
     */
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        if (!player.isShiftKeyDown()) {
            player.displayClientMessage(
                    Component.translatable("tooltip.transcend.sensor.usage_hint")
                            .withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.pass(stack);
        }

        // R56: 玩家所在区块的地脉分级
        if (level instanceof ServerLevel sl) {
            ChunkManaSavedData chunkData = ChunkManaSavedData.get(sl);
            ChunkPos cp = new ChunkPos(player.blockPosition());
            float chunkMana = chunkData.getMana(cp);
            ChunkManaSavedData.Tier tier = chunkData.getTier(cp);
            boolean stabilized = chunkData.isStabilized(cp);
            ChatFormatting tierColor = switch (tier) {
                case EXHAUSTED -> ChatFormatting.DARK_RED;
                case WEAK -> ChatFormatting.GOLD;
                case STABLE -> ChatFormatting.GREEN;
                case RICH -> ChatFormatting.AQUA;
            };
            String tierKey = "tier.transcend.chunk_mana." + tier.name().toLowerCase();
            player.displayClientMessage(
                    Component.literal("§7地脉: ")
                            .append(Component.translatable(tierKey).withStyle(tierColor))
                            .append(Component.literal(String.format(" §7(%.0f mana%s)",
                                    chunkMana, stabilized ? " §b★稳定器" : ""))), false);
        }

        NetworkScanResult scan = scanNetwork(level, player.blockPosition());
        if (scan.count == 0) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.sensor.no_network")
                            .withStyle(ChatFormatting.GRAY), true);
        } else {
            int avgPct = scan.totalMax > 0 ? (int) (scan.total * 100.0 / scan.totalMax) : 0;
            player.displayClientMessage(
                    Component.literal(String.format("§6§l网络: §b%d §7个节点 §7| §b%d§7/§b%d §6(%d%%)",
                            scan.count, scan.total, scan.totalMax, avgPct)), false);
            if (scan.maxPos != null) {
                player.displayClientMessage(
                        Component.literal(String.format("§e最大节点: §f[%d,%d,%d] §b%d mana",
                                scan.maxPos.getX(), scan.maxPos.getY(), scan.maxPos.getZ(), scan.maxIndividual)), false);
            }
            level.playSound(null, player.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.7F, 1.4F);
        }
        return InteractionResultHolder.consume(stack);
    }

    /** 读取目标坐标处方块实体的 mana capability；不是 BE 或不暴露 capability 时返回 null。 */
    @Nullable
    private static IManaHandler readManaCapability(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return null;
        return be.getCapability(ManaHandlerCapability.MANA_HANDLER).orElse(null);
    }

    /** 在以玩家为中心的 {@link #SCAN_RADIUS} 球范围内枚举所有 mana 节点，聚合返回总量与峰值节点。 */
    private static NetworkScanResult scanNetwork(Level level, BlockPos origin) {
        NetworkScanResult r = new NetworkScanResult();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        long r2 = (long) SCAN_RADIUS * SCAN_RADIUS;
        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dy = -SCAN_RADIUS; dy <= SCAN_RADIUS; dy++) {
                for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r2) continue;
                    m.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockEntity nb = level.getBlockEntity(m);
                    if (nb == null) continue;
                    IManaHandler cap = nb.getCapability(ManaHandlerCapability.MANA_HANDLER).orElse(null);
                    if (cap == null) continue;
                    r.count++;
                    r.total += cap.getManaStored();
                    r.totalMax += cap.getMaxManaStored();
                    if (cap.getManaStored() > r.maxIndividual) {
                        r.maxIndividual = cap.getManaStored();
                        r.maxPos = new BlockPos(m);
                    }
                }
            }
        }
        return r;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.transcend.sensor.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.transcend.sensor.usage_click").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.transcend.sensor.usage_shift").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** {@link #scanNetwork} 的聚合返回值。 */
    private static final class NetworkScanResult {
        int count = 0;
        int total = 0;
        int totalMax = 0;
        int maxIndividual = 0;
        @Nullable BlockPos maxPos = null;
    }
}
