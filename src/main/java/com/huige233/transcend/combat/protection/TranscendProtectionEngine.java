package com.huige233.transcend.combat.protection;

import com.huige233.transcend.mixinitf.ITranscendMarked;
import com.huige233.transcend.util.ArmorUtils;
import com.huige233.transcend.util.TranscendGuard;
import com.huige233.transcend.combat.ReentryGuard;
import com.huige233.transcend.combat.attack.HealthAccess;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.util.CuriosFinder;
import com.huige233.transcend.util.TranscendDeadInside;
import com.huige233.transcend.util.TranscendDefense;
import com.huige233.transcend.util.TranscendForceKillUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;


/** 统一评估玩家装备、终结标记和临时无敌状态，决定伤害与移除拦截及生命修复。 */
public final class TranscendProtectionEngine {
    private static final ReentryGuard<LivingEntity> REPAIRS = new ReentryGuard<>(8);

    private TranscendProtectionEngine() {}
    public static TranscendProtectionState state(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide)
            return new TranscendProtectionState(false, false, 0);
        boolean eligible = entity instanceof Player player && player.getInventory() != null
                && (ArmorUtils.fullEquipped(player) || TranscendGuard.hasShieldOrSword(player));
        boolean marked = (entity instanceof ITranscendMarked m && m.transcend$isMarked())
                || TranscendForceKillUtil.isKilling(entity)
                || TranscendDeadInside.isAntiResurrect(entity);
        int ticks = entity.getPersistentData().getInt(TranscendDefense.NBT_INVULNERABLE_TIME);
        return new TranscendProtectionState(eligible, marked, Math.max(0, ticks));
    }
    public static boolean hasEquipmentQualification(LivingEntity e) { return state(e).equipmentEligible(); }
    public static boolean isTerminal(LivingEntity e) { return state(e).terminalMarkWins(); }
    public static boolean isProtected(LivingEntity e) { return state(e).protectedNow(); }
    public static boolean blocksDamage(LivingEntity e) { return isProtected(e); }
    public static boolean blocksHealing(LivingEntity e) { return isTerminal(e); }
    public static boolean blocksTotem(LivingEntity e) { return isTerminal(e); }

    // 超越饰品保留原有的事件层防护，不因此获得套装的底层移除/位移拦截。
    public static boolean blocksEventDamage(LivingEntity e) {
        var state = state(e);
        if (state.terminalMarkWins()) return false;
        return state.protectedNow() || (e instanceof ServerPlayer player
                && !player.level().isClientSide
                && CuriosFinder.hasCurio(player, ModItems.transcend_curio.get()));
    }
    public static boolean shouldBlockRemoval(LivingEntity e, Entity.RemovalReason reason) {
        if (e == null || !(reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED)) return false;
        if (e instanceof ServerPlayer p && p.hasDisconnected()) return false;
        return isProtected(e);
    }
    public static void repair(LivingEntity e) {
        if (isProtected(e)) REPAIRS.run(e, () -> {
            TranscendAttributeGuard.enforce(e);
            float max = TranscendHealthDataGuard.safeMax(e);
            HealthAccess.write(e, max);
            TranscendHealthDataGuard.repair(e);
            e.deathTime = 0;
            e.hurtTime = 0;
        });
    }
}
