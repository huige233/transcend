package com.huige233.transcend.circle.scroll;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.List;

public class EighteenfoldDragonEffect implements ScrollEffect {

    private static final int RADIUS = 20;
    private static final int MAX_TARGETS = 20;
    private static final float DAMAGE = 12.0F;

    @Override
    public boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos) {

        com.huige233.transcend.circle.scroll.ScrollVisualHelper.circle(level, pos, RADIUS, 0.95F, 0.95F, 1.0F, 400, "hexagram");
        com.huige233.transcend.circle.scroll.ScrollVisualHelper.shockwave(level, pos, RADIUS, 1.0F, 1.0F, 1.0F, 400);

        List<LivingEntity> hostiles = level.getEntitiesOfClass(
                LivingEntity.class, ScrollEffectUtil.radiusBox(pos, RADIUS),
                ScrollEffectUtil::isHostile);
        Collections.shuffle(hostiles);
        int count = Math.min(MAX_TARGETS, hostiles.size());
        for (int i = 0; i < count; i++) {
            LivingEntity victim = hostiles.get(i);

            com.huige233.transcend.circle.scroll.ScrollVisualHelper.pillarFromSky(level,
                    new net.minecraft.world.phys.Vec3(victim.getX(), victim.getY(), victim.getZ()),
                    24.0F, 0.95F, 0.95F, 1.0F, 50);
            victim.hurt(level.damageSources().magic(), DAMAGE);
        }
        return true;
    }

    @Override
    public int getManaCost() {
        return com.huige233.transcend.balance.BalanceConfig.get().scroll.eighteenfold_dragon_cost;
    }

    @Override
    public int getDuration() {
        return 0;
    }
}
