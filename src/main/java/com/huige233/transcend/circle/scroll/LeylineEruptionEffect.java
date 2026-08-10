package com.huige233.transcend.circle.scroll;

import com.huige233.transcend.world.mana.ChunkManaSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 地脉迸裂卷轴效果实现。 */
public class LeylineEruptionEffect implements ScrollEffect {

    private static final int RADIUS = 20;
    private static final float DAMAGE = 15.0F;
    private static final double LAUNCH_VELOCITY = 0.55D;

    @Override
    public boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos) {

        ScrollVisualHelper.shockwave(level, pos, RADIUS, 0.3F, 0.95F, 0.4F, 120);
        ScrollVisualHelper.circle(level, pos, RADIUS * 0.7F, 0.4F, 1.0F, 0.5F, 320, "hexagram");

        List<LivingEntity> hostiles = level.getEntitiesOfClass(
                LivingEntity.class, ScrollEffectUtil.radiusBox(pos, RADIUS),
                ScrollEffectUtil::isHostile);
        for (LivingEntity target : hostiles) {

            ScrollVisualHelper.pillarFromGround(level,
                    new net.minecraft.world.phys.Vec3(target.getX(), target.getY(), target.getZ()),
                    8.0F, 0.3F, 0.95F, 0.85F, 60);
            target.hurt(level.damageSources().magic(), DAMAGE);
        }

        List<Entity> all = level.getEntitiesOfClass(
                Entity.class, ScrollEffectUtil.radiusBox(pos, RADIUS), e -> true);
        for (Entity e : all) {
            Vec3 m = e.getDeltaMovement();
            e.setDeltaMovement(m.x, Math.max(m.y, 0) + LAUNCH_VELOCITY, m.z);
            e.hurtMarked = true;
        }

        ChunkManaSavedData data = ChunkManaSavedData.get(level);
        ChunkPos cpos = new ChunkPos(pos);
        data.setMana(cpos, data.getMana(cpos) +
                com.huige233.transcend.balance.BalanceConfig.get().scroll.leyline_eruption_mana_restore);

        return true;
    }

    @Override
    public int getManaCost() {
        return com.huige233.transcend.balance.BalanceConfig.get().scroll.leyline_eruption_cost;
    }

    @Override
    public int getDuration() {
        return 0;
    }
}
