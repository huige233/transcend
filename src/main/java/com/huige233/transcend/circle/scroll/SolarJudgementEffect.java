package com.huige233.transcend.circle.scroll;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** 日曜审判卷轴效果实现。 */
public class SolarJudgementEffect implements ScrollEffect {

    private static final int RADIUS = 14;
    private static final float DAMAGE = 20.0F;
    private static final int IGNITE_SECONDS = 8;

    @Override
    public boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos) {

        ScrollVisualHelper.circle(level, pos, RADIUS, 1.0F, 0.85F, 0.2F, 240, "hexagram");
        ScrollVisualHelper.shockwave(level, pos, RADIUS, 1.0F, 0.95F, 0.3F, 120);

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, ScrollEffectUtil.radiusBox(pos, RADIUS),
                ScrollEffectUtil::isHostile);
        for (LivingEntity target : targets) {

            ScrollVisualHelper.pillarFromSky(level,
                    new net.minecraft.world.phys.Vec3(target.getX(), target.getY(), target.getZ()),
                    32.0F, 1.0F, 0.95F, 0.2F, 60);

            target.hurt(level.damageSources().magic(), DAMAGE);
            if (ScrollEffectUtil.isUndead(target)) {
                target.setSecondsOnFire(IGNITE_SECONDS);
            }
        }
        return true;
    }

    @Override
    public int getManaCost() {
        return com.huige233.transcend.balance.BalanceConfig.get().scroll.solar_judgement_cost;
    }

    @Override
    public int getDuration() {
        return 0;
    }
}
