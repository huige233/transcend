package com.huige233.transcend.circle.scroll;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * 忘路迷雾 — 48格半径敌对生物施加失明200t + 缓慢III 200t，施法者隐身600t。
 */
public class UnrememberedFogEffect implements ScrollEffect {

    private static final int RADIUS = 48;
    private static final int DEBUFF_DURATION = 200;
    private static final int SLOW_AMP = 2; // III
    private static final int INVIS_DURATION = 600;

    @Override
    public boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos) {
        // Round 39: 暗色雾气涟漪 + pentagram（忘路迷雾）
        com.huige233.transcend.circle.scroll.ScrollVisualHelper.shieldRipple(level, pos, RADIUS, 0.3F, 0.25F, 0.4F, 400);
        com.huige233.transcend.circle.scroll.ScrollVisualHelper.circle(level, pos, RADIUS * 0.5F, 0.4F, 0.3F, 0.55F, 400, "pentagram");

        List<LivingEntity> hostiles = level.getEntitiesOfClass(
                LivingEntity.class, ScrollEffectUtil.radiusBox(pos, RADIUS),
                ScrollEffectUtil::isHostile);
        for (LivingEntity target : hostiles) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, DEBUFF_DURATION, 0, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DEBUFF_DURATION, SLOW_AMP, false, true));
        }
        caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, INVIS_DURATION, 0, false, true));
        return true;
    }

    @Override
    public int getManaCost() {
        return com.huige233.transcend.balance.BalanceConfig.get().scroll.unremembered_fog_cost;
    }

    @Override
    public int getDuration() {
        return 0;
    }
}
