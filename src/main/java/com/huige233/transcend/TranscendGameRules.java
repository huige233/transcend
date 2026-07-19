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

    public static final GameRules.Key<GameRules.BooleanValue> NEXUS_BINDING =
            GameRules.register("transcendNexusBinding",
                    GameRules.Category.MISC,
                    GameRules.BooleanValue.create(false));

    public static final GameRules.Key<GameRules.BooleanValue> NEXUS_SCARCITY =
            GameRules.register("transcendNexusScarcity",
                    GameRules.Category.MISC,
                    GameRules.BooleanValue.create(false));

    public static final GameRules.Key<GameRules.BooleanValue> NEXUS_ENTROPY =
            GameRules.register("transcendNexusEntropy",
                    GameRules.Category.MISC,
                    GameRules.BooleanValue.create(false));

    public static final GameRules.Key<GameRules.BooleanValue> NEXUS_FRAILTY =
            GameRules.register("transcendNexusFrailty",
                    GameRules.Category.MISC,
                    GameRules.BooleanValue.create(false));

    public static final GameRules.Key<GameRules.BooleanValue> NEXUS_SILENCE =
            GameRules.register("transcendNexusSilence",
                    GameRules.Category.MISC,
                    GameRules.BooleanValue.create(false));

    private TranscendGameRules() {
    }

    public static void init() {

    }

    public static boolean canBossMassSpellGrief(Level level) {
        return level != null && level.getGameRules().getBoolean(BOSS_MASS_SPELL_CAN_GRIEF);
    }

    public static void setNexusRule(MinecraftServer server, NexusType type, boolean value) {
        GameRules.Key<GameRules.BooleanValue> key = getNexusRuleKey(type);
        if (key != null) {
            server.getGameRules().getRule(key).set(value, server);
        }
    }

    public static boolean isNexusRuleActive(Level level, NexusType type) {
        if (level == null) return false;
        GameRules.Key<GameRules.BooleanValue> key = getNexusRuleKey(type);
        return key != null && level.getGameRules().getBoolean(key);
    }

    private static GameRules.Key<GameRules.BooleanValue> getNexusRuleKey(NexusType type) {
        return switch (type) {
            case BINDING  -> NEXUS_BINDING;
            case SCARCITY -> NEXUS_SCARCITY;
            case ENTROPY  -> NEXUS_ENTROPY;
            case FRAILTY  -> NEXUS_FRAILTY;
            case SILENCE  -> NEXUS_SILENCE;
        };
    }
}
