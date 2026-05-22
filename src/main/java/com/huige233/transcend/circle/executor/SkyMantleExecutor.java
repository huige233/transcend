package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 苍穹披风（Sky Mantle）功能执行器。
 * <p>
 * 在范围内的玩家可获得"创造模式式"飞行能力。当玩家离开作用范围或法环停用时，
 * 撤销其飞行能力，同时附加 6 秒缓降效果，防止从高空直接坠地。
 *
 * <p>额外消耗：每个正在受益的飞行玩家每分钟额外消耗 6 CM（按 tick 调用累积概率扣除）。
 *
 * <p>注意：本执行器只会回收"由它自己授予"的飞行权限。已经处于创造 / 冒险 / 旁观
 * 模式或由其它 mod 授予 mayfly 的玩家不在追踪集合内，不会被错误撤销。
 */
public class SkyMantleExecutor implements CircleFunctionExecutor {

    /** 缓降持续时间（tick）。 */
    private static final int SLOW_FALLING_DURATION_TICKS = 120;
    /** 每个飞行玩家每分钟附加消耗的 CM。 */
    private static final double CM_PER_FLYER_PER_MIN = 6.0;
    /** tick 调用频率：每 20 game tick 一次 ≈ 60 次/分钟。 */
    private static final double TICKS_PER_MIN = 60.0;

    /** 由本执行器授予过 mayfly 的玩家 UUID 集合。 */
    private final Set<UUID> grantedFlyers = new HashSet<>();

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.MASTER.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        grantedFlyers.clear();
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        ServerLevel level = ctx.getLevel();
        if (level == null) {
            return;
        }

        List<Player> inRadius = getPlayersInRadius(ctx);
        Set<UUID> currentInRadius = new HashSet<>();
        for (Player p : inRadius) {
            currentInRadius.add(p.getUUID());
        }

        // 1) 撤销：上一次被授予、但本次不在范围内的玩家
        List<UUID> toRevoke = new ArrayList<>();
        for (UUID uuid : grantedFlyers) {
            if (!currentInRadius.contains(uuid)) {
                toRevoke.add(uuid);
            }
        }
        for (UUID uuid : toRevoke) {
            Player player = level.getPlayerByUUID(uuid);
            revokeFlight(player);
            grantedFlyers.remove(uuid);
        }

        // 2) 授予：当前在范围内、尚未被授予的玩家
        for (Player player : inRadius) {
            if (player.isCreative() || player.isSpectator()) {
                // 创造 / 旁观本身就能飞，避免污染追踪集合
                continue;
            }
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                if (player instanceof ServerPlayer sp) {
                    sp.onUpdateAbilities();
                }
            }
            grantedFlyers.add(player.getUUID());
        }

        // 3) 按飞行玩家数量进行额外 CM 消耗（概率累积法）
        int flyerCount = grantedFlyers.size();
        if (flyerCount > 0) {
            double cmPerTickCall = (CM_PER_FLYER_PER_MIN * flyerCount) / TICKS_PER_MIN;
            int whole = (int) Math.floor(cmPerTickCall);
            double frac = cmPerTickCall - whole;
            if (level.getRandom().nextDouble() < frac) {
                whole += 1;
            }
            if (whole > 0) {
                // 若魔力不足，则强制撤销所有飞行能力（防止"白嫖"）
                if (!ctx.consumeMana(whole)) {
                    revokeAll(level);
                }
            }
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        ServerLevel level = ctx.getLevel();
        if (level == null) {
            grantedFlyers.clear();
            return;
        }
        revokeAll(level);
    }

    /** 撤销所有由本执行器授予的飞行能力，并附加缓降。 */
    private void revokeAll(ServerLevel level) {
        for (UUID uuid : grantedFlyers) {
            Player player = level.getPlayerByUUID(uuid);
            revokeFlight(player);
        }
        grantedFlyers.clear();
    }

    /** 对单个玩家撤销 mayfly，并施加缓降。 */
    private void revokeFlight(Player player) {
        if (player == null) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            // 其它来源的飞行能力，保持原状
            return;
        }
        if (player.getAbilities().mayfly) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            if (player instanceof ServerPlayer sp) {
                sp.onUpdateAbilities();
            }
        }
        player.addEffect(new MobEffectInstance(
                MobEffects.SLOW_FALLING,
                SLOW_FALLING_DURATION_TICKS,
                0,
                true,
                false,
                true
        ));
    }

    /** 在以核心方块为中心的范围内查找所有玩家。 */
    private List<Player> getPlayersInRadius(CircleFunctionContext ctx) {
        double r = ctx.getBaseRadius();
        BlockPos pos = ctx.getCorePos();
        AABB area = new AABB(
                pos.getX() - r, pos.getY() - 2, pos.getZ() - r,
                pos.getX() + r, pos.getY() + 4, pos.getZ() + r
        );
        return ctx.getLevel().getEntitiesOfClass(Player.class, area);
    }
}
