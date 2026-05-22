package com.huige233.transcend;

import com.huige233.transcend.world.nexus.NexusType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public final class TranscendGameRules {

    public static final GameRules.Key<GameRules.BooleanValue> BOSS_MASS_SPELL_CAN_GRIEF =
            GameRules.register("transcendBossMassSpellCanGrief",
                    GameRules.Category.MOBS,
                    GameRules.BooleanValue.create(false));

    // ─── Nexus Game Rules ────────────────────────────────────────────
    public static final GameRules.Key<GameRules.BooleanValue> NEXUS_BINDING =
            GameRules.register("transcendNexusBinding",
                    GameRules.Category.MISC,
                    GameRules.BooleanValue.create(false));

    public static final GameRules.Key<GameRules.BooleanValue> NEXUS_SCARCITY =
            GameRules.register("transcendNexusScarcity",
                    GameRules.Category.MISC,
                    GameRules.BooleanValue.create(false));

    public static final GameRules.Key<GameRules.BooleanValue> NEXUS_MERCY =
            GameRules.register("transcendNexusMercy",
                    GameRules.Category.MISC,
                    GameRules.BooleanValue.create(false));

    public static final GameRules.Key<GameRules.BooleanValue> NEXUS_FRAILTY =
            GameRules.register("transcendNexusFrailty",
                    GameRules.Category.MISC,
                    GameRules.BooleanValue.create(false));

    public static final GameRules.Key<GameRules.BooleanValue> NEXUS_ISOLATION =
            GameRules.register("transcendNexusIsolation",
                    GameRules.Category.MISC,
                    GameRules.BooleanValue.create(false));

    private TranscendGameRules() {
    }

    public static void init() {
        // Loads this class so gamerule registration runs during mod init.
    }

    public static boolean canBossMassSpellGrief(Level level) {
        return level != null && level.getGameRules().getBoolean(BOSS_MASS_SPELL_CAN_GRIEF);
    }

    /**
     * 根据枢纽类型设置对应的游戏规则。
     */
    public static void setNexusRule(MinecraftServer server, NexusType type, boolean value) {
        GameRules.Key<GameRules.BooleanValue> key = getNexusRuleKey(type);
        if (key != null) {
            server.getGameRules().getRule(key).set(value, server);
        }
    }

    /**
     * 查询枢纽对应的游戏规则是否已激活。
     */
    public static boolean isNexusRuleActive(Level level, NexusType type) {
        if (level == null) return false;
        GameRules.Key<GameRules.BooleanValue> key = getNexusRuleKey(type);
        return key != null && level.getGameRules().getBoolean(key);
    }

    private static GameRules.Key<GameRules.BooleanValue> getNexusRuleKey(NexusType type) {
        return switch (type) {
            case BINDING   -> NEXUS_BINDING;
            case SCARCITY  -> NEXUS_SCARCITY;
            case MERCY     -> NEXUS_MERCY;
            case FRAILTY   -> NEXUS_FRAILTY;
            case ISOLATION -> NEXUS_ISOLATION;
        };
    }
}
