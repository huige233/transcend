package com.huige233.transcend.ascension;

import com.huige233.transcend.ascension.resource.ClassResourceData;
import com.huige233.transcend.ascension.resource.ClassResourceType;
import com.huige233.transcend.ascension.tree.NodeDefinition;
import com.huige233.transcend.ascension.tree.TreeDefinition;
import com.huige233.transcend.ascension.tree.TreeRegistry;
import com.huige233.transcend.spell.SpellElement;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 玩家飞升存档数据：进度/资源/试炼统计等。 */
public class PlayerAscensionData {

    private static final String T_CLASS        = "mage_class";
    private static final String T_MASTERY      = "mastery";
    private static final String T_RITUAL_TIER  = "ritual_tier";
    private static final String T_INSIGHT_LEVEL = "insight_level";
    private static final String T_INSIGHT_XP   = "insight_xp";

    private static final String T_STAGE        = "stage";
    private static final String T_RITUALS      = "rituals_done";
    private static final String T_NODES        = "unlocked_nodes";
    private static final String T_POINTS       = "talent_points";
    private static final String T_LEVEL        = "asc_level";
    private static final String T_XP           = "asc_xp";
    private static final String T_KILLS        = "total_kills";
    private static final String T_CASTS        = "total_casts";
    private static final String T_BOSS_KILLS   = "boss_kills";
    private static final String T_RITUAL_STATS = "ritual_stats";

    private static final String T_PRIMARY_VOW       = "primary_vow";
    private static final String T_SECONDARY_VOW     = "secondary_vow";
    private static final String T_TERTIARY_VOW      = "tertiary_vow";
    private static final String T_CAPSTONE_VOW      = "capstone_vow";

    private static final String T_LIBERATED_VOWS     = "liberated_vows";
    private static final String T_SECONDARY_MASTERY = "secondary_mastery";
    private static final String T_SEALED_ELEMENT    = "sealed_element";
    private static final String T_DISCOVERED_COMPS  = "discovered_components";
    private static final String T_CIRCLE_STABLE_SEC = "circle_stabilize_seconds";
    private static final String T_MAX_CIRCLES       = "max_concurrent_circles";
    private static final String T_ARENA_WAVE        = "highest_arena_wave";
    private static final String T_SCROLLS_CRAFTED   = "scrolls_crafted";
    private static final String T_TABLETS_RESTORED  = "tablets_restored";
    private static final String T_NEXUS_ACTIONS     = "nexus_actions";
    private static final String T_PEAK_THROUGHPUT   = "peak_mana_throughput";
    private static final String T_LAST_DEATH_SAVE   = "last_death_save_at";
    private static final String T_SOUL_MARKS        = "soul_marks";
    private static final String T_SOUL_ENERGY       = "soul_energy";
    private static final String T_ELEMENT_PACT      = "element_pact";
    private static final String T_DATA_VERSION      = "cultivation_data_version";
    private static final String T_REALM             = "cultivation_realm";
    private static final String T_MINOR_STAGE       = "cultivation_minor_stage";
    private static final String T_CULTIVATION_XP    = "cultivation_xp";
    private static final String T_PEAK_CULTIVATION_XP = "peak_cultivation_xp";
    private static final String T_AURA_GUARD        = "aura_guard";
    private static final String T_ELEMENT_RESISTANCES = "element_resistances";
    private static final String T_TRIBULATION_ACTIVE = "tribulation_active";
    private static final String T_TRIBULATION_TARGET = "tribulation_target";
    private static final String T_TRIBULATION_DIMENSION = "tribulation_dimension";
    private static final String T_TRIBULATION_CENTER_X = "tribulation_center_x";
    private static final String T_TRIBULATION_CENTER_Y = "tribulation_center_y";
    private static final String T_TRIBULATION_CENTER_Z = "tribulation_center_z";
    private static final String T_TRIBULATION_ELAPSED = "tribulation_elapsed";
    private static final String T_TRIBULATION_ROUNDS = "tribulation_rounds";
    private static final String T_DEVIATION_TICKS = "cultivation_deviation_ticks";

    private static final int CURRENT_DATA_VERSION = 1;

    public static final int MAX_LEVEL = 10;
    public static final long[] LEVEL_XP = {
            0, 100, 300, 700, 1500, 3000, 6000, 12000, 25000, 50000, 100000
    };
    public static final int POINTS_PER_LEVEL = 3;

    private MageClass mageClass    = MageClass.NONE;
    private ElementMastery mastery = ElementMastery.NONE;

    private int stage = 0;

    private final Set<String> completedRituals = new HashSet<>();

    private final Set<String> unlockedNodes = new HashSet<>();

    private int  talentPoints  = 0;
    private int  ascensionLevel = 0;
    private long ascensionXP   = 0;

    private int dataVersion = CURRENT_DATA_VERSION;
    private CultivationRealm cultivationRealm = CultivationRealm.SPIRIT_SENSING;
    private CultivationStage cultivationStage = CultivationStage.EARLY;
    private long cultivationXP = 0L;
    private long peakCultivationXP = 0L;
    private boolean auraGuardEnabled = false;
    private final ElementResistanceData elementResistances = new ElementResistanceData();
    private boolean tribulationActive;
    private CultivationRealm tribulationTarget = CultivationRealm.FOUNDATION;
    private String tribulationDimension = "";
    private double tribulationCenterX;
    private double tribulationCenterY;
    private double tribulationCenterZ;
    private int tribulationElapsedTicks;
    private int tribulationRounds;
    private int cultivationDeviationTicks;

    private long totalKills   = 0;
    private long totalCasts   = 0;
    private long bossKills    = 0;

    private final AscensionStatBlock ritualStats = new AscensionStatBlock();

    private String primaryVow   = "";
    private String secondaryVow = "";
    private String tertiaryVow  = "";
    private String capstoneVow  = "";

    private final Set<String> liberatedVows = new HashSet<>();

    private String secondaryMastery = "none";
    private String sealedElement    = "";

    private int  discoveredComponents    = 0;
    private long circleStabilizeSeconds  = 0;
    private int  maxConcurrentCircles    = 0;
    private int  highestArenaWave        = 0;
    private int  scrollsCrafted          = 0;
    private int  tabletsRestored         = 0;
    private int  nexusActions            = 0;
    private long peakManaThroughput      = 0;

    private long lastDeathSaveAt         = -10000L;

    private long soulEnergy              = 0L;

    private String elementPact          = "";

    private static final String T_VOW_IMM_DAMAGE   = "vow_imm_damage";
    private static final String T_VOW_DEV_HEALING  = "vow_dev_healing";
    private long vowImmortalityDamageEndured = 0L;
    private long vowDevotionHealingAccum     = 0L;

    private int  vowGreedFullManaTicks       = 0;
    private boolean vowSolitudeUsedAoE       = false;
    private final java.util.Set<String> vowResonanceCombatReactions = new java.util.HashSet<>();
    private long vowResonanceLastCombatTick  = 0L;

    private static final String T_CLASS_RESOURCE = "class_resource";
    private final ClassResourceData classResource = new ClassResourceData();

    private final List<SoulMark> soulMarks = new ArrayList<>();

    public record SoulMark(ResourceLocation dimension, BlockPos pos) {}

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(T_CLASS,   mageClass.id);
        tag.putString(T_MASTERY, mastery.id);
        tag.putInt   (T_RITUAL_TIER, stage);
        tag.putInt   (T_INSIGHT_LEVEL, ascensionLevel);
        tag.putLong  (T_INSIGHT_XP, ascensionXP);
        tag.putInt   (T_STAGE,   stage);
        tag.putInt   (T_POINTS,  talentPoints);
        tag.putInt   (T_LEVEL,   ascensionLevel);
        tag.putLong  (T_XP,      ascensionXP);
        tag.putLong  (T_KILLS,   totalKills);
        tag.putLong  (T_CASTS,   totalCasts);
        tag.putLong  (T_BOSS_KILLS, bossKills);
        tag.put      (T_RITUAL_STATS, ritualStats.save());
        tag.putInt   (T_DATA_VERSION, dataVersion);
        tag.putString(T_REALM, cultivationRealm.getId());
        tag.putString(T_MINOR_STAGE, cultivationStage.getId());
        tag.putLong  (T_CULTIVATION_XP, cultivationXP);
        tag.putLong  (T_PEAK_CULTIVATION_XP, peakCultivationXP);
        tag.putBoolean(T_AURA_GUARD, auraGuardEnabled);
        tag.put(T_ELEMENT_RESISTANCES, elementResistances.save());
        tag.putBoolean(T_TRIBULATION_ACTIVE, tribulationActive);
        tag.putString(T_TRIBULATION_TARGET, tribulationTarget.getId());
        tag.putString(T_TRIBULATION_DIMENSION, tribulationDimension);
        tag.putDouble(T_TRIBULATION_CENTER_X, tribulationCenterX);
        tag.putDouble(T_TRIBULATION_CENTER_Y, tribulationCenterY);
        tag.putDouble(T_TRIBULATION_CENTER_Z, tribulationCenterZ);
        tag.putInt(T_TRIBULATION_ELAPSED, tribulationElapsedTicks);
        tag.putInt(T_TRIBULATION_ROUNDS, tribulationRounds);
        tag.putInt(T_DEVIATION_TICKS, cultivationDeviationTicks);

        ListTag ritualList = new ListTag();
        for (String r : completedRituals) ritualList.add(StringTag.valueOf(r));
        tag.put(T_RITUALS, ritualList);

        ListTag nodeList = new ListTag();
        for (String n : unlockedNodes) nodeList.add(StringTag.valueOf(n));
        tag.put(T_NODES, nodeList);

        tag.putString(T_PRIMARY_VOW,       primaryVow);
        tag.putString(T_SECONDARY_VOW,     secondaryVow);
        tag.putString(T_TERTIARY_VOW,      tertiaryVow);
        tag.putString(T_CAPSTONE_VOW,      capstoneVow);
        tag.putString(T_SECONDARY_MASTERY, secondaryMastery);
        tag.putString(T_SEALED_ELEMENT,    sealedElement);

        ListTag liberatedList = new ListTag();
        for (String v : liberatedVows) liberatedList.add(StringTag.valueOf(v));
        tag.put(T_LIBERATED_VOWS, liberatedList);

        tag.putInt (T_DISCOVERED_COMPS,  discoveredComponents);
        tag.putLong(T_CIRCLE_STABLE_SEC, circleStabilizeSeconds);
        tag.putInt (T_MAX_CIRCLES,       maxConcurrentCircles);
        tag.putInt (T_ARENA_WAVE,        highestArenaWave);
        tag.putInt (T_SCROLLS_CRAFTED,   scrollsCrafted);
        tag.putInt (T_TABLETS_RESTORED,  tabletsRestored);
        tag.putInt (T_NEXUS_ACTIONS,     nexusActions);
        tag.putLong(T_PEAK_THROUGHPUT,   peakManaThroughput);
        tag.putLong(T_LAST_DEATH_SAVE,   lastDeathSaveAt);
        tag.putLong(T_SOUL_ENERGY,       soulEnergy);
        tag.putString(T_ELEMENT_PACT,    elementPact);

        tag.putLong(T_VOW_IMM_DAMAGE,    vowImmortalityDamageEndured);
        tag.putLong(T_VOW_DEV_HEALING,   vowDevotionHealingAccum);

        ListTag marksList = new ListTag();
        for (SoulMark sm : soulMarks) {
            if (sm.dimension() == null) continue;
            CompoundTag e = new CompoundTag();
            e.putString("dim", sm.dimension().toString());
            e.putInt("x", sm.pos().getX());
            e.putInt("y", sm.pos().getY());
            e.putInt("z", sm.pos().getZ());
            marksList.add(e);
        }
        tag.put(T_SOUL_MARKS, marksList);

        tag.put(T_CLASS_RESOURCE, classResource.save());

        return tag;
    }

    public void load(CompoundTag tag) {
        mageClass  = MageClass.getById(tag.getString(T_CLASS));
        mastery    = ElementMastery.getById(tag.getString(T_MASTERY));
        int legacyRitualTier = tag.getInt(T_STAGE);
        int legacyInsightLevel = tag.getInt(T_LEVEL);
        long legacyInsightXP = tag.getLong(T_XP);
        stage = Math.max(0, Math.min(4, tag.contains(T_RITUAL_TIER, Tag.TAG_ANY_NUMERIC)
                ? tag.getInt(T_RITUAL_TIER) : legacyRitualTier));
        talentPoints   = tag.getInt(T_POINTS);
        ascensionLevel = Math.max(0, Math.min(MAX_LEVEL,
                tag.contains(T_INSIGHT_LEVEL, Tag.TAG_ANY_NUMERIC)
                        ? tag.getInt(T_INSIGHT_LEVEL) : legacyInsightLevel));
        ascensionXP = Math.max(0L, tag.contains(T_INSIGHT_XP, Tag.TAG_ANY_NUMERIC)
                ? tag.getLong(T_INSIGHT_XP) : legacyInsightXP);
        totalKills     = tag.getLong(T_KILLS);
        totalCasts     = tag.getLong(T_CASTS);
        bossKills      = tag.getLong(T_BOSS_KILLS);

        if (tag.contains(T_REALM, Tag.TAG_STRING)) {
            dataVersion = Math.max(1, tag.getInt(T_DATA_VERSION));
            cultivationRealm = CultivationRealm.byId(tag.getString(T_REALM));
            cultivationStage = CultivationStage.byId(tag.getString(T_MINOR_STAGE));
            CultivationProgression.State progress = CultivationProgression.restore(
                    cultivationRealm.getRank(), cultivationStage, tag.getLong(T_CULTIVATION_XP),
                    tag.getLong(T_PEAK_CULTIVATION_XP));
            cultivationStage = progress.stage();
            cultivationXP = progress.xp();
            peakCultivationXP = progress.peakXp();

            auraGuardEnabled = false;
        } else {
            migrateLegacyCultivation(legacyRitualTier, legacyInsightLevel, legacyInsightXP);
        }
        if (tag.contains(T_ELEMENT_RESISTANCES, Tag.TAG_COMPOUND)) {
            elementResistances.load(tag.getCompound(T_ELEMENT_RESISTANCES));
        } else {
            elementResistances.clear();
        }
        tribulationActive = tag.getBoolean(T_TRIBULATION_ACTIVE);
        tribulationTarget = CultivationRealm.byId(tag.getString(T_TRIBULATION_TARGET));
        tribulationDimension = tag.getString(T_TRIBULATION_DIMENSION);
        tribulationCenterX = tag.getDouble(T_TRIBULATION_CENTER_X);
        tribulationCenterY = tag.getDouble(T_TRIBULATION_CENTER_Y);
        tribulationCenterZ = tag.getDouble(T_TRIBULATION_CENTER_Z);
        tribulationElapsedTicks = Math.max(0, tag.getInt(T_TRIBULATION_ELAPSED));
        tribulationRounds = Math.max(0, tag.getInt(T_TRIBULATION_ROUNDS));
        cultivationDeviationTicks = Math.max(0, tag.getInt(T_DEVIATION_TICKS));
        if (cultivationDeviationTicks > 0) auraGuardEnabled = false;

        if (tag.contains(T_RITUAL_STATS, Tag.TAG_COMPOUND))
            ritualStats.load(tag.getCompound(T_RITUAL_STATS));

        completedRituals.clear();
        ListTag rl = tag.getList(T_RITUALS, Tag.TAG_STRING);
        for (int i = 0; i < rl.size(); i++) completedRituals.add(rl.getString(i));

        unlockedNodes.clear();
        ListTag nl = tag.getList(T_NODES, Tag.TAG_STRING);
        for (int i = 0; i < nl.size(); i++) unlockedNodes.add(nl.getString(i));

        liberatedVows.clear();
        ListTag lv = tag.getList(T_LIBERATED_VOWS, Tag.TAG_STRING);
        for (int i = 0; i < lv.size(); i++) liberatedVows.add(lv.getString(i));

        primaryVow       = tag.contains(T_PRIMARY_VOW)       ? tag.getString(T_PRIMARY_VOW)       : "";
        secondaryVow     = tag.contains(T_SECONDARY_VOW)     ? tag.getString(T_SECONDARY_VOW)     : "";
        tertiaryVow      = tag.contains(T_TERTIARY_VOW)      ? tag.getString(T_TERTIARY_VOW)      : "";
        capstoneVow      = tag.contains(T_CAPSTONE_VOW)      ? tag.getString(T_CAPSTONE_VOW)      : "";
        secondaryMastery = tag.contains(T_SECONDARY_MASTERY) ? tag.getString(T_SECONDARY_MASTERY) : "none";
        sealedElement    = tag.contains(T_SEALED_ELEMENT)    ? tag.getString(T_SEALED_ELEMENT)    : "";

        discoveredComponents   = tag.contains(T_DISCOVERED_COMPS)  ? tag.getInt (T_DISCOVERED_COMPS)  : 0;
        circleStabilizeSeconds = tag.contains(T_CIRCLE_STABLE_SEC) ? tag.getLong(T_CIRCLE_STABLE_SEC) : 0L;
        maxConcurrentCircles   = tag.contains(T_MAX_CIRCLES)       ? tag.getInt (T_MAX_CIRCLES)       : 0;
        highestArenaWave       = tag.contains(T_ARENA_WAVE)        ? tag.getInt (T_ARENA_WAVE)        : 0;
        scrollsCrafted         = tag.contains(T_SCROLLS_CRAFTED)   ? tag.getInt (T_SCROLLS_CRAFTED)   : 0;
        tabletsRestored        = tag.contains(T_TABLETS_RESTORED)  ? tag.getInt (T_TABLETS_RESTORED)  : 0;
        nexusActions           = tag.contains(T_NEXUS_ACTIONS)     ? tag.getInt (T_NEXUS_ACTIONS)     : 0;
        peakManaThroughput     = tag.contains(T_PEAK_THROUGHPUT)   ? tag.getLong(T_PEAK_THROUGHPUT)   : 0L;
        lastDeathSaveAt        = tag.contains(T_LAST_DEATH_SAVE)   ? tag.getLong(T_LAST_DEATH_SAVE)   : -10000L;
        soulEnergy             = tag.contains(T_SOUL_ENERGY)        ? tag.getLong(T_SOUL_ENERGY)        : 0L;
        elementPact            = tag.contains(T_ELEMENT_PACT)       ? tag.getString(T_ELEMENT_PACT)     : "";

        vowImmortalityDamageEndured = tag.contains(T_VOW_IMM_DAMAGE)  ? tag.getLong(T_VOW_IMM_DAMAGE)  : 0L;
        vowDevotionHealingAccum     = tag.contains(T_VOW_DEV_HEALING) ? tag.getLong(T_VOW_DEV_HEALING) : 0L;

        soulMarks.clear();
        if (tag.contains(T_SOUL_MARKS, Tag.TAG_LIST)) {
            ListTag ml = tag.getList(T_SOUL_MARKS, Tag.TAG_COMPOUND);
            for (int i = 0; i < ml.size(); i++) {
                CompoundTag e = ml.getCompound(i);
                ResourceLocation dim = ResourceLocation.tryParse(e.getString("dim"));
                if (dim == null) continue;
                soulMarks.add(new SoulMark(dim,
                        new BlockPos(e.getInt("x"), e.getInt("y"), e.getInt("z"))));
            }
        }

        if (tag.contains(T_CLASS_RESOURCE, Tag.TAG_COMPOUND)) {
            classResource.load(tag.getCompound(T_CLASS_RESOURCE));
        } else {
            classResource.reset();
        }

    }

    public void copyFrom(PlayerAscensionData o) {
        load(o.save());
    }

    private void migrateLegacyCultivation(int legacyRitualTier, int legacyInsightLevel,
                                          long legacyInsightXP) {
        dataVersion = CURRENT_DATA_VERSION;
        cultivationRealm = CultivationRealm.fromLegacyStage(legacyRitualTier);
        CultivationProgression.State progress = CultivationProgression.restore(cultivationRealm.getRank(),
                CultivationStage.fromLegacyLevel(legacyInsightLevel), legacyInsightXP, legacyInsightXP);
        applyCultivationState(progress);
        auraGuardEnabled = false;
    }

    public CultivationRealm getCultivationRealm() { return cultivationRealm; }
    public CultivationStage getCultivationStage() { return cultivationStage; }
    public long getCultivationXP() { return cultivationXP; }
    public long getPeakCultivationXP() { return peakCultivationXP; }
    public long getCultivationXPRequired() {
        return CultivationProgression.xpRequired(cultivationRealm.getRank(), cultivationStage);
    }
    public boolean isReadyForTribulation() {
        return CultivationProgression.isReadyForTribulation(cultivationRealm.getRank(), cultivationState());
    }
    public int getSpellTier() { return cultivationRealm.getMaxSpellTier(); }
    public int getMaxSpellEffectSlots() { return cultivationRealm.getMaxEffectSlots(); }
    public boolean isAuraGuardEnabled() { return auraGuardEnabled; }
    public boolean hasCultivationDeviation() { return cultivationDeviationTicks > 0; }
    public int getCultivationDeviationTicks() { return cultivationDeviationTicks; }
    public ElementResistanceData getElementResistances() { return elementResistances; }
    public float getElementResistanceBonus(SpellElement element) {
        return elementResistances.getBonus(element);
    }

    public void setCultivationProgress(CultivationRealm realm, CultivationStage minorStage, long xp) {
        if (hasCultivationDeviation()) return;
        cultivationRealm = realm == null ? CultivationRealm.SPIRIT_SENSING : realm;
        CultivationProgression.State progress = CultivationProgression.restore(
                cultivationRealm.getRank(), minorStage, xp, xp);
        applyCultivationState(progress);
    }

    public void addCultivationXP(long amount) {
        if (amount <= 0L || hasCultivationDeviation()) return;
        applyCultivationState(CultivationProgression.addXp(
                cultivationRealm.getRank(), cultivationState(), amount));
    }

    public void losePeakCultivationXP(float fraction) {
        applyCultivationState(CultivationProgression.applyPeakXpLoss(cultivationState(), fraction));
    }

    private CultivationProgression.State cultivationState() {
        return new CultivationProgression.State(cultivationStage, cultivationXP, peakCultivationXP);
    }

    private void applyCultivationState(CultivationProgression.State state) {
        cultivationStage = state.stage();
        cultivationXP = state.xp();
        peakCultivationXP = state.peakXp();
    }

    public void setAuraGuardEnabled(boolean enabled) {
        auraGuardEnabled = enabled && !hasCultivationDeviation();
    }

    public void applyCultivationDeviation(int durationTicks) {
        cultivationDeviationTicks = Math.max(cultivationDeviationTicks, Math.max(0, durationTicks));
        auraGuardEnabled = false;
    }

    public boolean tickCultivationDeviation() {
        if (cultivationDeviationTicks <= 0) return false;
        cultivationDeviationTicks--;
        return cultivationDeviationTicks == 0;
    }

    public boolean isTribulationActive() { return tribulationActive; }
    public CultivationRealm getTribulationTarget() { return tribulationTarget; }
    public String getTribulationDimension() { return tribulationDimension; }
    public double getTribulationCenterX() { return tribulationCenterX; }
    public double getTribulationCenterY() { return tribulationCenterY; }
    public double getTribulationCenterZ() { return tribulationCenterZ; }
    public int getTribulationElapsedTicks() { return tribulationElapsedTicks; }
    public int getTribulationRounds() { return tribulationRounds; }

    public void beginTribulation(CultivationRealm target, String dimension, double x, double y, double z) {
        tribulationActive = true;
        tribulationTarget = target;
        tribulationDimension = dimension == null ? "" : dimension;
        tribulationCenterX = x;
        tribulationCenterY = y;
        tribulationCenterZ = z;
        tribulationElapsedTicks = 0;
        tribulationRounds = 0;
        auraGuardEnabled = false;
    }

    public void updateTribulationProgress(int elapsedTicks, int rounds) {
        tribulationElapsedTicks = Math.max(0, elapsedTicks);
        tribulationRounds = Math.max(0, rounds);
    }

    public void clearTribulation() {
        tribulationActive = false;
        tribulationElapsedTicks = 0;
        tribulationRounds = 0;
        tribulationDimension = "";
    }

    public ClassResourceData getClassResource() { return classResource; }

    public MageClass getMageClass() { return mageClass; }
    public boolean hasSelectedClass() { return mageClass.isSelected(); }

    public boolean selectClass(MageClass c) {
        if (stage < 1) return false;
        if (mageClass.isSelected()) return false;
        if (!c.isSelected()) return false;
        this.mageClass = c;
        if (!mastery.isSelected()) mastery = c.getCanonicalMastery();
        return true;
    }

    public void forceSetClass(MageClass c) {
        this.mageClass = c;
        if (c != null && c.isSelected() && !mastery.isSelected()) mastery = c.getCanonicalMastery();
    }

    public void forceSetRitualTier(int ritualTier) {
        this.stage = Math.max(0, Math.min(4, ritualTier));
    }

    @Deprecated
    public void forceSetStage(int stage) {
        forceSetRitualTier(stage);
    }

    public ElementMastery getMastery() { return mastery; }
    public boolean hasMastery() { return mastery.isSelected(); }

    public boolean selectMastery(ElementMastery m, boolean forceOverride) {
        if (stage < 1 && !forceOverride) return false;
        if (mastery.isSelected() && !forceOverride) return false;
        this.mastery = m;
        return true;
    }

    public int respec() {
        int refund = 0;
        for (String nodeId : unlockedNodes) {
            NodeDefinition node = TreeRegistry.getInstance().getNode(nodeId);
            if (node != null && TreeRegistry.getInstance().isNodeAuthorized(nodeId, mageClass)) {
                refund += node.getCost();
            }
        }
        unlockedNodes.clear();
        mastery = mageClass.getCanonicalMastery();
        elementPact = "";
        talentPoints += refund;
        return refund;
    }

    public int getRitualTier() { return stage; }

    @Deprecated
    public int getStage() { return getRitualTier(); }

    public boolean isRitualCompleted(AscensionRitual ritual) {
        return completedRituals.contains(ritual.name());
    }

    public boolean isRitualCompleted(String name) {
        return completedRituals.contains(name);
    }

    public boolean tryCompleteRitual(AscensionRitual ritual) {
        if (isRitualCompleted(ritual)) return false;
        if (ritual.stageIndex != stage) return false;
        if (!ritual.isMet(this)) return false;

        completedRituals.add(ritual.name());
        stage++;

        AscensionStatBlock reward = switch (ritual) {
            case AWAKENING      -> AscensionStatBlock.awakeningReward();
            case TEMPERING      -> AscensionStatBlock.temperingReward();
            case PURIFICATION   -> AscensionStatBlock.purificationReward();
            case TRANSCENDENCE  -> AscensionStatBlock.transcendenceReward();
        };
        ritualStats.addFrom(reward);

        return true;
    }

    public AscensionRitual getPendingRitual() {
        return AscensionRitual.getByStage(stage);
    }

    public Set<String> getCompletedRituals() { return Collections.unmodifiableSet(completedRituals); }

    public boolean isNodeUnlocked(String id)           { return unlockedNodes.contains(id); }
    public Set<String> getUnlockedNodes()              { return Collections.unmodifiableSet(unlockedNodes); }
    public boolean hasNode(String id)                  { return unlockedNodes.contains(id); }

    public boolean isTierUnlockedByStage(int tier) {
        int requiredRank = switch (tier) {
            case 0, 1 -> 1;
            case 2 -> 3;
            case 3 -> 5;
            case 4 -> 8;
            case 5 -> 11;
            default -> Integer.MAX_VALUE;
        };
        return cultivationRealm.getRank() >= requiredRank;
    }

    public boolean canUnlock(String nodeId) {
        if (unlockedNodes.contains(nodeId)) return false;
        TreeRegistry registry = TreeRegistry.getInstance();
        TreeDefinition owner = registry.getNodeOwner(nodeId);
        if (owner == null) return false;
        NodeDefinition node = owner.getNode(nodeId);
        if (node == null) return false;
        if (owner.getTreeType() == com.huige233.transcend.ascension.tree.TreeType.TALENT
                && owner.getMageClass() != mageClass) return false;
        if (!isTierUnlockedByStage(node.getTier())) return false;

        for (String prereq : node.getParents()) {
            if (!unlockedNodes.contains(prereq)) return false;
        }
        return true;
    }

    public boolean tryUnlockNode(String nodeId) {
        if (!canUnlock(nodeId)) return false;
        NodeDefinition node = TreeRegistry.getInstance().getNode(nodeId);
        if (node == null) return false;
        if (talentPoints < node.getCost()) return false;
        if (node.isTierFive()) {
            TreeDefinition tree = findTreeForNode(nodeId);
            if (tree != null) {
                for (NodeDefinition n : tree.getNodesForTier(5)) {
                    if (unlockedNodes.contains(n.getId())) return false;
                }
            }
        }
        talentPoints -= node.getCost();
        unlockedNodes.add(nodeId);
        return true;
    }

    private TreeDefinition findTreeForNode(String nodeId) {
        return TreeRegistry.getInstance().getNodeOwner(nodeId);
    }

    public int  getTalentPoints()    { return talentPoints; }
    public void addTalentPoints(int n) { talentPoints = Math.max(0, talentPoints + n); }

    public int getInsightLevel() { return ascensionLevel; }
    public long getInsightXP() { return ascensionXP; }

    @Deprecated
    public int getAscensionLevel() { return getInsightLevel(); }

    @Deprecated
    public long getAscensionXP() { return getInsightXP(); }

    public long getXPForCurrentLevel() {
        return ascensionLevel <= 0 ? 0 : LEVEL_XP[ascensionLevel];
    }
    public long getXPForNextLevel() {
        return ascensionLevel >= MAX_LEVEL ? Long.MAX_VALUE : LEVEL_XP[ascensionLevel + 1];
    }
    public float getLevelProgress() {
        if (ascensionLevel >= MAX_LEVEL) return 1f;
        long cur = LEVEL_XP[ascensionLevel], next = LEVEL_XP[ascensionLevel + 1];
        return (next <= cur) ? 1f : (float)(ascensionXP - cur) / (float)(next - cur);
    }

    public boolean addInsightXP(long amount) {
        if (amount <= 0L || ascensionLevel >= MAX_LEVEL) return false;
        ascensionXP = amount > Long.MAX_VALUE - ascensionXP
                ? Long.MAX_VALUE : ascensionXP + amount;
        boolean leveledUp = false;
        while (ascensionLevel < MAX_LEVEL && ascensionXP >= LEVEL_XP[ascensionLevel + 1]) {
            ascensionLevel++;
            talentPoints += POINTS_PER_LEVEL;
            leveledUp = true;
        }
        return leveledUp;
    }

    @Deprecated
    public boolean addAscensionXP(long amount) {
        return addInsightXP(amount);
    }

    public void setInsightXP(long newXP) {
        long oldLevel = ascensionLevel;
        ascensionXP = Math.max(0, newXP);

        ascensionLevel = 0;
        for (int lv = 1; lv <= MAX_LEVEL; lv++) {
            if (ascensionXP >= LEVEL_XP[lv]) ascensionLevel = lv;
            else break;
        }

        long levelDelta = ascensionLevel - oldLevel;
        if (levelDelta > 0) {
            talentPoints += (int) (levelDelta * POINTS_PER_LEVEL);
        } else if (levelDelta < 0) {

            talentPoints = Math.max(0, talentPoints + (int) (levelDelta * POINTS_PER_LEVEL));
        }
    }

    @Deprecated
    public void setAscensionXP(long newXP) {
        setInsightXP(newXP);
    }

    public void setInsightLevel(int newLevel) {
        newLevel = Math.max(0, Math.min(MAX_LEVEL, newLevel));
        int oldLevel = ascensionLevel;
        ascensionLevel = newLevel;
        ascensionXP = LEVEL_XP[newLevel];
        int levelDelta = newLevel - oldLevel;
        if (levelDelta > 0) {
            talentPoints += levelDelta * POINTS_PER_LEVEL;
        } else if (levelDelta < 0) {
            talentPoints = Math.max(0, talentPoints + levelDelta * POINTS_PER_LEVEL);
        }
    }

    @Deprecated
    public void setAscensionLevel(int newLevel) {
        setInsightLevel(newLevel);
    }

    public long getTotalKills()  { return totalKills; }
    public long getTotalCasts()  { return totalCasts; }
    public long getBossKills()   { return bossKills; }

    public void addKill(boolean isBoss) {
        totalKills++;
        if (isBoss) bossKills++;
    }

    public void addCast() { totalCasts++; }

    public AscensionStatBlock buildTotalStats() {
        AscensionStatBlock total = new AscensionStatBlock();
        total.addFrom(AscensionStatBlock.fromLevel(ascensionLevel, mageClass));
        total.addFrom(ritualStats);
        total.addFrom(TreeRegistry.getInstance().computeNodeStats(unlockedNodes, mageClass));
        total.addFrom(AscensionStatBlock.fromMastery(mastery));
        total.addFrom(buildVowStats());

        total.bonusMaxHealth                  = Math.min(total.bonusMaxHealth, 236f);
        total.bonusManaCapacity               = Math.min(total.bonusManaCapacity, 580);
        total.cooldownReduction               = Math.min(total.cooldownReduction, 0.35f);
        total.moveSpeedBonus                  = Math.min(total.moveSpeedBonus, 0.25f);
        total.critChance                      = Math.min(total.critChance, 0.75f);
        total.critMultiplier                  = Math.min(total.critMultiplier, 2.20f);
        total.spellPowerBonus                 = Math.min(total.spellPowerBonus, 0.25f);
        total.spellVamp                       = Math.min(total.spellVamp, 0.10f);
        total.incomingSpellDamageReduction    = Math.min(total.incomingSpellDamageReduction, 0.15f);
        total.damageReductionFlat             = Math.min(total.damageReductionFlat, 4f);
        total.resistanceIgnore                = Math.min(total.resistanceIgnore, 0.25f);
        total.manaCostReduction               = Math.min(total.manaCostReduction, 0.25f);

        total.armorPenetration                = Math.min(total.armorPenetration, 0.32f);
        total.reactionBonus                   = Math.min(total.reactionBonus, 4.00f);
        total.manaRegenBonus                  = Math.min(total.manaRegenBonus, 1.00f);
        total.lifesteal                       = Math.min(total.lifesteal, 0.10f);
        total.xpGainMult                      = Math.min(total.xpGainMult, 1.50f);
        total.dodgeChance                     = Math.min(total.dodgeChance, 0.15f);

        total.healingReceivedBonus            = Math.min(total.healingReceivedBonus, 0.35f);
        total.naturalRegenBonus               = Math.min(total.naturalRegenBonus, 1.00f);
        total.foodConsumptionReduction        = Math.min(total.foodConsumptionReduction, 0.40f);

        total.controlResistance               = Math.min(total.controlResistance, 0.35f);
        total.fallDamageReduction             = Math.min(total.fallDamageReduction, 0.75f);

        int realmSteps = Math.max(0, cultivationRealm.getRank() - 1);
        total.bonusMaxHealth += realmSteps * 4.0f;
        total.bonusManaCapacity += realmSteps * 20;
        total.spellPowerBonus += realmSteps * 0.03f;
        total.moveSpeedBonus += Math.min(0.11f, realmSteps * 0.01f);

        return total;
    }

    private AscensionStatBlock buildVowStats() {
        AscensionStatBlock v = new AscensionStatBlock();
        for (int stage = 1; stage <= 4; stage++) {
            String vowId = getVowForStage(stage);
            if (vowId == null || vowId.isEmpty()) continue;
            AscensionVow vow = VowRegistry.get(vowId);
            if (vow == null) continue;

            boolean lib = isVowLiberated(vowId);
            v.spellPowerBonus    += vow.spellDamageBonus(lib);
            v.bonusMaxHealth     += vow.healthAdd(lib);
            v.bonusManaCapacity  += (int) vow.manaCapAdd(lib);
            v.critChance         += vow.critChanceAdd(lib);
            v.cooldownReduction  += vow.cdrAdd(lib);
            v.reactionBonus      += vow.reactionBonus(lib);
            v.manaRegenBonus     += vow.manaRegenBonus(lib);
            float msMult = vow.moveSpeedMult(lib);
            if (msMult != 1.0f) {
                v.moveSpeedBonus += (msMult - 1.0f);
            }
            float cm = vow.critMultMin(lib);
            if (cm > v.critMultiplier) {
                v.critMultiplier = cm;
            }
        }
        return v;
    }

    public float getVowManaCostMult() {
        float mult = 1.0f;
        for (int stage = 1; stage <= 4; stage++) {
            String vowId = getVowForStage(stage);
            if (vowId == null || vowId.isEmpty()) continue;
            AscensionVow vow = VowRegistry.get(vowId);
            if (vow == null) continue;
            mult *= vow.manaCostMult(isVowLiberated(vowId));
        }
        return boundedVowMultiplier(mult);
    }

    public float getVowHealingMult() {
        float mult = 1.0f;
        for (int stage = 1; stage <= 4; stage++) {
            String vowId = getVowForStage(stage);
            if (vowId == null || vowId.isEmpty()) continue;
            AscensionVow vow = VowRegistry.get(vowId);
            if (vow == null) continue;
            mult *= vow.healingMult(isVowLiberated(vowId));
        }
        return boundedVowMultiplier(mult);
    }

    public float getVowCircleUpkeepMult() {
        float mult = 1.0f;
        for (int stage = 1; stage <= 4; stage++) {
            String vowId = getVowForStage(stage);
            if (vowId == null || vowId.isEmpty()) continue;
            AscensionVow vow = VowRegistry.get(vowId);
            if (vow == null) continue;
            mult *= vow.circleUpkeepMult(isVowLiberated(vowId));
        }
        return boundedVowMultiplier(mult);
    }

    public float getVowCooldownMult() {
        float mult = 1.0f;
        for (int stage = 1; stage <= 4; stage++) {
            String vowId = getVowForStage(stage);
            if (vowId == null || vowId.isEmpty()) continue;
            AscensionVow vow = VowRegistry.get(vowId);
            if (vow != null) mult *= vow.cooldownMult(isVowLiberated(vowId));
        }
        return boundedVowMultiplier(mult);
    }

    private static float boundedVowMultiplier(float multiplier) {
        return Float.isFinite(multiplier) ? Math.max(0.5F, Math.min(2.0F, multiplier)) : 1.0F;
    }

    public float getSpellDamageMultiplier(SpellElement element) {
        AscensionStatBlock stats = buildTotalStats();
        float masteryBonus = mastery.getDamageBonus(element);
        float pactBonus = getElementPactDamageBonus(element);
        return 1.0f + stats.spellPowerBonus + masteryBonus + pactBonus;
    }

    public float getSpellDamageMultiplier(SpellElement element, net.minecraft.world.entity.player.Player player) {
        float attrBonus = (float) player.getAttributeValue(com.huige233.transcend.TranscendAttributes.SPELL_POWER.get());
        float masteryBonus = mastery.getDamageBonus(element);
        float pactBonus = getElementPactDamageBonus(element);
        return 1.0f + attrBonus + masteryBonus + pactBonus;
    }

    public float getEffectiveCDR() {
        return buildTotalStats().getEffectiveCDR();
    }

    public float getManaCostReduction(SpellElement element) {
        AscensionStatBlock stats = buildTotalStats();
        float fromStat = stats.getEffectiveManaCostReduction();
        float fromMastery = mastery.getManaCostReduction(element);
        float fromPact = getElementPactCostReduction(element);

        return Math.min(0.25f, fromStat + fromMastery + fromPact);
    }

    public AscensionStatBlock getRitualStats() { return ritualStats; }

    public String getPrimaryVow()   { return primaryVow;   }
    public String getSecondaryVow() { return secondaryVow; }
    public String getTertiaryVow()  { return tertiaryVow;  }
    public String getCapstoneVow()  { return capstoneVow;  }

    public void setPrimaryVow  (String v) { this.primaryVow   = v == null ? "" : v; }
    public void setSecondaryVow(String v) { this.secondaryVow = v == null ? "" : v; }
    public void setTertiaryVow (String v) { this.tertiaryVow  = v == null ? "" : v; }
    public void setCapstoneVow (String v) { this.capstoneVow  = v == null ? "" : v; }

    public boolean hasVowForStage(int stage) {
        return switch (stage) {
            case 1 -> !primaryVow.isEmpty();
            case 2 -> !secondaryVow.isEmpty();
            case 3 -> !tertiaryVow.isEmpty();
            case 4 -> !capstoneVow.isEmpty();
            default -> false;
        };
    }

    public String getVowForStage(int stage) {
        return switch (stage) {
            case 1 -> primaryVow;
            case 2 -> secondaryVow;
            case 3 -> tertiaryVow;
            case 4 -> capstoneVow;
            default -> "";
        };
    }

    public void setVowForStage(int stage, String vowId) {
        String current = getVowForStage(stage);
        if (current != null && !current.isEmpty() && isVowLiberated(current)) {
            return;
        }
        String v = vowId == null ? "" : vowId;
        switch (stage) {
            case 1 -> primaryVow   = v;
            case 2 -> secondaryVow = v;
            case 3 -> tertiaryVow  = v;
            case 4 -> capstoneVow  = v;
            default -> {}
        }
    }

    public boolean isVowLiberated(String vowId) {
        return vowId != null && !vowId.isEmpty() && liberatedVows.contains(vowId);
    }

    public boolean liberateVow(String vowId) {
        if (vowId == null || vowId.isEmpty()) return false;
        return liberatedVows.add(vowId);
    }

    public java.util.Set<String> getLiberatedVows() {
        return Collections.unmodifiableSet(liberatedVows);
    }

    public String getSecondaryMastery() { return secondaryMastery; }
    public void   setSecondaryMastery(String m) { this.secondaryMastery = m == null ? "none" : m; }

    public String getSealedElement() { return sealedElement; }
    public void   setSealedElement(String e) { this.sealedElement = e == null ? "" : e; }

    public int  getDiscoveredComponents()   { return discoveredComponents; }
    public long getCircleStabilizeSeconds() { return circleStabilizeSeconds; }
    public int  getMaxConcurrentCircles()   { return maxConcurrentCircles; }
    public int  getHighestArenaWave()       { return highestArenaWave; }
    public int  getScrollsCrafted()         { return scrollsCrafted; }
    public int  getTabletsRestored()        { return tabletsRestored; }
    public int  getNexusActions()           { return nexusActions; }
    public long getPeakManaThroughput()     { return peakManaThroughput; }

    public void setDiscoveredComponents(int v)    { this.discoveredComponents   = v; }
    public void setCircleStabilizeSeconds(long v) { this.circleStabilizeSeconds = v; }
    public void setMaxConcurrentCircles(int v)    { this.maxConcurrentCircles   = v; }
    public void setHighestArenaWave(int v)        { this.highestArenaWave       = v; }
    public void setScrollsCrafted(int v)          { this.scrollsCrafted         = v; }
    public void setTabletsRestored(int v)         { this.tabletsRestored        = v; }
    public void setNexusActions(int v)            { this.nexusActions           = v; }
    public void setPeakManaThroughput(long v)     { this.peakManaThroughput     = v; }

    public void incrementDiscoveredComponents() { discoveredComponents++; }
    public void incrementScrollsCrafted()       { scrollsCrafted++; }
    public void incrementTabletsRestored()      { tabletsRestored++; }
    public void incrementNexusActions()         { nexusActions++; }

    public void updateCircleStabilizeSeconds(long seconds) { circleStabilizeSeconds = Math.max(circleStabilizeSeconds, seconds); }
    public void updateMaxConcurrentCircles(int count)      { maxConcurrentCircles   = Math.max(maxConcurrentCircles,   count); }
    public void updateHighestArenaWave(int wave)           { highestArenaWave       = Math.max(highestArenaWave,       wave); }
    public void updatePeakManaThroughput(long throughput)  { peakManaThroughput     = Math.max(peakManaThroughput,     throughput); }

    public long getLastDeathSaveAt() { return lastDeathSaveAt; }
    public void setLastDeathSaveAt(long tick) { this.lastDeathSaveAt = tick; }

    public long getSoulEnergy() { return soulEnergy; }

    public long getMaxSoulEnergy() {
        return switch (stage) {
            case 1 -> 50L;
            case 2 -> 200L;
            case 3 -> 500L;
            case 4 -> 2000L;
            default -> 0L;
        };
    }

    public long addSoulEnergy(long amount) {
        if (amount <= 0) return 0L;
        if (stage <= 0) return 0L;
        long max = getMaxSoulEnergy();
        long before = soulEnergy;
        soulEnergy = Math.min(max, soulEnergy + amount);
        return soulEnergy - before;
    }

    public boolean consumeSoulEnergy(long cost) {
        if (cost <= 0) return true;
        if (soulEnergy < cost) return false;
        soulEnergy -= cost;
        return true;
    }

    public void setSoulEnergy(long value) {
        if (value < 0) value = 0;
        long max = getMaxSoulEnergy();
        soulEnergy = Math.min(max, value);
    }

    public List<SoulMark> getSoulMarks() { return Collections.unmodifiableList(soulMarks); }

    public int getMaxSoulMarks() { return Math.max(0, stage); }

    public boolean addSoulMark(@Nullable ResourceLocation dim, BlockPos pos) {
        if (dim == null) return false;
        if (stage <= 0) return false;
        for (SoulMark sm : soulMarks) {
            if (sm.dimension().equals(dim) && sm.pos().equals(pos)) return false;
        }
        int max = getMaxSoulMarks();
        while (soulMarks.size() >= max) {
            soulMarks.remove(0);
        }
        soulMarks.add(new SoulMark(dim, pos));
        return true;
    }

    public boolean removeSoulMark(@Nullable ResourceLocation dim, BlockPos pos) {
        if (dim == null) return false;
        return soulMarks.removeIf(sm -> sm.dimension().equals(dim) && sm.pos().equals(pos));
    }

    @Nullable
    public SoulMark findNearestSoulMark(@Nullable ResourceLocation dim, BlockPos near) {
        if (dim == null) return null;
        SoulMark best = null;
        double bestSq = Double.MAX_VALUE;
        for (SoulMark sm : soulMarks) {
            if (!sm.dimension().equals(dim)) continue;
            double d = sm.pos().distSqr(near);
            if (d < bestSq) {
                bestSq = d;
                best = sm;
            }
        }
        return best;
    }

    @Nullable
    public SpellElement getElementPact() {
        if (elementPact == null || elementPact.isEmpty()) return null;
        return SpellElement.getById(elementPact);
    }

    public String getElementPactId() {
        return elementPact == null ? "" : elementPact;
    }

    public boolean hasElementPact() {
        return elementPact != null && !elementPact.isEmpty();
    }

    public boolean bindElementPact(SpellElement element) {
        if (element == null) return false;
        if (stage < 2) return false;
        if (hasElementPact()) return false;
        this.elementPact = element.id;
        return true;
    }

    public void forceSetElementPact(@Nullable SpellElement element) {
        this.elementPact = (element == null) ? "" : element.id;
    }

    public float getElementPactDamageBonus(@Nullable SpellElement element) {
        SpellElement pact = getElementPact();
        if (pact == null || element == null) return 0f;
        return (pact == element) ? 0.25f : -0.10f;
    }

    public float getElementPactCostReduction(@Nullable SpellElement element) {
        SpellElement pact = getElementPact();
        if (pact == null || element == null) return 0f;
        return (pact == element) ? 0.20f : 0f;
    }

    @Nullable
    public AscensionVow getActiveTertiaryVow() {
        if (tertiaryVow == null || tertiaryVow.isEmpty()) return null;
        return VowRegistry.get(tertiaryVow);
    }

    public long getVowImmortalityDamageEndured() { return vowImmortalityDamageEndured; }
    public void addVowImmortalityDamage(float amount) {
        if (amount > 0) vowImmortalityDamageEndured += (long) amount;
    }
    public void resetVowImmortalityDamage() { vowImmortalityDamageEndured = 0L; }

    public int  getVowGreedFullManaTicks() { return vowGreedFullManaTicks; }
    public void tickVowGreedFull()         { vowGreedFullManaTicks++; }
    public void resetVowGreedFullManaTicks() { vowGreedFullManaTicks = 0; }

    public boolean isVowSolitudeUsedAoE() { return vowSolitudeUsedAoE; }
    public void markVowSolitudeUsedAoE()  { vowSolitudeUsedAoE = true; }
    public void resetVowSolitudeForBoss() { vowSolitudeUsedAoE = false; }

    public java.util.Set<String> getVowResonanceCombatReactions() {
        return java.util.Collections.unmodifiableSet(vowResonanceCombatReactions);
    }
    public void addVowResonanceCombatReaction(String reactionName) {
        vowResonanceCombatReactions.add(reactionName);
    }
    public long getVowResonanceLastCombatTick() { return vowResonanceLastCombatTick; }
    public void updateVowResonanceLastCombatTick(long tick) {
        vowResonanceLastCombatTick = tick;
    }
    public void resetVowResonanceCombat() {
        vowResonanceCombatReactions.clear();
        vowResonanceLastCombatTick = 0L;
    }

    public long getVowDevotionHealingAccum() { return vowDevotionHealingAccum; }
    public void addVowDevotionHealing(float amount) {
        if (amount > 0) vowDevotionHealingAccum += (long) amount;
    }
}
