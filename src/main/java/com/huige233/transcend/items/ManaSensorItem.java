package com.huige233.transcend.items;

import com.huige233.transcend.mana.IManaHandler;
import com.huige233.transcend.mana.ManaHandlerCapability;
import com.huige233.transcend.world.mana.ChunkManaSavedData;
import com.huige233.transcend.world.mana.ChunkManaObservation;
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

public class ManaSensorItem extends Item {

    private static final int SCAN_RADIUS = 32;

    public ManaSensorItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof com.huige233.transcend.block.ManaWellBlockEntity well && player != null) {
            float multCap = well.getCurrentMultiplierCap();
            int interval = well.getCurrentInterval();
            player.displayClientMessage(
                    Component.translatable("msg.transcend.sensor.well", pos.getX(), pos.getY(), pos.getZ(),
                            Component.translatable(well.isResonance()
                                    ? "msg.transcend.sensor.resonance" : "msg.transcend.sensor.normal"),
                            multCap, interval, Component.translatable(well.isWorking()
                                    ? "msg.transcend.sensor.working" : "msg.transcend.sensor.stopped")),
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
                    Component.translatable("msg.transcend.sensor.node", pos.getX(), pos.getY(), pos.getZ(),
                            stored, max, pct, flow),
                    false);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.5F, 1.6F);
        }
        return InteractionResult.CONSUME;
    }

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

        if (level instanceof ServerLevel sl) {
            ChunkPos cp = new ChunkPos(player.blockPosition());
            ChunkManaObservation.Sample observation = ChunkManaObservation.observe(sl, cp);
            if (!observation.known()) {
                player.displayClientMessage(Component.literal("[Leyline] UNKNOWN")
                        .withStyle(ChatFormatting.GRAY), false);
            } else {
            float chunkMana = observation.mana().orElseThrow();
            ChunkManaSavedData.Tier tier = observation.tier();
            boolean stabilized = observation.stabilized();
            ChatFormatting tierColor = switch (tier) {
                case EXHAUSTED -> ChatFormatting.DARK_RED;
                case WEAK -> ChatFormatting.GOLD;
                case STABLE -> ChatFormatting.GREEN;
                case RICH -> ChatFormatting.AQUA;
            };
            String tierKey = "tier.transcend.chunk_mana." + tier.name().toLowerCase();
            player.displayClientMessage(
                    Component.translatable("msg.transcend.sensor.leyline",
                            Component.translatable(tierKey).withStyle(tierColor), chunkMana,
                            Component.translatable(stabilized
                                    ? "msg.transcend.sensor.stabilized" : "msg.transcend.sensor.unstabilized")), false);
            }
        }

        NetworkScanResult scan = scanNetwork(level, player.blockPosition());
        if (scan.count == 0) {
            player.displayClientMessage(
                    Component.translatable("msg.transcend.sensor.no_network")
                            .withStyle(ChatFormatting.GRAY), true);
        } else {
            int avgPct = scan.totalMax > 0 ? (int) (scan.total * 100.0 / scan.totalMax) : 0;
            player.displayClientMessage(
                    Component.translatable("msg.transcend.sensor.network",
                            scan.count, scan.total, scan.totalMax, avgPct), false);
            if (scan.maxPos != null) {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.sensor.largest_node",
                                scan.maxPos.getX(), scan.maxPos.getY(), scan.maxPos.getZ(), scan.maxIndividual), false);
            }
            level.playSound(null, player.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.7F, 1.4F);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Nullable
    private static IManaHandler readManaCapability(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return null;
        return be.getCapability(ManaHandlerCapability.MANA_HANDLER).orElse(null);
    }

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

    private static final class NetworkScanResult {
        int count = 0;
        int total = 0;
        int totalMax = 0;
        int maxIndividual = 0;
        @Nullable BlockPos maxPos = null;
    }
}
