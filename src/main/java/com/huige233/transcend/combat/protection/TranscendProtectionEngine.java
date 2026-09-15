package com.huige233.transcend.combat.protection;

import com.huige233.transcend.mixinitf.ITranscendMarked;
import com.huige233.transcend.util.ArmorUtils;
import com.huige233.transcend.util.TranscendGuard;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;


/** 统一评估玩家装备、终结标记和临时无敌状态，决定伤害与移除拦截及生命修复。 */
public final class TranscendProtectionEngine {
    private TranscendProtectionEngine() {}
    public static TranscendProtectionState state(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide || !(entity instanceof Player player))
            return new TranscendProtectionState(false, false, 0);
        boolean eligible = ArmorUtils.fullEquipped(player) || TranscendGuard.hasShieldOrSword(player);
        boolean marked = entity instanceof ITranscendMarked m && m.transcend$isMarked();
        int ticks = entity.getPersistentData().getInt("transcend_invulnerable_time");
        return new TranscendProtectionState(eligible, marked, Math.max(0, ticks));
    }
    public static boolean hasEquipmentQualification(LivingEntity e) { return state(e).equipmentEligible(); }
    public static boolean isTerminal(LivingEntity e) { return state(e).terminalMarkWins(); }
    public static boolean isProtected(LivingEntity e) { return state(e).protectedNow(); }
    public static boolean blocksDamage(LivingEntity e) { return isProtected(e); }
    public static boolean blocksHealing(LivingEntity e) { return isTerminal(e); }
    public static boolean blocksTotem(LivingEntity e) { return isTerminal(e); }
    public static boolean shouldBlockRemoval(LivingEntity e, Entity.RemovalReason reason) {
        if (e == null || !(reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED)) return false;
        if (e instanceof ServerPlayer p && p.hasDisconnected()) return false;
        return isProtected(e);
    }
    public static void repair(LivingEntity e) {
        if (isProtected(e)) {
            float max = TranscendHealthDataGuard.safeMax(e);
            com.huige233.transcend.combat.attack.HealthAccess.write(e, max);
            TranscendHealthDataGuard.repair(e);
        }
    }
}
