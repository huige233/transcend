package com.huige233.transcend.circle.executor;

import com.huige233.transcend.block.circle.MagicCircleCoreBlockEntity;
import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 盟约蓄池（Covenant Reservoir）功能执行器。
 * <p>
 * 在 128 格范围内扫描其他法环核心方块实体，定期把魔力转移给储量低于半容量的目标核心。
 * 最多链接 4 个目标核心。
 */
public class CovenantReservoirExecutor implements CircleFunctionExecutor {

    /** 链接缓存刷新周期（ticks） */
    private static final int RESCAN_INTERVAL_TICKS = 200;
    /** 单次 tick 转移的魔力量 */
    private static final int TRANSFER_PER_TICK = 1;
    /** 最大链接数 */
    private static final int MAX_LINKS = 4;
    /** 扫描距离（按方向逐级探查） */
    private static final int[] SCAN_DISTANCES = { 16, 32, 64, 128 };

    /** 每个核心位置缓存的链接信息（执行器是单例，必须按 corePos 区分） */
    private final Map<BlockPos, LinkCache> caches = new HashMap<>();

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.MASTER.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        caches.remove(ctx.getCorePos().immutable());
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos selfPos = ctx.getCorePos().immutable();
        long gameTime = level.getGameTime();

        LinkCache cache = caches.computeIfAbsent(selfPos, p -> new LinkCache());

        if (cache.lastScanTick == 0L || gameTime - cache.lastScanTick >= RESCAN_INTERVAL_TICKS) {
            cache.links = scanForLinks(level, selfPos);
            cache.lastScanTick = gameTime;
        }

        if (cache.links.isEmpty()) return;

        Iterator<BlockPos> it = cache.links.iterator();
        while (it.hasNext()) {
            BlockPos targetPos = it.next();
            BlockEntity be = level.getBlockEntity(targetPos);
            if (!(be instanceof MagicCircleCoreBlockEntity targetCore)) {
                it.remove();
                continue;
            }
            int targetMax = targetCore.getMaxMana();
            if (targetMax <= 0) continue;
            if (targetCore.getStoredMana() >= targetMax / 2) continue;

            // 先尝试从自己消耗，再注入对方
            if (ctx.consumeMana(TRANSFER_PER_TICK)) {
                int inserted = targetCore.insertMana(TRANSFER_PER_TICK);
                if (inserted < TRANSFER_PER_TICK) {
                    // 对方没接收完 → 退回剩余给自己
                    ctx.insertMana(TRANSFER_PER_TICK - inserted);
                }
            } else {
                // 自己魔力不足，本 tick 直接放弃后续
                break;
            }
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        caches.remove(ctx.getCorePos().immutable());
    }

    /** 在 6 个方向、4 个距离上扫描其他法环核心，最多 {@link #MAX_LINKS} 个。 */
    private List<BlockPos> scanForLinks(ServerLevel level, BlockPos selfPos) {
        List<BlockPos> found = new ArrayList<>(MAX_LINKS);
        for (Direction dir : Direction.values()) {
            for (int dist : SCAN_DISTANCES) {
                if (found.size() >= MAX_LINKS) return found;
                BlockPos check = selfPos.relative(dir, dist);
                BlockEntity be = level.getBlockEntity(check);
                if (be instanceof MagicCircleCoreBlockEntity && !check.equals(selfPos)) {
                    BlockPos imm = check.immutable();
                    if (!found.contains(imm)) found.add(imm);
                }
            }
        }
        return found;
    }

    /** 链接缓存数据。 */
    private static final class LinkCache {
        long lastScanTick = 0L;
        List<BlockPos> links = new ArrayList<>();
    }
}
