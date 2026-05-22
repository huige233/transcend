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
 * 葳蕤复苏（Verdant Restoration）功能执行器。
 * <p>
 * 为半径内的玩家持续提供生命回复，并周期性清理负面状态：
 * <ul>
 *     <li>每 tick 调用施加再生 I（T4+：再生 II），持续 60 tick。</li>
 *     <li>每 600 tick（≈30 秒）清除一次"中毒"效果。</li>
 *     <li>T5 起额外清除"凋零"效果。</li>
 * </ul>
 */
public class VerdantRestorationExecutor implements CircleFunctionExecutor {

    /** 再生持续时间（tick）。 */
    private static final int REGEN_DURATION_TICKS = 60;
    /** 清除负面状态的时间间隔（tick）。 */
    private static final int CLEANSE_INTERVAL_TICKS = 600;

    /** 内部计时器（按 tick 间隔自增）。 */
    private int cleanseTimer = 0;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.ADEPT.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        cleanseTimer = 0;
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        if (ctx.getLevel() == null) {
            return;
        }

        List<Player> players = getPlayersInRadius(ctx);
        if (players.isEmpty()) {
            // 仍要推进计时器，避免长时间空转后突然爆发清理
            cleanseTimer = Math.min(cleanseTimer + 20, CLEANSE_INTERVAL_TICKS);
            return;
        }

        int tier = ctx.getTier().getLevel();
        int regenAmplifier = tier >= CircleTier.ARCHON.getLevel() ? 1 : 0;
        boolean cleanseWither = tier >= CircleTier.PRIMORDIAL.getLevel();

        for (Player player : players) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.REGENERATION,
                    REGEN_DURATION_TICKS,
                    regenAmplifier,
                    true,
                    false,
                    true
            ));
        }

        // 计时器：tick 方法本身大约每 20 game tick 触发一次
        cleanseTimer += 20;
        if (cleanseTimer >= CLEANSE_INTERVAL_TICKS) {
            cleanseTimer = 0;
            for (Player player : players) {
                player.removeEffect(MobEffects.POISON);
                if (cleanseWither) {
                    player.removeEffect(MobEffects.WITHER);
                }
            }
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        cleanseTimer = 0;
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
