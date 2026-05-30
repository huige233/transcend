package com.huige233.transcend.gear;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * R80: 造物之道（Path of Artifice）核心数据 API。
 *
 * <p>所有装备客制化数据写入 ItemStack 的 NBT 子标签 {@value #ROOT_TAG}。
 * 单一存储格式 — 任何 {@code isDamageableItem()} 装备（武器/工具/护甲）都可载入。
 *
 * <h2>规则（玩家锁定）</h2>
 * <ul>
 *   <li><b>不可逆</b> — 任一阶段写入后，对应字段永不可移除（直到装备销毁）</li>
 *   <li><b>顺序灵活</b> — B/A/C/D 阶段可任意顺序</li>
 *   <li><b>E 必先</b> — 任何非 E 阶段必须在 CRUCIBLE 已写入后才能进入</li>
 * </ul>
 *
 * <h2>NBT 结构</h2>
 * <pre>
 * "transcend:forge_data": {
 *   "tier": 0..5,                    // 已完成阶段数（仅递增）
 *   "crucible":  { aspect, offset, process_id }
 *   "resonance": [{ crystal, lvl }]  // 上限 4
 *   "soul":      [{ mob, echo, tier }]  // 上限 3
 *   "experience":{ kills, casts, blocks, hits, tier } // tier 0..3
 *   "celestial": { blessing, phase, biome }
 * }
 * </pre>
 *
 * <h2>R81-R85 接入点</h2>
 * <ul>
 *   <li>R81 坩埚 → 调 {@link #writeCrucible}</li>
 *   <li>R82 共鸣 → 调 {@link #addResonanceSocket}</li>
 *   <li>R83 灵魂 → 调 {@link #addSoulEcho}</li>
 *   <li>R84 觉醒 → 调 {@link #incrementExperience} + {@link #upgradeExperienceTier}</li>
 *   <li>R85 加冕 → 调 {@link #writeCelestial}</li>
 *   <li>R86 战斗 hook → 调 {@link #getCrucible}/{@link #getSockets}/etc 读取生效</li>
 * </ul>
 */
public class GearForgeData {

    public static final String ROOT_TAG = "transcend_forge_data";

    // ── R80 锁定的上限 ────────────────────────────────────────────────
    public static final int MAX_RESONANCE_SOCKETS = 4;   // Q3 玩家决策
    public static final int MAX_SOUL_ECHOES       = 3;
    public static final int MAX_EXPERIENCE_TIER   = 3;

    // ── 阶段 NBT 子键 ────────────────────────────────────────────────
    private static final String K_TIER       = "tier";
    private static final String K_CRUCIBLE   = "crucible";
    private static final String K_RESONANCE  = "resonance";
    private static final String K_SOUL       = "soul";
    private static final String K_EXPERIENCE = "experience";
    private static final String K_CELESTIAL  = "celestial";
    /** R91: 触发型词条（独立第 6 类，不进 ForgeStage 门禁） */
    private static final String K_TRIGGER    = "trigger";
    /** R91: 触发型词条 CD（按词条 id 存上次触发的 server tick）— 用于周期 / on-hurt 的 once-per-Nmin */
    private static final String K_TRIGGER_CD = "trigger_cd";
    /** R91: harmonic 击杀后的叠加计数（每次击杀 +1，最多 3，攻击后归 0）*/
    private static final String K_HARMONIC_STACKS = "harmonic_stacks";

    // ── R84 觉醒阈值（玩家 Q4 大幅下调）─────────────────────────────
    /** 武器：击杀数 → tier I/II/III */
    public static final long[] WEAPON_KILL_THRESHOLDS = { 100L, 400L, 1000L };
    /** 工具：挖掘块数 → tier I/II/III */
    public static final long[] TOOL_BLOCK_THRESHOLDS  = { 1000L, 4000L, 10000L };
    /** 护甲：受击次数 → tier I/II/III */
    public static final long[] ARMOR_HIT_THRESHOLDS   = { 50L, 200L, 500L };

    private GearForgeData() {} // 静态工具类

    // ─── 数据 records ────────────────────────────────────────────────

    public record CrucibleData(String aspect, float offset, String processId) {}
    public record ResonanceSocket(String crystalId, int level) {}
    public record SoulEcho(String mobId, String echoType, int tier) {}
    public record ExperienceData(long kills, long casts, long blocks, long hits, int tier) {}
    public record CelestialBlessing(String blessing, int moonPhase, String biomeClass) {}
    /** R91: 触发型词条数据（每件装备 ≤ 1 个，不可逆）*/
    public record TriggerAffixData(String affixId) {}

    // ─── 状态机（核心规则）──────────────────────────────────────────

    /** 装备是否可进入造物之道管线（Q1 锁定：仅 isDamageableItem 装备） */
    public static boolean isEligibleForPipeline(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) return false;
        return GearCategory.classify(stack) != GearCategory.OTHER;
    }

    /** 装备是否已进入管线（tier &gt; 0） */
    public static boolean isInPipeline(ItemStack stack) {
        return getTier(stack) > 0;
    }

    /** 当前 tier（0..5），= 已完成阶段数 */
    public static int getTier(ItemStack stack) {
        CompoundTag root = readRoot(stack);
        return root == null ? 0 : root.getInt(K_TIER);
    }

    /**
     * 检查能否启动给定阶段。规则：
     * <ul>
     *   <li>已写入 → 永远不可（不可逆）</li>
     *   <li>装备不合格 → 不可</li>
     *   <li>非 CRUCIBLE 阶段 → 必须 CRUCIBLE 已写入</li>
     *   <li>CRUCIBLE → 永远可（前提是装备合格 + 未写入）</li>
     * </ul>
     */
    public static boolean canEnterStage(ItemStack stack, ForgeStage stage) {
        if (!isEligibleForPipeline(stack)) return false;
        if (isStageWritten(stack, stage)) return false;
        if (stage == ForgeStage.CRUCIBLE) return true;
        return isStageWritten(stack, ForgeStage.CRUCIBLE); // 必须先过坩埚
    }

    /** 阶段是否已写入。E/D 看键存在；B/A 看 list 非空；C 看 tier &gt; 0。 */
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

    // ─── 写入器（被 R81-R85 各阶段方块/物品调用）────────────────────

    /**
     * R81 坩埚：写入基底（仅可写 1 次）。
     * @return true 成功；false 装备不合格 / 已写入
     */
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

    /**
     * R82 共鸣：追加词槽（上限 {@value #MAX_RESONANCE_SOCKETS}）。
     * 首次添加时升 tier；追加不升 tier。
     */
    public static boolean addResonanceSocket(ItemStack stack, String crystalId, int level) {
        if (!isEligibleForPipeline(stack)) return false;
        if (!isStageWritten(stack, ForgeStage.CRUCIBLE)) return false;  // E 必先门
        CompoundTag root = getOrCreateRoot(stack);
        ListTag list = root.getList(K_RESONANCE, Tag.TAG_COMPOUND);
        if (list.size() >= MAX_RESONANCE_SOCKETS) return false;
        boolean wasEmpty = list.isEmpty();
        CompoundTag socket = new CompoundTag();
        socket.putString("crystal", crystalId == null ? "" : crystalId);
        socket.putInt("lvl", level);
        list.add(socket);
        root.put(K_RESONANCE, list);
        if (wasEmpty) bumpTier(root); // 首次写入升 tier
        return true;
    }

    /**
     * R83 灵魂：追加回响（上限 {@value #MAX_SOUL_ECHOES}）。
     * 首次添加时升 tier；追加不升 tier。
     */
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

    /**
     * R84 经历：累加计数器（不直接升 tier；阈值满足后调 {@link #upgradeExperienceTier} 升）。
     *
     * <p>按 {@link GearCategory#classify} 分流：仅相关计数器累加。
     */
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

    /**
     * R84 经历：根据装备分类的当前计数 + 阈值表，尝试提升 awakening tier。
     * 首次升 tier (0→1) 时升管线 tier；后续 tier 提升不升管线 tier。
     *
     * @return 升级后的 awakening tier（0..3）；未升则返回当前值
     */
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
            case OTHER  -> 0L; // 不应发生（isEligibleForPipeline 过滤）
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
            if (curTier == 0) bumpTier(root); // 首次升 awakening 同时升管线 tier
        }
        return newTier;
    }

    /** R85 天命加冕：写入冠石（仅可写 1 次） */
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

    // ─── R91 触发型词条（独立第 6 类）─────────────────────────────────

    /**
     * R91: 铭刻触发型词条（仅可写 1 次，不可逆）。
     *
     * <p>与 5 阶段独立 — 不需要 CRUCIBLE 前置。任何 {@link #isEligibleForPipeline 合格} 装备都可铭刻。
     * <p>不提升管线 tier（5 阶段 tier 维持原计数）。
     *
     * @return true 成功；false 装备不合格 / 已铭刻
     */
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

    /** R91: 读取触发词条 CD（上次触发时 server tick）— 用于 once-per-N period 控制。0 表示未触发过。 */
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

    /** R91: harmonic 击杀后的叠加计数（0..3，每次击杀 +1，攻击后归 0）。 */
    public static int getHarmonicStacks(ItemStack stack) {
        CompoundTag root = readRoot(stack);
        return root == null ? 0 : root.getInt(K_HARMONIC_STACKS);
    }

    public static void setHarmonicStacks(ItemStack stack, int stacks) {
        CompoundTag root = getOrCreateRoot(stack);
        root.putInt(K_HARMONIC_STACKS, Math.max(0, Math.min(3, stacks)));
    }

    // ─── 读取器（被战斗 hook 调）─────────────────────────────────────

    @Nullable
    public static CrucibleData getCrucible(ItemStack stack) {
        CompoundTag root = readRoot(stack);
        if (root == null || !root.contains(K_CRUCIBLE, Tag.TAG_COMPOUND)) return null;
        CompoundTag c = root.getCompound(K_CRUCIBLE);
        return new CrucibleData(c.getString("aspect"), c.getFloat("offset"), c.getString("process_id"));
    }

    /** 不可变 list — 战斗 hook 直接遍历不会触发 setChanged */
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

    // ─── 内部工具 ─────────────────────────────────────────────────────

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

    /** 升管线 tier（不超过 5）。仅在阶段首次写入时调用。 */
    private static void bumpTier(CompoundTag root) {
        int t = root.getInt(K_TIER);
        if (t < 5) root.putInt(K_TIER, t + 1);
    }

    private static long[] thresholdsFor(GearCategory cat) {
        return switch (cat) {
            case WEAPON -> WEAPON_KILL_THRESHOLDS;
            case TOOL   -> TOOL_BLOCK_THRESHOLDS;
            case ARMOR  -> ARMOR_HIT_THRESHOLDS;
            case OTHER  -> WEAPON_KILL_THRESHOLDS; // fallback；不应被使用
        };
    }
}
