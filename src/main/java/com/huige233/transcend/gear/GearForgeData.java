package com.huige233.transcend.gear;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GearForgeData {

    public static final String ROOT_TAG = "transcend_forge_data";

    public static final int MAX_RESONANCE_SOCKETS = 4;
    public static final int MAX_SOUL_ECHOES       = 3;
    public static final int MAX_EXPERIENCE_TIER   = 3;

    private static final String K_TIER       = "tier";
    private static final String K_CRUCIBLE   = "crucible";
    private static final String K_RESONANCE  = "resonance";
    private static final String K_SOUL       = "soul";
    private static final String K_EXPERIENCE = "experience";
    private static final String K_CELESTIAL  = "celestial";

    private static final String K_TRIGGER    = "trigger";

    private static final String K_TRIGGER_CD = "trigger_cd";

    private static final String K_HARMONIC_STACKS = "harmonic_stacks";

    public static final long[] WEAPON_KILL_THRESHOLDS = { 100L, 400L, 1000L };

    public static final long[] TOOL_BLOCK_THRESHOLDS  = { 1000L, 4000L, 10000L };

    public static final long[] ARMOR_HIT_THRESHOLDS   = { 50L, 200L, 500L };

    private GearForgeData() {}

    public record CrucibleData(String aspect, float offset, String processId) {}
    public record ResonanceSocket(String crystalId, int level) {}
    public record SoulEcho(String mobId, String echoType, int tier) {}
    public record ExperienceData(long kills, long casts, long blocks, long hits, int tier) {}
    public record CelestialBlessing(String blessing, int moonPhase, String biomeClass) {}

    public record TriggerAffixData(String affixId) {}

    public static boolean isEligibleForPipeline(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) return false;
        return GearCategory.classify(stack) != GearCategory.OTHER;
    }

    public static boolean isInPipeline(ItemStack stack) {
        return getTier(stack) > 0;
    }

    public static int getTier(ItemStack stack) {
        CompoundTag root = readRoot(stack);
        return root == null ? 0 : root.getInt(K_TIER);
    }

    public static boolean canEnterStage(ItemStack stack, ForgeStage stage) {
        if (!isEligibleForPipeline(stack)) return false;
        if (isStageWritten(stack, stage)) return false;
        if (stage == ForgeStage.CRUCIBLE) return true;
        return isStageWritten(stack, ForgeStage.CRUCIBLE);
    }

    public static boolean isStageWritten(ItemStack stack, ForgeStage stage) {
        CompoundTag root = readRoot(stack);
        if (root == null) return false;
        return switch (stage) {
            case CRUCIBLE  -> root.contains(K_CRUCIBLE,   Tag.TAG_COMPOUND);
            case RESONANCE -> root.contains(K_RESONANCE,  Tag.TAG_LIST)
                              && !root.getList(K_RESONANCE, Tag.TAG_COMPOUND).isEmpty();
            case SOUL      -> root.contains(K_SOUL, Tag.TAG_LIST)
                              && !root.getList(K_SOUL, Tag.TAG_COMPOUND).isEmpty();
            case EXPERIENCE -> {
                CompoundTag exp = root.getCompound(K_EXPERIENCE);
                yield exp.getInt("tier") > 0;
            }
            case CELESTIAL -> root.contains(K_CELESTIAL, Tag.TAG_COMPOUND);
        };
    }

    public static boolean writeCrucible(ItemStack stack, String aspect, float offset, String processId) {
        if (!canEnterStage(stack, ForgeStage.CRUCIBLE)) return false;
        CompoundTag root = getOrCreateRoot(stack);
        CompoundTag c = new CompoundTag();
        c.putString("aspect", aspect == null ? "" : aspect);
        c.putFloat("offset", offset);
        c.putString("process_id", processId == null ? "" : processId);
        root.put(K_CRUCIBLE, c);
        bumpTier(root);
        return true;
    }

    public static boolean addResonanceSocket(ItemStack stack, String crystalId, int level) {
        if (!isEligibleForPipeline(stack)) return false;
        if (!isStageWritten(stack, ForgeStage.CRUCIBLE)) return false;
        CompoundTag root = getOrCreateRoot(stack);
        ListTag list = root.getList(K_RESONANCE, Tag.TAG_COMPOUND);
        if (list.size() >= MAX_RESONANCE_SOCKETS) return false;
        boolean wasEmpty = list.isEmpty();
        CompoundTag socket = new CompoundTag();
        socket.putString("crystal", crystalId == null ? "" : crystalId);
        socket.putInt("lvl", level);
        list.add(socket);
        root.put(K_RESONANCE, list);
        if (wasEmpty) bumpTier(root);
        return true;
    }

    public static boolean addSoulEcho(ItemStack stack, String mobId, String echoType, int tier) {
        if (!isEligibleForPipeline(stack)) return false;
        if (!isStageWritten(stack, ForgeStage.CRUCIBLE)) return false;
        CompoundTag root = getOrCreateRoot(stack);
        ListTag list = root.getList(K_SOUL, Tag.TAG_COMPOUND);
        if (list.size() >= MAX_SOUL_ECHOES) return false;
        boolean wasEmpty = list.isEmpty();
        CompoundTag echo = new CompoundTag();
        echo.putString("mob", mobId == null ? "" : mobId);
        echo.putString("echo", echoType == null ? "" : echoType);
        echo.putInt("tier", tier);
        list.add(echo);
        root.put(K_SOUL, list);
        if (wasEmpty) bumpTier(root);
        return true;
    }

    public static void incrementExperience(ItemStack stack, long deltaKills, long deltaCasts,
                                            long deltaBlocks, long deltaHits) {
        if (!isEligibleForPipeline(stack)) return;
        if (!isStageWritten(stack, ForgeStage.CRUCIBLE)) return;
        CompoundTag root = getOrCreateRoot(stack);
        CompoundTag exp = root.getCompound(K_EXPERIENCE);
        if (deltaKills  > 0) exp.putLong("kills",  exp.getLong("kills")  + deltaKills);
        if (deltaCasts  > 0) exp.putLong("casts",  exp.getLong("casts")  + deltaCasts);
        if (deltaBlocks > 0) exp.putLong("blocks", exp.getLong("blocks") + deltaBlocks);
        if (deltaHits   > 0) exp.putLong("hits",   exp.getLong("hits")   + deltaHits);
        root.put(K_EXPERIENCE, exp);
    }

    public static int upgradeExperienceTier(ItemStack stack) {
        if (!isEligibleForPipeline(stack)) return 0;
        if (!isStageWritten(stack, ForgeStage.CRUCIBLE)) return 0;
        CompoundTag root = getOrCreateRoot(stack);
        CompoundTag exp = root.getCompound(K_EXPERIENCE);
        int curTier = exp.getInt("tier");
        if (curTier >= MAX_EXPERIENCE_TIER) return curTier;

        long countForCategory = switch (GearCategory.classify(stack)) {
            case WEAPON -> exp.getLong("kills");
            case TOOL   -> exp.getLong("blocks");
            case ARMOR  -> exp.getLong("hits");
            case OTHER  -> 0L;
        };
        long[] thresholds = thresholdsFor(GearCategory.classify(stack));

        int newTier = curTier;
        for (int i = curTier; i < MAX_EXPERIENCE_TIER; i++) {
            if (countForCategory >= thresholds[i]) {
                newTier = i + 1;
            } else break;
        }
        if (newTier > curTier) {
            exp.putInt("tier", newTier);
            root.put(K_EXPERIENCE, exp);
            if (curTier == 0) bumpTier(root);
        }
        return newTier;
    }

    public static boolean writeCelestial(ItemStack stack, String blessing, int moonPhase, String biomeClass) {
        if (!canEnterStage(stack, ForgeStage.CELESTIAL)) return false;
        CompoundTag root = getOrCreateRoot(stack);
        CompoundTag d = new CompoundTag();
        d.putString("blessing", blessing == null ? "" : blessing);
        d.putInt("phase", moonPhase);
        d.putString("biome", biomeClass == null ? "" : biomeClass);
        root.put(K_CELESTIAL, d);
        bumpTier(root);
        return true;
    }

    public static boolean writeTriggerAffix(ItemStack stack, String affixId) {
        if (!isEligibleForPipeline(stack)) return false;
        if (hasTriggerAffix(stack)) return false;
        if (affixId == null || affixId.isEmpty()) return false;
        CompoundTag root = getOrCreateRoot(stack);
        CompoundTag t = new CompoundTag();
        t.putString("affix", affixId);
        root.put(K_TRIGGER, t);
        return true;
    }

    public static boolean hasTriggerAffix(ItemStack stack) {
        CompoundTag root = readRoot(stack);
        return root != null && root.contains(K_TRIGGER, Tag.TAG_COMPOUND);
    }

    @Nullable
    public static TriggerAffixData getTriggerAffix(ItemStack stack) {
        CompoundTag root = readRoot(stack);
        if (root == null || !root.contains(K_TRIGGER, Tag.TAG_COMPOUND)) return null;
        CompoundTag t = root.getCompound(K_TRIGGER);
        return new TriggerAffixData(t.getString("affix"));
    }

    public static long getTriggerCd(ItemStack stack, String affixId) {
        CompoundTag root = readRoot(stack);
        if (root == null || !root.contains(K_TRIGGER_CD, Tag.TAG_COMPOUND)) return 0L;
        return root.getCompound(K_TRIGGER_CD).getLong(affixId);
    }

    public static void setTriggerCd(ItemStack stack, String affixId, long tick) {
        CompoundTag root = getOrCreateRoot(stack);
        CompoundTag cd = root.getCompound(K_TRIGGER_CD);
        cd.putLong(affixId, tick);
        root.put(K_TRIGGER_CD, cd);
    }

    public static int getHarmonicStacks(ItemStack stack) {
        CompoundTag root = readRoot(stack);
        return root == null ? 0 : root.getInt(K_HARMONIC_STACKS);
    }

    public static void setHarmonicStacks(ItemStack stack, int stacks) {
        CompoundTag root = getOrCreateRoot(stack);
        root.putInt(K_HARMONIC_STACKS, Math.max(0, Math.min(3, stacks)));
    }

    @Nullable
    public static CrucibleData getCrucible(ItemStack stack) {
        CompoundTag root = readRoot(stack);
        if (root == null || !root.contains(K_CRUCIBLE, Tag.TAG_COMPOUND)) return null;
        CompoundTag c = root.getCompound(K_CRUCIBLE);
        return new CrucibleData(c.getString("aspect"), c.getFloat("offset"), c.getString("process_id"));
    }

    public static List<ResonanceSocket> getSockets(ItemStack stack) {
        CompoundTag root = readRoot(stack);
        if (root == null || !root.contains(K_RESONANCE, Tag.TAG_LIST)) return Collections.emptyList();
        ListTag list = root.getList(K_RESONANCE, Tag.TAG_COMPOUND);
        List<ResonanceSocket> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            out.add(new ResonanceSocket(t.getString("crystal"), t.getInt("lvl")));
        }
        return Collections.unmodifiableList(out);
    }

    public static List<SoulEcho> getSoulEchoes(ItemStack stack) {
        CompoundTag root = readRoot(stack);
        if (root == null || !root.contains(K_SOUL, Tag.TAG_LIST)) return Collections.emptyList();
        ListTag list = root.getList(K_SOUL, Tag.TAG_COMPOUND);
        List<SoulEcho> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            out.add(new SoulEcho(t.getString("mob"), t.getString("echo"), t.getInt("tier")));
        }
        return Collections.unmodifiableList(out);
    }

    public static ExperienceData getExperience(ItemStack stack) {
        CompoundTag root = readRoot(stack);
        if (root == null) return new ExperienceData(0, 0, 0, 0, 0);
        CompoundTag e = root.getCompound(K_EXPERIENCE);
        return new ExperienceData(
                e.getLong("kills"), e.getLong("casts"),
                e.getLong("blocks"), e.getLong("hits"),
                e.getInt("tier"));
    }

    @Nullable
    public static CelestialBlessing getCelestial(ItemStack stack) {
        CompoundTag root = readRoot(stack);
        if (root == null || !root.contains(K_CELESTIAL, Tag.TAG_COMPOUND)) return null;
        CompoundTag d = root.getCompound(K_CELESTIAL);
        return new CelestialBlessing(d.getString("blessing"), d.getInt("phase"), d.getString("biome"));
    }

    @Nullable
    private static CompoundTag readRoot(ItemStack stack) {
        if (stack.isEmpty()) return null;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ROOT_TAG, Tag.TAG_COMPOUND)) return null;
        return tag.getCompound(ROOT_TAG);
    }

    private static CompoundTag getOrCreateRoot(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            tag.put(ROOT_TAG, new CompoundTag());
        }
        return tag.getCompound(ROOT_TAG);
    }

    private static void bumpTier(CompoundTag root) {
        int t = root.getInt(K_TIER);
        if (t < 5) root.putInt(K_TIER, t + 1);
    }

    private static long[] thresholdsFor(GearCategory cat) {
        return switch (cat) {
            case WEAPON -> WEAPON_KILL_THRESHOLDS;
            case TOOL   -> TOOL_BLOCK_THRESHOLDS;
            case ARMOR  -> ARMOR_HIT_THRESHOLDS;
            case OTHER  -> WEAPON_KILL_THRESHOLDS;
        };
    }
}
