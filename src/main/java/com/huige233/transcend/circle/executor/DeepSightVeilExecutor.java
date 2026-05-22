package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 深渊洞察之纱（Deep Sight Veil）功能执行器。
 * <p>
 * 为半径内的玩家施加夜视与水下呼吸，便于探索黑暗与水下环境。
 * T3 起额外赋予潮涌能量，增强水下视野 / 挖掘速度并能在水下攻击。
 */
public class DeepSightVeilExecutor implements CircleFunctionExecutor {

    /** buff 持续时间（tick）。 */
    private static final int BUFF_DURATION_TICKS = 80;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.INITIATE.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        // 无需特殊处理
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        if (ctx.getLevel() == null) {
            return;
        }

        List<Player> players = getPlayersInRadius(ctx);
        if (players.isEmpty()) {
            return;
        }

        boolean grantConduit = ctx.getTier().getLevel() >= CircleTier.MASTER.getLevel();
        for (Player player : players) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION,
                    BUFF_DURATION_TICKS,
                    0,
                    true,
                    false,
                    true
            ));
            player.addEffect(new MobEffectInstance(
                    MobEffects.WATER_BREATHING,
                    BUFF_DURATION_TICKS,
                    0,
                    true,
                    false,
                    true
            ));
            if (grantConduit) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.CONDUIT_POWER,
                        BUFF_DURATION_TICKS,
                        0,
                        true,
                        false,
                        true
                ));
            }
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        // 效果自然消散
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
