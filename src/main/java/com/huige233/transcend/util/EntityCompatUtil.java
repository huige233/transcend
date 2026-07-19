package com.huige233.transcend.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class EntityCompatUtil {

    private EntityCompatUtil() {
    }

    public static boolean isProtectedPlayer(Entity entity) {
        return entity instanceof Player player && (player.isCreative() || player.isSpectator());
    }

    public static Player findNearestValidPlayer(Level level, Entity center, double radius) {
        if (level == null || center == null) return null;
        return level.getNearestPlayer(center.getX(), center.getY(), center.getZ(), radius,
                p -> p != null && p.isAlive() && !isProtectedPlayer(p));
    }
}
