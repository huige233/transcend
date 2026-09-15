package com.huige233.transcend.combat.attack;

import net.minecraft.world.entity.Entity;


/** 记录分级攻击是否获准、是否产生变化及其原因。 */
public record AttackResult(AttackLevel level, boolean accepted, boolean changed, String reason) {
    public static AttackResult rejected(AttackLevel level, String reason) { return new AttackResult(level, false, false, reason); }
    public static AttackResult done(AttackLevel level, boolean changed, String reason) { return new AttackResult(level, true, changed, reason); }
}
