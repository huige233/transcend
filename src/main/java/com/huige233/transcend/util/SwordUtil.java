package com.huige233.transcend.util;

import com.huige233.transcend.combat.attack.AttackLevel;
import com.huige233.transcend.combat.attack.AttackProfile;
import com.huige233.transcend.combat.attack.TranscendAttackEngine;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;

/** 将剑的单体和范围强制击杀请求转交统一攻击引擎。 */
public final class SwordUtil {
    private SwordUtil() {}
    public static void annihilate(Entity target, @Nullable Player attacker) {
        TranscendAttackEngine.apply(target, AttackProfile.of(AttackLevel.FORCE_KILL, Float.MAX_VALUE, attacker));
    }
    public static int killRange(Level level, Player attacker, int range) {
        if (level.isClientSide) return 0;
        int count = 0;
        var box = attacker.getBoundingBox().inflate(Math.max(0, range));
        for (Entity entity : level.getEntities(attacker, box, e -> !(e instanceof Player) && e.isAlive())) {
            var result = TranscendAttackEngine.apply(entity,
                    AttackProfile.of(AttackLevel.FORCE_KILL, Float.MAX_VALUE, attacker));
            if (result.changed()) count++;
        }
        return count;
    }
}
