package com.huige233.transcend.handle;

import com.huige233.transcend.entity.projectile.ParticleBolt;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;


/** 在天狼星弹体有效命中受支持的非玩家目标后施加短时虚弱并阻止治疗。 */
public final class SiriusCombatHandler {
    private static final String UNTIL = "transcend_sirius_heal_lock_until";
    private SiriusCombatHandler() {}

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!(event.getSource().getDirectEntity() instanceof ParticleBolt bolt)
                || !bolt.isSirius() || event.isCanceled()
                || !Float.isFinite(event.getAmount()) || event.getAmount() <= 0.0F
                || com.huige233.transcend.tech.combat.BossDefenseRegistry.percentDamage(
                        target, event.getSource(), bolt.penetration()) <= 0.0F
                || target.level().isClientSide || target instanceof net.minecraft.world.entity.player.Player) return;
        if (!com.huige233.transcend.tech.combat.BossDefenseRegistry.allowsPercentDamage(target, event.getSource())) return;
        long until = target.level().getGameTime() + 40L;
        target.getPersistentData().putLong(UNTIL, until);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false));
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        LivingEntity target = event.getEntity();
        if (!target.level().isClientSide && target.getPersistentData().getLong(UNTIL) > target.level().getGameTime()) {
            event.setAmount(0.0F);
            event.setCanceled(true);
        }
    }
}
