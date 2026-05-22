package com.huige233.transcend.circle.scroll;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 倒悬天诏 — 18格内非玩家实体向上抛8格，并施加 漂浮II 60tick。
 */
public class InvertedHeavenEffect implements ScrollEffect {

    private static final int RADIUS = 18;
    private static final double LIFT_VELOCITY = 1.5D;
    private static final int LEVITATION_DURATION = 60;
    private static final int LEVITATION_AMP = 1; // II

    @Override
    public boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos) {
        // Round 39: 紫白色升空冲击波（倒悬天诏）
        com.huige233.transcend.circle.scroll.ScrollVisualHelper.shockwave(level, pos, RADIUS, 0.7F, 0.5F, 1.0F, 400);
        com.huige233.transcend.circle.scroll.ScrollVisualHelper.circle(level, pos, RADIUS * 0.6F, 0.85F, 0.7F, 1.0F, 400, "hexagram");

        List<Entity> entities = level.getEntitiesOfClass(
                Entity.class, ScrollEffectUtil.radiusBox(pos, RADIUS),
                e -> !(e instanceof Player));
        for (Entity e : entities) {
            Vec3 m = e.getDeltaMovement();
            e.setDeltaMovement(m.x, LIFT_VELOCITY, m.z);
            e.hurtMarked = true;
            if (e instanceof LivingEntity le) {
                le.addEffect(new MobEffectInstance(MobEffects.LEVITATION, LEVITATION_DURATION, LEVITATION_AMP, false, true));
            }
        }
        return true;
    }

    @Override
    public int getManaCost() {
        return com.huige233.transcend.balance.BalanceConfig.get().scroll.inverted_heaven_cost;
    }

    @Override
    public int getDuration() {
        return 0;
    }
}
