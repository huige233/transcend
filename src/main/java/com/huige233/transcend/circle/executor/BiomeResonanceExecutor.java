package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 群系共振（Biome Resonance）功能执行器。
 * <p>
 * 每 600 tick（30 秒）检测核心所在群系，并通过聊天向半径内玩家显示。
 */
public class BiomeResonanceExecutor implements CircleFunctionExecutor {

    private static final int DETECT_INTERVAL_TICKS = 600;

    /** 每个核心位置上次广播的世界时间。 */
    private final Map<BlockPos, Long> lastBroadcast = new HashMap<>();

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.MASTER.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        lastBroadcast.remove(ctx.getCorePos().immutable());
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        BlockPos selfPos = ctx.getCorePos().immutable();
        long gameTime = ctx.getLevel().getGameTime();
        Long last = lastBroadcast.get(selfPos);
        if (last != null && gameTime - last < DETECT_INTERVAL_TICKS) return;

        String biomeName = ctx.getLevel().getBiome(ctx.getCorePos())
                .unwrapKey()
                .map(k -> k.location().toString())
                .orElse("unknown");

        double radius = ctx.getBaseRadius();
        List<Player> players = ctx.getMobsInRadius(Player.class, radius);
        Component msg = Component.literal("§7[法环共鸣] 生态域: §b" + biomeName);
        for (Player player : players) {
            player.displayClientMessage(msg, false);
        }

        lastBroadcast.put(selfPos, gameTime);
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        lastBroadcast.remove(ctx.getCorePos().immutable());
    }
}
