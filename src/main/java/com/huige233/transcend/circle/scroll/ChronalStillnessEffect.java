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
 * 时停 — 32格半径，对非玩家生物施加 缓慢V+虚弱V+挖掘疲劳V 160tick，Boss仅40tick。
 */
public class ChronalStillnessEffect implements ScrollEffect {

    private static final int RADIUS = 32;
    private static final int DURATION_NORMAL = 160;
    private static final int DURATION_BOSS = 40;
    private static final int AMPLIFIER = 4; // V 级

    @Override
    public boolean execute(ServerLevel level, ServerPlayer caster, BlockPos pos) {
        // Round 39: 蓝色 hexagram 法环 + 慢速涟漪（时间冻结视觉）
        ScrollVisualHelper.circle(level, pos, RADIUS, 0.4F, 0.7F, 1.0F, 400, "hexagram");
        ScrollVisualHelper.shieldRipple(level, pos, RADIUS, 0.5F, 0.8F, 1.0F, 160);

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, ScrollEffectUtil.radiusBox(pos, RADIUS),
                e -> !(e instanceof Player));
        for (LivingEntity target : targets) {
            int duration = ScrollEffectUtil.isBoss(target) ? DURATION_BOSS : DURATION_NORMAL;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, AMPLIFIER, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, AMPLIFIER, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, AMPLIFIER, false, true));
        }
        return true;
    }

    @Override
    public int getManaCost() {
        return com.huige233.transcend.balance.BalanceConfig.get().scroll.chronal_stillness_cost;
    }

    @Override
    public int getDuration() {
        return 0;
    }
}
