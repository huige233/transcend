package com.huige233.transcend.combat.attack;

import net.minecraft.world.entity.Entity;
import javax.annotation.Nullable;


/** 封装攻击等级、伤害量、尝试预算与攻击者，并据此判定防御绕过和玩家目标限制。 */
public record AttackProfile(AttackLevel level, float amount, int maxAttempts, @Nullable Entity attacker) {
    public AttackProfile {
        if (level == null) throw new IllegalArgumentException("level");
        if (!Float.isFinite(amount) || amount < 0) amount = 0;
        amount = Math.min(amount, 1.0e7f);
        maxAttempts = Math.max(1, Math.min(maxAttempts, 20));
    }
    public static AttackProfile of(AttackLevel level, float amount, @Nullable Entity attacker) {
        return new AttackProfile(level, amount, 1, attacker);
    }
    public boolean bypassesArmor() { return level != AttackLevel.NORMAL; }
    public boolean bypassesResistance() { return level.ordinal() >= AttackLevel.RESISTANCE_PIERCING.ordinal(); }
    public boolean bypassesEnchantments() { return level.ordinal() >= AttackLevel.TRUE_DAMAGE.ordinal(); }
    public boolean bypassesShield() { return bypassesEnchantments(); }
    public boolean bypassesCooldown() { return level.ordinal() >= AttackLevel.INVULNERABILITY_PIERCING.ordinal(); }
    
    public boolean bypassesInvulnerability() {
        return level.ordinal() >= AttackLevel.INVULNERABILITY_PIERCING.ordinal() && level != AttackLevel.SOFT_KILL;
    }
    public boolean permitsPlayerTarget() { return level != AttackLevel.HARD_DELETE && level != AttackLevel.WORLD_PURGE; }
}
