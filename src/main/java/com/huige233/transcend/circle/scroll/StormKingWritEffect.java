package com.huige233.transcend.circle.scroll;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.List;

/**
 * 雷霆王诏 — 召唤雷暴 6000tick，并在32格内12个随机敌对生物处召唤闪电。
 */
public class StormKingWritEffect implements ScrollEffect {

    private static final int WEATHER_DURATION = 6000;
    private static final int LIGHTNING_RADIUS = 32;
    private static final int LIGHTNING_COUNT = 12;

    @Override
    public boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos) {
        // Round 39: 黄色雷霆法环 + 紫蓝冲击波（雷暴召唤）
        ScrollVisualHelper.circle(level, pos, LIGHTNING_RADIUS, 1.0F, 0.95F, 0.4F, 400, "pentagram");
        ScrollVisualHelper.shockwave(level, pos, LIGHTNING_RADIUS, 0.7F, 0.7F, 1.0F, 120);

        // 设置雷暴
        level.setWeatherParameters(0, WEATHER_DURATION, true, true);

        // 召唤闪电到随机敌对生物
        List<LivingEntity> hostiles = level.getEntitiesOfClass(
                LivingEntity.class, ScrollEffectUtil.radiusBox(pos, LIGHTNING_RADIUS),
                ScrollEffectUtil::isHostile);
        Collections.shuffle(hostiles);
        int count = Math.min(LIGHTNING_COUNT, hostiles.size());
        for (int i = 0; i < count; i++) {
            LivingEntity target = hostiles.get(i);
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(target.getX(), target.getY(), target.getZ());
                level.addFreshEntity(bolt);
            }
        }
        return true;
    }

    @Override
    public int getManaCost() {
        return com.huige233.transcend.balance.BalanceConfig.get().scroll.storm_king_writ_cost;
    }

    @Override
    public int getDuration() {
        return 0;
    }
}
