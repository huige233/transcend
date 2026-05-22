package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 炉心安宁（Hearth Stability）功能执行器。
 * <p>
 * 在范围内提供两类防护：
 * <ul>
 *     <li>每 20 tick 扫描一次半径内的火焰方块（{@link Blocks#FIRE}），并将其熄灭。</li>
 *     <li>每次 tick 调用为半径内的玩家施加抗性提升 I（40 tick）。</li>
 * </ul>
 *
 * <p>为避免一次性扫描过大体积，火焰扫描在垂直方向上限制在核心方块 ±4 的范围内。
 */
public class HearthStabilityExecutor implements CircleFunctionExecutor {

    /** 抗性持续时间（tick）。 */
    private static final int RESISTANCE_DURATION_TICKS = 40;
    /** 扫描垂直高度的上下半径。 */
    private static final int FIRE_SCAN_VERTICAL = 4;
    /** 单次扫描熄灭火焰数量上限，避免大半径下卡服。 */
    private static final int MAX_EXTINGUISH_PER_PULSE = 64;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.ADEPT.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        // 无需特殊处理
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        ServerLevel level = ctx.getLevel();
        if (level == null) {
            return;
        }

        // 1) 给玩家施加抗性 I
        List<Player> players = getPlayersInRadius(ctx);
        for (Player player : players) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE,
                    RESISTANCE_DURATION_TICKS,
                    0,
                    true,
                    false,
                    true
            ));
        }

        // 2) 扫描并熄灭火焰方块
        extinguishFires(ctx, level);
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        // 无需特殊处理
    }

    /** 在 base 半径内扫描火焰方块并移除。 */
    private void extinguishFires(CircleFunctionContext ctx, ServerLevel level) {
        int radius = (int) ctx.getBaseRadius();
        if (radius <= 0) {
            return;
        }

        BlockPos core = ctx.getCorePos();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int extinguished = 0;

        // 简单立方体扫描；后续可改为球形或随机采样
        for (int dy = -FIRE_SCAN_VERTICAL; dy <= FIRE_SCAN_VERTICAL && extinguished < MAX_EXTINGUISH_PER_PULSE; dy++) {
            for (int dx = -radius; dx <= radius && extinguished < MAX_EXTINGUISH_PER_PULSE; dx++) {
                for (int dz = -radius; dz <= radius && extinguished < MAX_EXTINGUISH_PER_PULSE; dz++) {
                    cursor.set(core.getX() + dx, core.getY() + dy, core.getZ() + dz);
                    if (level.getBlockState(cursor).is(Blocks.FIRE)) {
                        level.removeBlock(cursor, false);
                        extinguished++;
                    }
                }
            }
        }
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
