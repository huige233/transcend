package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NexusGatehouseExecutor implements CircleFunctionExecutor {

    private static final double AURA_RADIUS = 4.0;

    private static final int EFFECT_DURATION_TICKS = 100;

    private static final int UPKEEP_PER_TICK = 1;

    private static final int MESSAGE_INTERVAL_TICKS = 200;

    private final Map<BlockPos, Integer> messageTimers = new HashMap<>();

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.ARCHON.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        messageTimers.put(ctx.getCorePos().immutable(), 0);
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        ServerLevel level = ctx.getLevel();
        if (level == null) {
            return;
        }

        if (!ctx.consumeMana(UPKEEP_PER_TICK)) {
            return;
        }

        BlockPos corePos = ctx.getCorePos().immutable();
        AABB area = new AABB(
                corePos.getX() - AURA_RADIUS, corePos.getY() - 2, corePos.getZ() - AURA_RADIUS,
                corePos.getX() + AURA_RADIUS + 1, corePos.getY() + 4, corePos.getZ() + AURA_RADIUS + 1
        );
        List<Player> players = level.getEntitiesOfClass(Player.class, area);

        for (Player player : players) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.GLOWING, EFFECT_DURATION_TICKS, 0,
                    true, false, true));
            player.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE, EFFECT_DURATION_TICKS, 0,
                    true, false, true));
        }

        int timer = messageTimers.getOrDefault(corePos, 0) + 1;
        if (timer >= MESSAGE_INTERVAL_TICKS) {
            timer = 0;
            if (!players.isEmpty()) {
                Component msg = Component.translatable("msg.transcend.circle.nexus_gatehouse.charging")
                        .withStyle(ChatFormatting.LIGHT_PURPLE);
                for (Player player : players) {
                    player.sendSystemMessage(msg);
                }
            }
        }
        messageTimers.put(corePos, timer);
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        messageTimers.remove(ctx.getCorePos().immutable());
    }
}
