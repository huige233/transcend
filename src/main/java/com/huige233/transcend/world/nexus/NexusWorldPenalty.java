package com.huige233.transcend.world.nexus;

import com.huige233.transcend.world.TranscendDimensions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** 次元世界惩罚规则类。 */
public final class NexusWorldPenalty {

    private NexusWorldPenalty() {}

    public static boolean isConstraintActive(ServerPlayer sp, NexusType type) {
        if (sp == null) return false;
        if (sp.level().dimension() != TranscendDimensions.NEXUS_LEVEL) return false;
        MinecraftServer server = sp.getServer();
        return server != null && !NexusManager.isNexusDestroyed(server, type);
    }

    public static boolean isDashBlocked(ServerPlayer sp) {
        return isConstraintActive(sp, NexusType.BINDING);
    }

    public static float getManaCapMultiplier(ServerPlayer sp) {
        return isConstraintActive(sp, NexusType.SCARCITY) ? 0.5F : 1.0F;
    }

    public static boolean isAuraAbsorptionDisabled(ServerPlayer sp) {
        return isConstraintActive(sp, NexusType.SCARCITY);
    }

    public static boolean isReactionScrambled(ServerPlayer sp) {
        return isConstraintActive(sp, NexusType.ENTROPY);
    }

    public static double getMaxHealthModifier(ServerPlayer sp) {
        return isConstraintActive(sp, NexusType.FRAILTY) ? -0.2 : 0.0;
    }

    public static float getHealMultiplier(ServerPlayer sp) {
        return isConstraintActive(sp, NexusType.FRAILTY) ? 0.5F : 1.0F;
    }

    public static boolean isClassResourceFrozen(ServerPlayer sp) {
        return isConstraintActive(sp, NexusType.SILENCE);
    }

    public static boolean hasAnyConstraint(ServerPlayer sp) {
        if (sp == null) return false;
        if (sp.level().dimension() != TranscendDimensions.NEXUS_LEVEL) return false;
        MinecraftServer server = sp.getServer();
        if (server == null) return false;

        for (NexusType type : NexusType.values()) {
            if (!NexusManager.isNexusDestroyed(server, type)) return true;
        }
        return false;
    }
}
