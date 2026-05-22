package com.huige233.transcend.circle.scroll;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.List;

/**
 * 黑日归零 — 40格内最多12只敌对生物各受40魔法伤害，施法者获得 凋零II 1400t。
 */
public class ForbiddenBlackSunEffect implements ScrollEffect {

    private static final int RADIUS = 40;
    private static final int MAX_TARGETS = 12;
    private static final float DAMAGE = 40.0F;
    private static final float BOSS_DAMAGE = 20.0F;
    private static final int WITHER_DURATION = 1400;
    private static final int WITHER_AMP = 1; // II

    @Override
    public boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos) {
        // Round 39: 禁咒级 - 黑色 pentagram + 紫色超大冲击波 + 涟漪（黑日归零）
        com.huige233.transcend.circle.scroll.ScrollVisualHelper.circle(level, pos, RADIUS * 0.4F, 0.1F, 0.0F, 0.1F, 400, "pentagram");
        com.huige233.transcend.circle.scroll.ScrollVisualHelper.shockwave(level, pos, RADIUS, 0.3F, 0.0F, 0.4F, 400);
        com.huige233.transcend.circle.scroll.ScrollVisualHelper.shieldRipple(level, pos, RADIUS * 0.6F, 0.5F, 0.0F, 0.5F, 400);

        List<LivingEntity> hostiles = level.getEntitiesOfClass(
                LivingEntity.class, ScrollEffectUtil.radiusBox(pos, RADIUS),
                ScrollEffectUtil::isHostile);
        Collections.shuffle(hostiles);
        int count = Math.min(MAX_TARGETS, hostiles.size());
        for (int i = 0; i < count; i++) {
            LivingEntity target = hostiles.get(i);
            // Round 41: 每目标降下黑紫光柱 — 黑日归零的诛戮视觉
            com.huige233.transcend.circle.scroll.ScrollVisualHelper.pillarFromSky(level,
                    new net.minecraft.world.phys.Vec3(target.getX(), target.getY(), target.getZ()),
                    40.0F, 0.5F, 0.0F, 0.5F, 80);
            float dmg = ScrollEffectUtil.isBoss(target) ? BOSS_DAMAGE : DAMAGE;
            target.hurt(level.damageSources().magic(), dmg);
        }
        caster.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_DURATION, WITHER_AMP, false, true));
        return true;
    }

    @Override
    public int getManaCost() {
        return com.huige233.transcend.balance.BalanceConfig.get().scroll.forbidden_black_sun_cost;
    }

    @Override
    public int getDuration() {
        return 0;
    }
}
