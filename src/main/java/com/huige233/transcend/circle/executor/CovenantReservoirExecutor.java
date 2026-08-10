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

/** 契约蓄水池法阵功能执行器。 */
public class CovenantReservoirExecutor implements CircleFunctionExecutor {

    private static final int RESCAN_INTERVAL_TICKS = 200;

    private static final int TRANSFER_PER_TICK = 1;

    private static final int MAX_LINKS = 4;

    private static final int[] SCAN_DISTANCES = { 16, 32, 64, 128 };

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

            if (ctx.consumeMana(TRANSFER_PER_TICK)) {
                int inserted = targetCore.insertMana(TRANSFER_PER_TICK);
                if (inserted < TRANSFER_PER_TICK) {

                    ctx.insertMana(TRANSFER_PER_TICK - inserted);
                }
            } else {

                break;
            }
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        caches.remove(ctx.getCorePos().immutable());
    }

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

    private static final class LinkCache {
        long lastScanTick = 0L;
        List<BlockPos> links = new ArrayList<>();
    }
}
