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

/**
 * 枢纽门户（Nexus Gatehouse）功能执行器 — 维度门户预备态（T4+）。
 *
 * <p>v1 简化逻辑：
 * <ul>
 *     <li>对核心 4 格范围内的玩家施加发光 + 抗性 I 效果，模拟"门户能量场"。</li>
 *     <li>每 200 tick 在附近玩家聊天框中显示蓄能信息。</li>
 *     <li>每次 tick 消耗 1 CM 作为持续维持。</li>
 * </ul>
 * 完整的跨维度传送需要等待 {@code TranscendDimensions} 接入。
 */
public class NexusGatehouseExecutor implements CircleFunctionExecutor {

    /** 玩家增益范围（方块）。 */
    private static final double AURA_RADIUS = 4.0;
    /** 效果持续时间，略大于 tick 间隔。 */
    private static final int EFFECT_DURATION_TICKS = 100;
    /** 每次 tick 消耗的 CM。 */
    private static final int UPKEEP_PER_TICK = 1;
    /** 聊天提示的间隔（tick）。 */
    private static final int MESSAGE_INTERVAL_TICKS = 200;

    /** 每个核心的聊天提示计数器（singleton 执行器 → 按核心位置 keyed）。 */
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

        // 持续维持消耗
        if (!ctx.consumeMana(UPKEEP_PER_TICK)) {
            return;
        }

        BlockPos corePos = ctx.getCorePos().immutable();
        AABB area = new AABB(
                corePos.getX() - AURA_RADIUS, corePos.getY() - 2, corePos.getZ() - AURA_RADIUS,
                corePos.getX() + AURA_RADIUS + 1, corePos.getY() + 4, corePos.getZ() + AURA_RADIUS + 1
        );
        List<Player> players = level.getEntitiesOfClass(Player.class, area);

        // 施加门户能量场效果
        for (Player player : players) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.GLOWING, EFFECT_DURATION_TICKS, 0,
                    true, false, true));
            player.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE, EFFECT_DURATION_TICKS, 0,
                    true, false, true));
        }

        // 周期性聊天提示
        int timer = messageTimers.getOrDefault(corePos, 0) + 1;
        if (timer >= MESSAGE_INTERVAL_TICKS) {
            timer = 0;
            if (!players.isEmpty()) {
                Component msg = Component.literal("§7[枢纽门庭] §d门户蓄能中...")
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
