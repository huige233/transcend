package com.huige233.transcend.combat.attack;

import com.huige233.transcend.TranscendDamage;
import com.huige233.transcend.util.TranscendDeadInside;
import com.huige233.transcend.util.TranscendForceKillUtil;
import com.huige233.transcend.mixin.LivingEntityAttackInvoker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import javax.annotation.Nullable;


/** 校验服务端目标和递归深度后，按显式攻击等级分派伤害、击杀或移除操作。 */
public final class TranscendAttackEngine {
    private TranscendAttackEngine() {}
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final int MAX_DEPTH = 8;
    public static AttackResult apply(@Nullable Entity target, AttackProfile profile) {
        int depth = DEPTH.get();
        if (depth >= MAX_DEPTH) return AttackResult.rejected(profile.level(), "attack recursion limit");
        DEPTH.set(depth + 1);
        try { return applyInternal(target, profile); }
        finally { if (depth == 0) DEPTH.remove(); else DEPTH.set(depth); }
    }
    private static AttackResult applyInternal(@Nullable Entity target, AttackProfile profile) {
        if (target == null || target.level().isClientSide || !(target.level() instanceof ServerLevel))
            return AttackResult.rejected(profile.level(), "server target required");
        if (target instanceof Player && !profile.permitsPlayerTarget())
            return AttackResult.rejected(profile.level(), "player lifecycle protected");
        if (profile.level() == AttackLevel.WORLD_PURGE)
            return AttackResult.rejected(profile.level(), "world purge requires an explicit non-player target");
        if (profile.level() == AttackLevel.HARD_DELETE) {
            TranscendForceKillUtil.forceRemove(target, Entity.RemovalReason.KILLED);
            return AttackResult.done(profile.level(), true, "explicit target removed");
        }
        if (profile.level() == AttackLevel.DEAD_INSIDE && target instanceof LivingEntity living) {
            TranscendDeadInside.apply(living, profile.attacker() instanceof Player p ? p : null);
            return AttackResult.done(profile.level(), true, "dead inside");
        }
        if (profile.level() == AttackLevel.FORCE_KILL) {
            TranscendForceKillUtil.forceKill(target, profile.attacker());
            return AttackResult.done(profile.level(), true, "explicit target force killed");
        }
        if (!(target instanceof LivingEntity living))
            return AttackResult.rejected(profile.level(), "living target required");
        var source = TranscendDamage.attack(living.level(), profile);
        if (profile.level() == AttackLevel.ACTUALLY_HURT) {
            living.invulnerableTime = 0;
            ((LivingEntityAttackInvoker) living).transcend$actuallyHurt(source, profile.amount());
            return AttackResult.done(profile.level(), true, "actuallyHurt invoked");
        }
        boolean changed = living.hurt(source, profile.amount());
        return AttackResult.done(profile.level(), changed, "tagged damage applied");
    }
}
