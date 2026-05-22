package com.huige233.transcend.circle.executor;

import com.huige233.transcend.block.circle.MagicCircleCoreBlockEntity;
import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * 远程魔力链接（Remote Mana Link）功能执行器（v1 简化实现）。
 * <p>
 * 在 6 个朝向 × {8, 16, 32, 64} 距离扫描其它法环核心。命中的第一个、且储魔量低于
 * 本环的目标会被缓存，并以每次 tick 调用 2 CM 的速率注入。每 100 次 tick 重新扫描。
 */
public class RemoteManaLinkExecutor implements CircleFunctionExecutor {

    private static final int SCAN_INTERVAL = 100;
    private static final int[] SCAN_DISTANCES = {8, 16, 32, 64};
    private static final int TRANSFER_PER_TICK = 2;

    private final Map<BlockPos, Integer> tickCounters = new HashMap<>();
    private final Map<BlockPos, BlockPos> cachedTargets = new HashMap<>();

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.ADEPT.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        // 无需初始化
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        BlockPos key = ctx.getCorePos();
        int counter = tickCounters.getOrDefault(key, 0) + 1;

        if (counter >= SCAN_INTERVAL || !cachedTargets.containsKey(key)) {
            counter = 0;
            BlockPos target = scanForTarget(ctx);
            if (target != null) {
                cachedTargets.put(key.immutable(), target.immutable());
            } else {
                cachedTargets.remove(key);
            }
        }
        tickCounters.put(key.immutable(), counter);

        BlockPos target = cachedTargets.get(key);
        if (target == null) return;

        ServerLevel level = ctx.getLevel();
        BlockEntity be = level.getBlockEntity(target);
        if (!(be instanceof MagicCircleCoreBlockEntity targetCore)) {
            cachedTargets.remove(key);
            return;
        }

        int sourceMana = ctx.getStoredMana();
        int targetMana = targetCore.getStoredMana();
        if (sourceMana <= 0 || targetMana >= sourceMana) {
            return;
        }

        int toTransfer = Math.min(TRANSFER_PER_TICK, sourceMana - targetMana);
        toTransfer = Math.min(toTransfer, sourceMana);
        if (toTransfer <= 0) return;

        if (ctx.consumeMana(toTransfer)) {
            int accepted = targetCore.insertMana(toTransfer);
            // 如果对端拒收一部分，退还给本端（避免凭空消失）
            int rejected = toTransfer - accepted;
            if (rejected > 0) {
                ctx.insertMana(rejected);
            }
        }
    }

    /** 扫描 6 个方向各 4 个距离寻找首个储魔量低于本环的目标核心。 */
    private BlockPos scanForTarget(CircleFunctionContext ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos core = ctx.getCorePos();
        int myMana = ctx.getStoredMana();

        for (int dist : SCAN_DISTANCES) {
            for (Direction dir : Direction.values()) {
                BlockPos check = core.relative(dir, dist);
                BlockEntity be = level.getBlockEntity(check);
                if (be instanceof MagicCircleCoreBlockEntity other
                        && !check.equals(core)
                        && other.getStoredMana() < myMana) {
                    return check;
                }
            }
        }
        return null;
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        tickCounters.remove(ctx.getCorePos());
        cachedTargets.remove(ctx.getCorePos());
    }
}
