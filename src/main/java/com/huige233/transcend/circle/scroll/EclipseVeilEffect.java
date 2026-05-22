package com.huige233.transcend.circle.scroll;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * 日蚀帷幕 — 30格半径，玩家获得隐身600tick，敌对生物获得失明200tick。
 */
public class EclipseVeilEffect implements ScrollEffect {

    private static final int RADIUS = 30;
    private static final int INVIS_DURATION = 600;
    private static final int BLIND_DURATION = 200;

    @Override
    public boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos) {
        // Round 39: 暗紫法环 + 黑色涟漪（日蚀帷幕）
        ScrollVisualHelper.circle(level, pos, RADIUS, 0.2F, 0.0F, 0.4F, 400, "pentagram");
        ScrollVisualHelper.shieldRipple(level, pos, RADIUS, 0.15F, 0.05F, 0.3F, 240);

        List<Player> players = level.getEntitiesOfClass(
                Player.class, ScrollEffectUtil.radiusBox(pos, RADIUS), p -> true);
        for (Player player : players) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, INVIS_DURATION, 0, false, true));
        }

        List<LivingEntity> hostiles = level.getEntitiesOfClass(
                LivingEntity.class, ScrollEffectUtil.radiusBox(pos, RADIUS),
                ScrollEffectUtil::isHostile);
        for (LivingEntity target : hostiles) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLIND_DURATION, 0, false, true));
        }

        return true;
    }

    @Override
    public int getManaCost() {
        return com.huige233.transcend.balance.BalanceConfig.get().scroll.eclipse_veil_cost;
    }

    @Override
    public int getDuration() {
        return 0;
    }
}
