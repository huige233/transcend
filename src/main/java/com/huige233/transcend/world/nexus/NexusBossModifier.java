package com.huige233.transcend.world.nexus;

import net.minecraft.server.MinecraftServer;

public final class NexusBossModifier {

    private NexusBossModifier() {}

    public static boolean canBossRapidTeleport(MinecraftServer server) {
        return NexusManager.isNexusDestroyed(server, NexusType.BINDING);
    }

    public static float getBossTeleportCooldownMult(MinecraftServer server) {
        return canBossRapidTeleport(server) ? 0.4F : 1.0F;
    }

    public static boolean canBossSiphonAura(MinecraftServer server) {
        return NexusManager.isNexusDestroyed(server, NexusType.SCARCITY);
    }

    public static boolean isBossReactionImmune(MinecraftServer server) {
        return NexusManager.isNexusDestroyed(server, NexusType.ENTROPY);
    }

    public static boolean canBossApplyClashOnPlayer(MinecraftServer server) {
        return NexusManager.isNexusDestroyed(server, NexusType.ENTROPY);
    }

    public static boolean hasExecuteSlash(MinecraftServer server) {
        return NexusManager.isNexusDestroyed(server, NexusType.FRAILTY);
    }

    public static float getExecuteThreshold() { return 0.25F; }

    public static float getExecuteDamageMult() { return 0.5F; }

    public static boolean hasBossBurstCycle(MinecraftServer server) {
        return NexusManager.isNexusDestroyed(server, NexusType.SILENCE);
    }

    public static boolean shouldStartAtPhase3(MinecraftServer server) {
        return NexusManager.areAllNexusesDestroyed(server);
    }

    public static boolean shouldBossHaveResurrection(MinecraftServer server) {
        return NexusManager.areAllNexusesDestroyed(server);
    }

    public static float getBossMinSpellPowerMultiplier(MinecraftServer server) {
        return NexusManager.areAllNexusesDestroyed(server) ? 3.0F : 1.0F;
    }

    public static int getDestroyedCount(MinecraftServer server) {
        return NexusManager.getDestroyedCount(server);
    }
}
