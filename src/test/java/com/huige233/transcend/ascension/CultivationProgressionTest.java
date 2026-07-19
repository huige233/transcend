package com.huige233.transcend.ascension;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class CultivationProgressionTest {
    @Test
    void thresholdsScaleByRankAndMinorStage() {
        assertEquals(500L, CultivationProgression.xpRequired(1, CultivationStage.EARLY));
        assertEquals(3000L, CultivationProgression.xpRequired(2, CultivationStage.MIDDLE));
        assertEquals(144000L, CultivationProgression.xpRequired(12, CultivationStage.LATE));
    }

    @Test
    void hostileKillXpIsHealthScaledAndBounded() {
        assertEquals(1L, CultivationProgression.killXp(1.0F, false));
        assertEquals(3L, CultivationProgression.killXp(21.0F, false));
        assertEquals(100L, CultivationProgression.killXp(5000.0F, false));
        assertEquals(500L, CultivationProgression.killXp(5000.0F, true));
    }

    @Test
    void gainsAdvanceStagesCarryOverflowAndCapLate() {
        CultivationProgression.State state = new CultivationProgression.State(
                CultivationStage.EARLY, 450L, 450L);
        state = CultivationProgression.addXp(1, state, 900L);

        assertEquals(CultivationStage.LATE, state.stage());
        assertEquals(100L, state.xp());
        assertEquals(100L, state.peakXp());

        state = CultivationProgression.addXp(1, state, 5000L);
        assertEquals(1000L, state.xp());
        assertEquals(1000L, state.peakXp());
    }

    @Test
    void readinessRequiresFullLateStage() {
        assertFalse(CultivationProgression.isReadyForTribulation(2,
                new CultivationProgression.State(CultivationStage.LATE, 3999L, 3999L)));
        assertTrue(CultivationProgression.isReadyForTribulation(2,
                new CultivationProgression.State(CultivationStage.LATE, 4000L, 4000L)));
    }

    @Test
    void failureLossUsesCurrentStagePeakAndRetainsIt() {
        CultivationProgression.State failed = CultivationProgression.applyPeakXpLoss(
                new CultivationProgression.State(CultivationStage.LATE, 800L, 1000L), 0.20F);

        assertEquals(600L, failed.xp());
        assertEquals(1000L, failed.peakXp());
    }

    @Test
    void playerProgressAndRitualRewardsSurviveSaveLoad() {
        PlayerAscensionData original = new PlayerAscensionData();
        original.setCultivationProgress(CultivationRealm.GOLDEN_CORE, CultivationStage.MIDDLE, 1000L);
        original.addCultivationXP(250L);
        AscensionStatBlock rewards = original.getRitualStats();
        rewards.healingReceivedBonus = 0.35F;
        rewards.naturalRegenBonus = 1.0F;
        rewards.foodConsumptionReduction = 0.40F;
        rewards.deathSaveEnabled = 1.0F;
        rewards.controlResistance = 0.35F;
        rewards.fallDamageReduction = 0.75F;

        PlayerAscensionData loaded = new PlayerAscensionData();
        loaded.load(original.save());

        assertEquals(CultivationRealm.GOLDEN_CORE, loaded.getCultivationRealm());
        assertEquals(CultivationStage.MIDDLE, loaded.getCultivationStage());
        assertEquals(1250L, loaded.getCultivationXP());
        assertEquals(1250L, loaded.getPeakCultivationXP());
        assertEquals(0.35F, loaded.getRitualStats().healingReceivedBonus);
        assertEquals(1.0F, loaded.getRitualStats().naturalRegenBonus);
        assertEquals(0.40F, loaded.getRitualStats().foodConsumptionReduction);
        assertEquals(1.0F, loaded.getRitualStats().deathSaveEnabled);
        assertEquals(0.35F, loaded.getRitualStats().controlResistance);
        assertEquals(0.75F, loaded.getRitualStats().fallDamageReduction);
    }

    @Test
    void threeProgressionAxesExposeExplicitApisAndDeprecateAmbiguousAccessors() {
        assertAll("explicit progression APIs",
                () -> assertPublicAccessor("getRitualTier", int.class),
                () -> assertPublicAccessor("getInsightLevel", int.class),
                () -> assertPublicAccessor("getInsightXP", long.class),
                () -> assertPublicAccessor("getCultivationRealm", CultivationRealm.class),
                () -> assertPublicAccessor("getCultivationStage", CultivationStage.class),
                () -> assertPublicAccessor("getCultivationXP", long.class),
                () -> assertPublicAccessor("getCultivationXPRequired", long.class),
                () -> assertPublicAccessor("isReadyForTribulation", boolean.class));

        assertAll("legacy accessors remain callable migration aliases but are deprecated",
                () -> assertDeprecatedAccessor("getStage"),
                () -> assertDeprecatedAccessor("getAscensionLevel"),
                () -> assertDeprecatedAccessor("getAscensionXP"));
    }

    @Test
    void modernThreeAxisNbtRoundTripsWithoutCollapsingIndependentProgress() {
        CompoundTag modern = new CompoundTag();
        modern.putInt("ritual_tier", 2);
        modern.putInt("insight_level", 8);
        modern.putLong("insight_xp", 30_000L);
        modern.putInt("cultivation_data_version", 1);
        modern.putString("cultivation_realm", "heaven_immortal");
        modern.putString("cultivation_minor_stage", "middle");
        modern.putLong("cultivation_xp", 12_000L);
        modern.putLong("peak_cultivation_xp", 12_500L);

        PlayerAscensionData loaded = new PlayerAscensionData();
        loaded.load(modern);
        assertThreeAxes(loaded, 2, 8, 30_000L,
                CultivationRealm.HEAVEN_IMMORTAL, CultivationStage.MIDDLE, 12_000L);

        CompoundTag saved = loaded.save();
        assertModernAndCompatibilityKeys(saved, 2, 8, 30_000L);
        PlayerAscensionData roundTripped = new PlayerAscensionData();
        roundTripped.load(saved);
        assertThreeAxes(roundTripped, 2, 8, 30_000L,
                CultivationRealm.HEAVEN_IMMORTAL, CultivationStage.MIDDLE, 12_000L);
    }

    @Test
    void legacyNbtInitializesCultivationAndSurvivesThreeAxisRoundTrip() {
        CompoundTag legacy = new CompoundTag();
        legacy.putInt("stage", 4);
        legacy.putInt("asc_level", 7);
        legacy.putLong("asc_xp", 22_000L);

        PlayerAscensionData migrated = new PlayerAscensionData();
        migrated.load(legacy);
        assertThreeAxes(migrated, 4, 7, 22_000L,
                CultivationRealm.SPIRIT_TRANSFORMATION, CultivationStage.LATE, 22_000L);

        CompoundTag saved = migrated.save();
        assertModernAndCompatibilityKeys(saved, 4, 7, 22_000L);
        assertEquals("spirit_transformation", saved.getString("cultivation_realm"));
        assertEquals("late", saved.getString("cultivation_minor_stage"));
        assertEquals(22_000L, saved.getLong("cultivation_xp"));

        PlayerAscensionData roundTripped = new PlayerAscensionData();
        roundTripped.load(saved);
        assertThreeAxes(roundTripped, 4, 7, 22_000L,
                CultivationRealm.SPIRIT_TRANSFORMATION, CultivationStage.LATE, 22_000L);
    }

    private static void assertPublicAccessor(String name, Class<?> returnType) {
        Method method = requireMethod(name);
        assertTrue(Modifier.isPublic(method.getModifiers()), name + " must be public");
        assertEquals(returnType, method.getReturnType(), name + " return type");
    }

    private static void assertDeprecatedAccessor(String name) {
        assertTrue(requireMethod(name).isAnnotationPresent(Deprecated.class),
                name + " must remain as a deprecated compatibility accessor");
    }

    private static Method requireMethod(String name) {
        try {
            return PlayerAscensionData.class.getMethod(name);
        } catch (NoSuchMethodException exception) {
            return fail("PlayerAscensionData must expose " + name + "()", exception);
        }
    }

    private static long invokeNumber(PlayerAscensionData data, String methodName) {
        try {
            return ((Number) requireMethod(methodName).invoke(data)).longValue();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Cannot invoke PlayerAscensionData." + methodName + "()", exception);
        }
    }

    private static void assertThreeAxes(PlayerAscensionData data, int ritualTier,
                                        int insightLevel, long insightXp,
                                        CultivationRealm realm, CultivationStage stage,
                                        long cultivationXp) {
        assertAll("independent ritual, insight, and cultivation progress",
                () -> assertEquals(ritualTier, invokeNumber(data, "getRitualTier"), "ritual tier"),
                () -> assertEquals(insightLevel, invokeNumber(data, "getInsightLevel"), "insight level"),
                () -> assertEquals(insightXp, invokeNumber(data, "getInsightXP"), "insight XP"),
                () -> assertEquals(realm, data.getCultivationRealm(), "cultivation realm"),
                () -> assertEquals(stage, data.getCultivationStage(), "minor stage"),
                () -> assertEquals(cultivationXp, data.getCultivationXP(), "cultivation XP"));
    }

    private static void assertModernAndCompatibilityKeys(CompoundTag tag, int ritualTier,
                                                          int insightLevel, long insightXp) {
        assertAll("modern keys and legacy compatibility keys",
                () -> assertEquals(ritualTier, tag.getInt("ritual_tier"), "modern ritual tier"),
                () -> assertEquals(insightLevel, tag.getInt("insight_level"), "modern insight level"),
                () -> assertEquals(insightXp, tag.getLong("insight_xp"), "modern insight XP"),
                () -> assertEquals(ritualTier, tag.getInt("stage"), "legacy ritual tier"),
                () -> assertEquals(insightLevel, tag.getInt("asc_level"), "legacy insight level"),
                () -> assertEquals(insightXp, tag.getLong("asc_xp"), "legacy insight XP"));
    }
}
