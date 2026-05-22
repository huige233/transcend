package com.huige233.transcend.circle.scroll;

import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.network.S2COreRevealPack;
import com.huige233.transcend.network.S2COreRevealPack.OreEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * 地脉照骨 — 矿石透视。
 *
 * <p>对玩家施加短暂夜视(辅助看预览)，
 * 同时扫描 24 格球形范围内所有"原版矿石"标签方块，
 * 通过 {@link S2COreRevealPack} 让客户端在指定时长高亮。
 *
 * <p>颜色按矿石类型映射，未知矿石用品红色 fallback。
 */
public class OrebloodRevelationEffect implements ScrollEffect {

    /** 视效持续 tick 数（默认 30s — 由 BalanceConfig 控制） */
    private static int durationTicks() {
        return com.huige233.transcend.balance.BalanceConfig.get().scroll.oreblood_duration;
    }

    /** 扫描半径（方块） */
    private static final int RADIUS = 24;

    /** 单次最多高亮 1024 个矿石（受 S2C 包上限约束） */
    private static final int MAX_ORES = 1024;

    @Override
    public boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos) {
        int dur = durationTicks();
        // Round 40: shader 持续整个 buff 时长（用户要求 — 之前 60t 太短）
        com.huige233.transcend.circle.scroll.ScrollVisualHelper.circle(
                level, pos, RADIUS, 0.95F, 0.6F, 0.2F, dur, "hexagram");
        com.huige233.transcend.circle.scroll.ScrollVisualHelper.shieldRipple(
                level, pos, RADIUS * 0.5F, 0.95F, 0.6F, 0.2F, dur);

        // 玩家辅助：夜视 + 短时发光使玩家在阴暗环境也能看到自己
        caster.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,
                dur, 0, false, true));

        // 扫描矿石
        List<OreEntry> entries = scanOres(level, pos);
        if (entries.isEmpty()) {
            // 没找到矿石也算成功（避免玩家在贫瘠区域反复消耗），但仅给视觉效果
            return true;
        }

        // 推送 S2C 包给所有跟踪本区块的玩家（也含施法者本人）
        NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> caster),
                new S2COreRevealPack(pos, dur, entries));

        return true;
    }

    @Override
    public int getManaCost() {
        return com.huige233.transcend.balance.BalanceConfig.get().scroll.oreblood_revelation_cost;
    }

    @Override
    public int getDuration() {
        return 0;
    }

    // ============================================================
    // 扫描逻辑
    // ============================================================

    private static List<OreEntry> scanOres(ServerLevel level, BlockPos center) {
        List<OreEntry> hits = new ArrayList<>();
        int r = RADIUS;
        int rSq = r * r;

        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int dxSq = dx * dx + dz * dz;
                if (dxSq > rSq) continue;
                for (int dy = -r; dy <= r; dy++) {
                    if (dxSq + dy * dy > rSq) continue;

                    mp.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = level.getBlockState(mp);
                    if (!isOre(state)) continue;

                    int color = oreColor(state);
                    hits.add(new OreEntry(mp.immutable(), color));

                    if (hits.size() >= MAX_ORES) {
                        return hits;
                    }
                }
            }
        }
        return hits;
    }

    /** 判断方块是否是矿石（基于原版 tag）。 */
    private static boolean isOre(BlockState state) {
        // 原版的 #minecraft:c/ores 不存在，但 forge:ores 存在
        // 走 BlockTags 安全方式：依赖原版独立 tag
        return state.is(BlockTags.COAL_ORES)
                || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.DIAMOND_ORES)
                || state.is(BlockTags.EMERALD_ORES)
                || state.is(BlockTags.LAPIS_ORES)
                || state.is(BlockTags.REDSTONE_ORES)
                || state.is(BlockTags.COPPER_ORES);
    }

    /** 矿石→颜色映射（24-bit RGB）。 */
    private static int oreColor(BlockState state) {
        if (state.is(BlockTags.DIAMOND_ORES))   return 0x29DDE6; // 钻石青
        if (state.is(BlockTags.EMERALD_ORES))   return 0x39C25C; // 翡翠绿
        if (state.is(BlockTags.GOLD_ORES))      return 0xFFE34A; // 金黄
        if (state.is(BlockTags.LAPIS_ORES))     return 0x3D72E6; // 青金蓝
        if (state.is(BlockTags.REDSTONE_ORES))  return 0xE5251F; // 红石红
        if (state.is(BlockTags.COPPER_ORES))    return 0xE07A41; // 铜橙
        if (state.is(BlockTags.IRON_ORES))      return 0xC8AA8A; // 铁黄褐
        if (state.is(BlockTags.COAL_ORES))      return 0x222222; // 煤黑
        return 0xFF55FF; // 未知矿石：品红 fallback
    }
}
