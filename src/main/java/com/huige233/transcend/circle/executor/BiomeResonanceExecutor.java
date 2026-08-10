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

/** 生物群系共鸣法阵功能执行器。 */
public class BiomeResonanceExecutor implements CircleFunctionExecutor {

    private static final int DETECT_INTERVAL_TICKS = 600;

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
        Component msg = Component.translatable("msg.transcend.circle.biome_resonance", biomeName);
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
