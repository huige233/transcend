package com.huige233.transcend.circle.scroll;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 化身坠落 — 终局核弹。20格半径，对所有非玩家实体造成50魔法伤害（Boss减半），向外击退3格。
 */
public class AvatarFallEffect implements ScrollEffect {

    private static final int RADIUS = 20;
    private static final float DAMAGE = 50.0F;
    private static final float BOSS_DAMAGE = 25.0F;
    private static final double KNOCKBACK_POWER = 1.2D; // 约 3 格

    @Override
    public boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos) {
        // Round 39: 终焉级双层法环 + 大冲击波 + 中心涟漪（化身坠落）
        com.huige233.transcend.circle.scroll.ScrollVisualHelper.circle(level, pos, RADIUS, 1.0F, 0.4F, 0.0F, 400, "pentagram");
        com.huige233.transcend.circle.scroll.ScrollVisualHelper.circle(level, pos, RADIUS * 1.1F, 0.9F, 0.1F, 0.0F, 400, "hexagram");
        com.huige233.transcend.circle.scroll.ScrollVisualHelper.shockwave(level, pos, RADIUS * 1.2F, 1.0F, 0.5F, 0.1F, 400);
        com.huige233.transcend.circle.scroll.ScrollVisualHelper.shieldRipple(level, pos, RADIUS, 1.0F, 0.7F, 0.2F, 400);

        Vec3 center = Vec3.atCenterOf(pos);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, ScrollEffectUtil.radiusBox(pos, RADIUS),
                e -> !(e instanceof Player));
        // Round 41: 中心 + 每目标双层红橙光柱 — 化身坠落
        com.huige233.transcend.circle.scroll.ScrollVisualHelper.pillarFromSky(level,
                center, 40.0F, 1.0F, 0.5F, 0.1F, 80);
        for (LivingEntity target : targets) {
            float dmg = ScrollEffectUtil.isBoss(target) ? BOSS_DAMAGE : DAMAGE;
            // 每目标头顶光柱
            com.huige233.transcend.circle.scroll.ScrollVisualHelper.pillarFromSky(level,
                    new Vec3(target.getX(), target.getY(), target.getZ()),
                    30.0F, 1.0F, 0.4F, 0.0F, 60);
            target.hurt(level.damageSources().magic(), dmg);

            // 向外击退
            Vec3 dir = target.position().subtract(center);
            if (dir.lengthSqr() < 1.0E-4) {
                dir = new Vec3(level.random.nextDouble() - 0.5, 0, level.random.nextDouble() - 0.5);
            }
            Vec3 norm = dir.normalize();
            target.setDeltaMovement(
                    norm.x * KNOCKBACK_POWER,
                    0.6D,
                    norm.z * KNOCKBACK_POWER);
            target.hurtMarked = true;
        }
        return true;
    }

    @Override
    public int getManaCost() {
        return com.huige233.transcend.balance.BalanceConfig.get().scroll.avatar_fall_cost;
    }

    @Override
    public int getDuration() {
        return 0;
    }
}
