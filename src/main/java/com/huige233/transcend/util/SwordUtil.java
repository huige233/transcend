package com.huige233.transcend.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** 剑类工具（耐久/伤害）工具类。 */
public class SwordUtil {

    public static void annihilate(Entity target, @Nullable Player attacker) {
        TranscendForceKillUtil.forceKill(target, attacker);
    }

    public static void kill(Entity target, @Nullable Entity attacker) {
        TranscendForceKillUtil.forceKill(target, attacker);
    }

    public static int killRange(Level level, Player attacker, int range) {
        return TranscendForceKillUtil.forceKillRange(level, attacker, range);
    }

    public static int removeAllEntities(Level level, @Nullable Player attacker) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return 0;
        }

        List<Entity> snapshot = new ArrayList<>();
        serverLevel.getAllEntities().forEach(snapshot::add);

        int count = 0;
        for (Entity entity : snapshot) {
            if (entity == null || entity instanceof Player) {
                continue;
            }
            if (attacker != null && entity == attacker) {
                continue;
            }
            TranscendForceKillUtil.forceKill(entity, attacker);
            count++;
        }
        return count;
    }

    public static void erase(Entity entity) {
        TranscendForceKillUtil.forceRemove(entity, Entity.RemovalReason.KILLED);
    }

    public static void purge(Entity entity) {
        if (entity == null || entity.level().isClientSide) return;
        TranscendUnsafeKill.forceRemove(entity, Entity.RemovalReason.KILLED);
        TranscendEntityPurge.purgeFromLevel(entity, true);
    }
}
