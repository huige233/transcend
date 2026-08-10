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

/** 远程魔力链接法阵功能执行器。 */
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

            int rejected = toTransfer - accepted;
            if (rejected > 0) {
                ctx.insertMana(rejected);
            }
        }
    }

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
