package com.huige233.transcend.circle.executor;

import com.huige233.transcend.block.circle.MagicCircleCoreBlockEntity;
import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 奥术增幅法阵功能执行器。 */
public class ArcaneAmplifierExecutor implements CircleFunctionExecutor {

    private static final int SCAN_RANGE = 32;

    private static final int SCAN_STEP = 8;

    private static final int CACHE_REFRESH_TICKS = 200;

    private static final int MAX_BOOSTED_CORES = 2;

    private static final int BOOST_PER_TICK = 1;

    private static class CacheEntry {
        List<BlockPos> targets = new ArrayList<>();
        int ticksUntilRefresh = 0;
    }

    private final Map<BlockPos, CacheEntry> caches = new HashMap<>();

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.ADEPT.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        caches.put(ctx.getCorePos().immutable(), new CacheEntry());
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        ServerLevel level = ctx.getLevel();
        if (level == null) {
            return;
        }

        BlockPos corePos = ctx.getCorePos().immutable();
        CacheEntry entry = caches.computeIfAbsent(corePos, k -> new CacheEntry());

        entry.ticksUntilRefresh--;
        if (entry.ticksUntilRefresh <= 0) {
            entry.targets = scanForCores(level, corePos);
            entry.ticksUntilRefresh = CACHE_REFRESH_TICKS;
        }

        if (entry.targets.isEmpty()) {
            return;
        }

        int boosted = 0;
        Iterator<BlockPos> it = entry.targets.iterator();
        while (it.hasNext() && boosted < MAX_BOOSTED_CORES) {
            BlockPos targetPos = it.next();
            BlockEntity be = level.getBlockEntity(targetPos);
            if (!(be instanceof MagicCircleCoreBlockEntity targetCore)) {
                it.remove();
                continue;
            }
            if (!targetCore.isActive()) {
                continue;
            }
            targetCore.insertMana(BOOST_PER_TICK);
            boosted++;
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        caches.remove(ctx.getCorePos().immutable());
    }

    private List<BlockPos> scanForCores(ServerLevel level, BlockPos corePos) {
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        for (int dx = -SCAN_RANGE; dx <= SCAN_RANGE; dx += SCAN_STEP) {
            for (int dz = -SCAN_RANGE; dz <= SCAN_RANGE; dz += SCAN_STEP) {
                BlockPos scanPos = corePos.offset(dx, 0, dz);
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos checkPos = scanPos.above(dy).immutable();
                    if (!seen.add(checkPos)) continue;
                    if (checkPos.equals(corePos)) continue;
                    BlockEntity be = level.getBlockEntity(checkPos);
                    if (be instanceof MagicCircleCoreBlockEntity target
                            && !target.getBlockPos().equals(corePos)) {
                        found.add(target.getBlockPos().immutable());
                    }
                }
            }
        }
        return found;
    }
}
