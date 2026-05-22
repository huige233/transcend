package com.huige233.transcend.circle.scroll;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * 虚空放逐令 — 24格半径，秒杀非Boss敌对，Boss受30虚空伤害+虚弱III 200tick。
 */
public class VoidExileMandateEffect implements ScrollEffect {

    private static final int RADIUS = 24;
    private static final float BOSS_DAMAGE = 30.0F;
    private static final int BOSS_WEAKNESS_DURATION = 200;
    private static final int BOSS_WEAKNESS_AMP = 2; // III

    @Override
    public boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos) {
        // Round 39: 黑紫色虚空冲击 + pentagram 法环（虚空放逐）
        ScrollVisualHelper.shockwave(level, pos, RADIUS, 0.4F, 0.1F, 0.6F, 160);
        ScrollVisualHelper.circle(level, pos, RADIUS * 0.6F, 0.55F, 0.1F, 0.65F, 320, "pentagram");

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, ScrollEffectUtil.radiusBox(pos, RADIUS),
                ScrollEffectUtil::isHostile);
        for (LivingEntity target : targets) {
            // Round 41: 紫黑光柱降临 — 虚空放逐令的窥视感
            com.huige233.transcend.circle.scroll.ScrollVisualHelper.pillarFromSky(level,
                    new net.minecraft.world.phys.Vec3(target.getX(), target.getY(), target.getZ()),
                    32.0F, 0.55F, 0.1F, 0.65F, 60);
            if (ScrollEffectUtil.isBoss(target)) {
                // 虚空伤害（绕过护甲）
                target.hurt(level.damageSources().magic(), BOSS_DAMAGE);
                target.addEffect(new MobEffectInstance(
                        MobEffects.WEAKNESS, BOSS_WEAKNESS_DURATION, BOSS_WEAKNESS_AMP, false, true));
            } else {
                target.kill();
            }
        }
        return true;
    }

    @Override
    public int getManaCost() {
        return com.huige233.transcend.balance.BalanceConfig.get().scroll.void_exile_mandate_cost;
    }

    @Override
    public int getDuration() {
        return 0;
    }
}
