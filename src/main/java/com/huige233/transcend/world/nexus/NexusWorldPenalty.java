package com.huige233.transcend.world.nexus;

import net.minecraft.server.MinecraftServer;

/**
 * 枢纽破碎的全局惩罚计算器。
 *
 * 每个被摧毁的枢纽都会对全世界施加负面效果：
 *
 * - 束缚(BINDING)：全世界施法速度 -25%（施法间隔 ×1.25）
 * - 匮乏(SCARCITY)：全世界魔力自然恢复停止，区块魔力上限被限制到 1000
 * - 怜悯(MERCY)：正面buff被施加时持续时间 -40%，等级 -1
 * - 脆弱(FRAILTY)：全世界承伤 +15%，护甲/韧性都下降 4 点
 * - 隔绝(ISOLATION)：魔力井提取速度降到几乎为 0，玩家施法消耗魔力 +25%
 *
 * 这些惩罚是累加的。
 */
public final class NexusWorldPenalty {

    private NexusWorldPenalty() {}

    // ─── 束缚(BINDING) ──────────────────────────────────────────────

    /**
     * 全世界施法速度乘数。束缚被摧毁 → 施法间隔变长 (×1.25 = 速度 -25%)
     */
    public static float getSpellCastIntervalMultiplier(MinecraftServer server) {
        if (server == null) return 1.0F;
        return NexusManager.isNexusDestroyed(server, NexusType.BINDING) ? 1.25F : 1.0F;
    }

    // ─── 匮乏(SCARCITY) ─────────────────────────────────────────────

    /**
     * 魔力自然恢复是否停止。匮乏被摧毁 → 停止。
     */
    public static boolean isManaRegenStopped(MinecraftServer server) {
        if (server == null) return false;
        return NexusManager.isNexusDestroyed(server, NexusType.SCARCITY);
    }

    /**
     * 区块魔力上限。匮乏被摧毁 → 限制到 1000。
     * 返回 -1 表示无限制。
     */
    public static float getChunkManaCap(MinecraftServer server) {
        if (server == null) return -1.0F;
        return NexusManager.isNexusDestroyed(server, NexusType.SCARCITY) ? 1000.0F : -1.0F;
    }

    // ─── 怜悯(MERCY) ────────────────────────────────────────────────

    /**
     * 正面效果持续时间乘数。怜悯被摧毁 → -40% (×0.6)
     */
    public static float getPositiveEffectDurationMultiplier(MinecraftServer server) {
        if (server == null) return 1.0F;
        return NexusManager.isNexusDestroyed(server, NexusType.MERCY) ? 0.6F : 1.0F;
    }

    /**
     * 正面效果等级偏移。怜悯被摧毁 → -1级
     */
    public static int getPositiveEffectAmplifierOffset(MinecraftServer server) {
        if (server == null) return 0;
        return NexusManager.isNexusDestroyed(server, NexusType.MERCY) ? -1 : 0;
    }

    // ─── 脆弱(FRAILTY) ──────────────────────────────────────────────

    /**
     * 受到伤害乘数。脆弱被摧毁 → +15% (×1.15)
     */
    public static float getDamageTakenMultiplier(MinecraftServer server) {
        if (server == null) return 1.0F;
        return NexusManager.isNexusDestroyed(server, NexusType.FRAILTY) ? 1.15F : 1.0F;
    }

    /**
     * 护甲/韧性削减量。脆弱被摧毁 → -4
     */
    public static float getArmorReduction(MinecraftServer server) {
        if (server == null) return 0.0F;
        return NexusManager.isNexusDestroyed(server, NexusType.FRAILTY) ? 4.0F : 0.0F;
    }

    /**
     * 韧性削减量。脆弱被摧毁 → -4
     */
    public static float getToughnessReduction(MinecraftServer server) {
        if (server == null) return 0.0F;
        return NexusManager.isNexusDestroyed(server, NexusType.FRAILTY) ? 4.0F : 0.0F;
    }

    // ─── 隔绝(ISOLATION) ────────────────────────────────────────────

    /**
     * 魔力井提取速度乘数。隔绝被摧毁 → 几乎为 0 (×0.01)
     */
    public static float getManaWellExtractMultiplier(MinecraftServer server) {
        if (server == null) return 1.0F;
        return NexusManager.isNexusDestroyed(server, NexusType.ISOLATION) ? 0.01F : 1.0F;
    }

    /**
     * 玩家施法消耗魔力乘数。隔绝被摧毁 → +25% (×1.25)
     */
    public static float getSpellManaCostMultiplier(MinecraftServer server) {
        if (server == null) return 1.0F;
        return NexusManager.isNexusDestroyed(server, NexusType.ISOLATION) ? 1.25F : 1.0F;
    }

    // ─── 通用 ────────────────────────────────────────────────────────

    /**
     * 是否有任何全局惩罚激活。
     */
    public static boolean hasAnyPenalty(MinecraftServer server) {
        if (server == null) return false;
        return NexusManager.getDestroyedCount(server) > 0;
    }
}
