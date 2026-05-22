package com.huige233.transcend.circle.scroll;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * 至尊护盾 — 16格半径，对所有玩家施加 抗性提升IV + 防火 300tick。
 */
public class SovereignAegisEffect implements ScrollEffect {

    private static final int RADIUS = 16;
    private static final int RESISTANCE_AMP = 3; // IV
    private static final int FIRE_RES_AMP = 0;

    @Override
    public boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos) {
        int duration = com.huige233.transcend.balance.BalanceConfig.get().scroll.sovereign_aegis_duration;
        // Round 39: 金白色护盾涟漪 + 神圣法环
        ScrollVisualHelper.shieldRipple(level, pos, RADIUS, 1.0F, 0.95F, 0.6F, 240);
        ScrollVisualHelper.circle(level, pos, RADIUS * 0.8F, 1.0F, 0.9F, 0.5F, 400, "hexagram");

        List<Player> players = level.getEntitiesOfClass(
                Player.class, ScrollEffectUtil.radiusBox(pos, RADIUS), p -> true);
        for (Player player : players) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, RESISTANCE_AMP, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, FIRE_RES_AMP, false, true));
        }
        return true;
    }

    @Override
    public int getManaCost() {
        return com.huige233.transcend.balance.BalanceConfig.get().scroll.sovereign_aegis_cost;
    }

    @Override
    public int getDuration() {
        return 0;
    }
}
