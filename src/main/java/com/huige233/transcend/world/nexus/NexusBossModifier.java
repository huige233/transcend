package com.huige233.transcend.world.nexus;

import net.minecraft.server.MinecraftServer;

/**
 * 枢纽对超越化身的影响计算器。
 *
 * 逐个枢纽削弱效果：
 * - 束缚(BINDING)：Boss施法间隔 +50%
 * - 匮乏(SCARCITY)：Boss法术伤害 -50%
 * - 怜悯(MERCY)：Boss自然恢复和吸血不再能恢复护盾
 * - 脆弱(FRAILTY)：Boss护盾额外35%全属性减伤消失 + 每阶段抗性buff -1级
 * - 隔绝(ISOLATION)：Boss闪现/召唤陨石等技能CD +50%
 *
 * 全部摧毁时的加强效果：
 * - 超越化身直接从 PHASE_3 开始
 * - 第四阶段拥有一次满状态复活机会
 * - 法术威力乘数底线提升为 3.0
 */
public final class NexusBossModifier {

    private NexusBossModifier() {}

    // ─── 束缚(BINDING) ──────────────────────────────────────────────

    /**
     * Boss施法间隔乘数。束缚枢纽摧毁 → Boss施法更慢 (×1.5)
     */
    public static float getBossSpellIntervalMultiplier(MinecraftServer server) {
        return NexusManager.isNexusDestroyed(server, NexusType.BINDING) ? 1.5F : 1.0F;
    }

    // ─── 匮乏(SCARCITY) ─────────────────────────────────────────────

    /**
     * Boss法术伤害乘数。匮乏枢纽摧毁 → Boss法术伤害 -50% (×0.5)
     */
    public static float getBossSpellPowerMultiplier(MinecraftServer server) {
        return NexusManager.isNexusDestroyed(server, NexusType.SCARCITY) ? 0.5F : 1.0F;
    }

    // ─── 怜悯(MERCY) ────────────────────────────────────────────────

    /**
     * Boss自然恢复/吸血是否能恢复护盾。怜悯枢纽摧毁 → 不能。
     */
    public static boolean canBossRegenShield(MinecraftServer server) {
        return !NexusManager.isNexusDestroyed(server, NexusType.MERCY);
    }

    // ─── 脆弱(FRAILTY) ──────────────────────────────────────────────

    /**
     * Boss护盾是否有额外35%全属性减伤。脆弱枢纽摧毁 → 没有。
     */
    public static boolean hasBossShieldDamageReduction(MinecraftServer server) {
        return !NexusManager.isNexusDestroyed(server, NexusType.FRAILTY);
    }

    /**
     * Boss抗性等级偏移。脆弱枢纽摧毁 → 每阶段少1级抗性
     */
    public static int getBossResistanceLevelOffset(MinecraftServer server) {
        return NexusManager.isNexusDestroyed(server, NexusType.FRAILTY) ? -1 : 0;
    }

    // ─── 隔绝(ISOLATION) ────────────────────────────────────────────

    /**
     * Boss技能CD乘数（闪现、召唤陨石等）。隔绝枢纽摧毁 → CD +50% (×1.5)
     */
    public static float getBossAbilityCooldownMultiplier(MinecraftServer server) {
        return NexusManager.isNexusDestroyed(server, NexusType.ISOLATION) ? 1.5F : 1.0F;
    }

    // ─── 全部摧毁时的加强效果 ────────────────────────────────────────

    /** 全部摧毁 → Boss直接从第三阶段开始 */
    public static boolean shouldStartAtPhase3(MinecraftServer server) {
        return NexusManager.areAllNexusesDestroyed(server);
    }

    /** 全部摧毁 → Boss在第四阶段拥有一次满状态复活机会 */
    public static boolean shouldBossHaveResurrection(MinecraftServer server) {
        return NexusManager.areAllNexusesDestroyed(server);
    }

    /** 全部摧毁 → Boss法术威力底线乘数 3.0 */
    public static float getBossMinSpellPowerMultiplier(MinecraftServer server) {
        return NexusManager.areAllNexusesDestroyed(server) ? 3.0F : 1.0F;
    }

    /** 获取摧毁的枢纽数量（0-5） */
    public static int getDestroyedCount(MinecraftServer server) {
        return NexusManager.getDestroyedCount(server);
    }
}
