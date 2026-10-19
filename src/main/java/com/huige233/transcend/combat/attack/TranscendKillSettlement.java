package com.huige233.transcend.combat.attack;

import com.huige233.transcend.combat.ReentryGuard;
import com.huige233.transcend.combat.protection.TranscendProtectionEngine;
import com.huige233.transcend.mixin.LivingEntityDeathAccess;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** 保留击杀归属，并为强杀中漏过的非玩家掉落提供一次结算机会。 */
public final class TranscendKillSettlement {
    private static final String LOOT_SETTLED = "transcend_death_loot_settled";
    private static final ReentryGuard<LivingEntity> DEATHS = new ReentryGuard<>(8);
    private static final ReentryGuard<LivingEntity> DROPS = new ReentryGuard<>(8);

    private TranscendKillSettlement() {}

    public static void prepare(LivingEntity target, DamageSource source, Entity attacker) {
        if (target.level().isClientSide) return;
        if (attacker instanceof LivingEntity living) target.setLastHurtByMob(living);
        if (attacker instanceof Player player) {
            target.setLastHurtByPlayer(player);
            ((LivingEntityDeathAccess) target).transcend$setLastHurtByPlayerTime(100);
        }
        float health = HealthAccess.read(target);
        target.getCombatTracker().recordDamage(source,
                Float.isFinite(health) ? Math.max(1.0F, Math.min(health, 1.0e7F)) : 1.0F);
    }

    public static boolean hasSettledLoot(LivingEntity target) {
        return target.getPersistentData().getBoolean(LOOT_SETTLED);
    }

    public static void runDeath(LivingEntity target, Runnable original) {
        if (!TranscendProtectionEngine.isTerminal(target)) {
            original.run();
        } else if (!hasSettledLoot(target)) {
            DEATHS.run(target, original);
        }
    }

    public static void runDrops(LivingEntity target, Runnable original) {
        if (target.level().isClientSide) {
            original.run();
            return;
        }
        if (TranscendProtectionEngine.isTerminal(target) && hasSettledLoot(target)) return;
        // 在调用前登记，防止掉落事件中的重入重复发放；异常后也不自动重发部分掉落。
        target.getPersistentData().putBoolean(LOOT_SETTLED, true);
        original.run();
    }

    public static void finishMob(LivingEntity target, DamageSource source) {
        if (target instanceof Player || target.level().isClientSide || hasSettledLoot(target)) return;
        DROPS.run(target, () -> {
            try {
                ((LivingEntityDeathAccess) target).transcend$dropAllDeathLoot(source);
            } finally {
                target.getPersistentData().putBoolean(LOOT_SETTLED, true);
            }
        });
    }
}
