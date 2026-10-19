package com.huige233.transcend.tech.combat;

import com.huige233.transcend.combat.attack.TranscendHealthLedger;
import com.huige233.transcend.combat.protection.TranscendProtectionEngine;
import com.huige233.transcend.tech.quality.TechQuality;
import com.huige233.transcend.tech.rank.TechRank;
import com.huige233.transcend.util.TranscendGuard;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.HashMap;

/** 科技能源达到 A/S/EX 阈值后的直接生命层攻击。每个射击对同一目标只消耗一次预算。 */
public final class TechSuperAttackResolver {
    private static final Map<Level, Map<BudgetKey, Long>> SPENT = new WeakHashMap<>();
    // 大于弹体最长存活时间（60 tick），预算不能在弹体仍在飞行时清空。
    private static final long BUDGET_LIFETIME = 200L;
    private static final int MAX_BUDGETS_PER_LEVEL = 16_384;

    private TechSuperAttackResolver() {}

    public static boolean apply(LivingEntity target, TechRank rank, TechQuality quality,
                                boolean fullyReinforced, UUID shotId, long tick) {
        return apply(target, rank, quality, fullyReinforced, shotId, tick, null, 0);
    }

    public static boolean apply(LivingEntity target, TechRank rank, TechQuality quality,
                                boolean fullyReinforced, UUID shotId, long tick,
                                DamageSource source, int shieldPenetration) {
        if (target == null || target.level().isClientSide || rank == null
                || !rank.supportsDirectHealthAttack() || target.isRemoved()) return false;
        if (TranscendProtectionEngine.isProtected(target) || TranscendGuard.isProtected(target)) return false;
        if (target instanceof Player player && (player.isSpectator() || player.getAbilities().invulnerable)) return false;
        if (target instanceof Player defender && source != null && source.getEntity() instanceof Player attacker
                && !attacker.canHarmPlayer(defender)) return false;
        if (target instanceof Player && !(rank == TechRank.EX && quality == TechQuality.MYTHIC && fullyReinforced)) {
            return false;
        }
        if (rank == TechRank.EX && !fullyReinforced) return false;
        float maximum = target.getMaxHealth();
        if (!Float.isFinite(maximum) || maximum <= 0.0F) return false;
        float current = TranscendHealthLedger.read(target);
        if (!Float.isFinite(current) || current <= 0.0F) return false;
        float requested = maximum * rank.directHealthPercent() / 100.0F;
        BudgetKey key = new BudgetKey(shotId == null ? UUID.randomUUID() : shotId, target.getUUID());
        Map<BudgetKey, Long> budgets = SPENT.computeIfAbsent(target.level(), ignored -> new HashMap<>());
        budgets.entrySet().removeIf(entry -> tick < entry.getValue()
                || (double) tick - entry.getValue() > BUDGET_LIFETIME);
        if (budgets.size() >= MAX_BUDGETS_PER_LEVEL) return false;
        // 先锁定本次射击预算，再结算护盾，避免散射/穿透弹在护盾完全拦截时重复耗能。
        if (budgets.putIfAbsent(key, tick) != null) return false;
        // 直接生命攻击先穿过科技护盾，只有未被电容承受的部分才进入生命账本。
        float unblocked = com.huige233.transcend.handle.tech.PhaseShieldHandler
                .absorbDirectHealthAttack(target, requested, shieldPenetration, rank);
        if (unblocked <= 0.0001F) return true;
        // 直接生命攻击只压低当前生命；EX 对允许处决的目标可以降到 0。
        float next = rank == TechRank.A && !(target instanceof Player)
                ? Math.min(current, Math.max(1.0F, current - unblocked)) : Math.max(0.0F, current - unblocked);
        TranscendHealthLedger.forceSet(target, next);
        TranscendHealthLedger.converge(target, next);
        float after = TranscendHealthLedger.read(target);
        if (!Float.isFinite(after) || after > next + 0.01F) {
            return false;
        }
        if (after <= 0.0F) {
            target.die(source == null ? target.damageSources().generic() : source);
        }
        return true;
    }

    private record BudgetKey(UUID shot, UUID target) {}
}
