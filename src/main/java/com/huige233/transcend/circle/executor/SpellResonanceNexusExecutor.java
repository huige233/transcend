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
 * 法术共振枢纽（Spell Resonance Nexus）功能执行器。
 * <p>
 * 标记半径内的玩家为"法术增幅"状态。在 v1 中，仅为玩家施加力量 I（40 tick）
 * 作为可观察的增益占位；后续版本将通过 capability / 数据 tag 注入"法术伤害加成"
 * 字段，并在 mod 的法术伤害结算路径上读取该字段进行加成。
 *
 * <p>TODO（后续版本）：
 * <ul>
 *     <li>定义并附加 SpellBoostCapability（每玩家），在 tick 中刷新到位。</li>
 *     <li>在法术伤害事件 / 法术结算钩子中读取该 capability，按系数提升伤害。</li>
 *     <li>把力量 I 改为可选项或仅在没有法术系统加载时作为兜底显示。</li>
 * </ul>
 */
public class SpellResonanceNexusExecutor implements CircleFunctionExecutor {

    /** 占位增益的持续时间（tick）。 */
    private static final int BUFF_DURATION_TICKS = 40;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.MASTER.getLevel();
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

        for (Player player : players) {
            // v1 占位增益：力量 I。后续将由 SpellBoostCapability 接管。
            player.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_BOOST,
                    BUFF_DURATION_TICKS,
                    0,
                    true,
                    false,
                    true
            ));
            // TODO: SpellBoost.get(player).setBonus(0.20f, BUFF_DURATION_TICKS);
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        // 占位增益自然消散；后续版本需在此清理 capability 状态
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
