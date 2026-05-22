package com.huige233.transcend.ascension;

import com.huige233.transcend.ascension.tree.NodeDefinition;
import com.huige233.transcend.ascension.tree.TreeDefinition;
import com.huige233.transcend.ascension.tree.TreeRegistry;
import com.huige233.transcend.spell.SpellElement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 玩家全量飞升数据（NBT持久化）
 *
 * 包含：
 *  - 职业选择、元素专精
 *  - 已完成仪式集合 + 当前飞升阶段（0-4）
 *  - 各类进度计数器（击杀/施法/Boss击杀）
 *  - 已解锁天赋节点
 *  - 飞升等级/经验/天赋点
 *  - 仪式奖励累积属性块（持久化）
 *  - 实时合并属性块（运行时计算，不持久化）
 */
public class PlayerAscensionData {

    // ─── NBT键 ────────────────────────────────────────────────────────────
    private static final String T_CLASS        = "mage_class";
    private static final String T_MASTERY      = "mastery";
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

    // 誓约/试炼相关NBT键（新增）
    private static final String T_PRIMARY_VOW       = "primary_vow";
    private static final String T_SECONDARY_VOW     = "secondary_vow";
    private static final String T_TERTIARY_VOW      = "tertiary_vow";
    private static final String T_CAPSTONE_VOW      = "capstone_vow";
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

    // ─── 飞升等级常量 ─────────────────────────────────────────────────────
    public static final int MAX_LEVEL = 10;
    public static final long[] LEVEL_XP = {
            0, 100, 300, 700, 1500, 3000, 6000, 12000, 25000, 50000, 100000
    };
    public static final int POINTS_PER_LEVEL = 3;

    // ─── 数据字段 ─────────────────────────────────────────────────────────
    private MageClass mageClass    = MageClass.NONE;
    private ElementMastery mastery = ElementMastery.NONE;

    /** 当前飞升阶段 0-4（每完成一个仪式+1） */
    private int stage = 0;
    /** 已完成仪式ID集合 */
    private final Set<String> completedRituals = new HashSet<>();

    /** 已解锁天赋节点 */
    private final Set<String> unlockedNodes = new HashSet<>();

    private int  talentPoints  = 0;
    private int  ascensionLevel = 0;
    private long ascensionXP   = 0;

    // 进度计数器
    private long totalKills   = 0;
    private long totalCasts   = 0;
    private long bossKills    = 0;

    /** 仪式奖励累积的属性块（持久化，叠加不可逆） */
    private final AscensionStatBlock ritualStats = new AscensionStatBlock();

    // ─── 誓约系统（新增） ─────────────────────────────────────────────────
    private String primaryVow   = "";   // 阶段1誓约ID
    private String secondaryVow = "";   // 阶段2誓约ID
    private String tertiaryVow  = "";   // 阶段3誓约ID
    private String capstoneVow  = "";   // 阶段4誓约ID

    // ─── 二级精通（新增） ─────────────────────────────────────────────────
    private String secondaryMastery = "none";
    private String sealedElement    = "";

    // ─── 试炼进度追踪（新增） ─────────────────────────────────────────────
    private int  discoveredComponents    = 0;   // 发现的法术组件数
    private long circleStabilizeSeconds  = 0;   // 法阵稳定总秒数
    private int  maxConcurrentCircles    = 0;   // 同时维持的最多法阵数
    private int  highestArenaWave        = 0;   // 竞技场最高波数
    private int  scrollsCrafted          = 0;   // 制作的卷轴数
    private int  tabletsRestored         = 0;   // 修复的石板数
    private int  nexusActions            = 0;   // 枢纽维度行动数
    private long peakManaThroughput      = 0;   // 峰值魔力吞吐量

    // ─── 序列化 ───────────────────────────────────────────────────────────

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(T_CLASS,   mageClass.id);
        tag.putString(T_MASTERY, mastery.id);
        tag.putInt   (T_STAGE,   stage);
        tag.putInt   (T_POINTS,  talentPoints);
        tag.putInt   (T_LEVEL,   ascensionLevel);
        tag.putLong  (T_XP,      ascensionXP);
        tag.putLong  (T_KILLS,   totalKills);
        tag.putLong  (T_CASTS,   totalCasts);
        tag.putLong  (T_BOSS_KILLS, bossKills);
        tag.put      (T_RITUAL_STATS, ritualStats.save());

        ListTag ritualList = new ListTag();
        for (String r : completedRituals) ritualList.add(StringTag.valueOf(r));
        tag.put(T_RITUALS, ritualList);

        ListTag nodeList = new ListTag();
        for (String n : unlockedNodes) nodeList.add(StringTag.valueOf(n));
        tag.put(T_NODES, nodeList);

        // 誓约/二级精通/封印元素
        tag.putString(T_PRIMARY_VOW,       primaryVow);
        tag.putString(T_SECONDARY_VOW,     secondaryVow);
        tag.putString(T_TERTIARY_VOW,      tertiaryVow);
        tag.putString(T_CAPSTONE_VOW,      capstoneVow);
        tag.putString(T_SECONDARY_MASTERY, secondaryMastery);
        tag.putString(T_SEALED_ELEMENT,    sealedElement);

        // 试炼进度
        tag.putInt (T_DISCOVERED_COMPS,  discoveredComponents);
        tag.putLong(T_CIRCLE_STABLE_SEC, circleStabilizeSeconds);
        tag.putInt (T_MAX_CIRCLES,       maxConcurrentCircles);
        tag.putInt (T_ARENA_WAVE,        highestArenaWave);
        tag.putInt (T_SCROLLS_CRAFTED,   scrollsCrafted);
        tag.putInt (T_TABLETS_RESTORED,  tabletsRestored);
        tag.putInt (T_NEXUS_ACTIONS,     nexusActions);
        tag.putLong(T_PEAK_THROUGHPUT,   peakManaThroughput);

        return tag;
    }

    public void load(CompoundTag tag) {
        mageClass  = MageClass.getById(tag.getString(T_CLASS));
        mastery    = ElementMastery.getById(tag.getString(T_MASTERY));
        stage      = tag.getInt(T_STAGE);
        talentPoints   = tag.getInt(T_POINTS);
        ascensionLevel = tag.getInt(T_LEVEL);
        ascensionXP    = tag.getLong(T_XP);
        totalKills     = tag.getLong(T_KILLS);
        totalCasts     = tag.getLong(T_CASTS);
        bossKills      = tag.getLong(T_BOSS_KILLS);

        if (tag.contains(T_RITUAL_STATS, Tag.TAG_COMPOUND))
            ritualStats.load(tag.getCompound(T_RITUAL_STATS));

        completedRituals.clear();
        ListTag rl = tag.getList(T_RITUALS, Tag.TAG_STRING);
        for (int i = 0; i < rl.size(); i++) completedRituals.add(rl.getString(i));

        unlockedNodes.clear();
        ListTag nl = tag.getList(T_NODES, Tag.TAG_STRING);
        for (int i = 0; i < nl.size(); i++) unlockedNodes.add(nl.getString(i));

        // 誓约/二级精通/封印元素（默认空字符串/"none"）
        primaryVow       = tag.contains(T_PRIMARY_VOW)       ? tag.getString(T_PRIMARY_VOW)       : "";
        secondaryVow     = tag.contains(T_SECONDARY_VOW)     ? tag.getString(T_SECONDARY_VOW)     : "";
        tertiaryVow      = tag.contains(T_TERTIARY_VOW)      ? tag.getString(T_TERTIARY_VOW)      : "";
        capstoneVow      = tag.contains(T_CAPSTONE_VOW)      ? tag.getString(T_CAPSTONE_VOW)      : "";
        secondaryMastery = tag.contains(T_SECONDARY_MASTERY) ? tag.getString(T_SECONDARY_MASTERY) : "none";
        sealedElement    = tag.contains(T_SEALED_ELEMENT)    ? tag.getString(T_SEALED_ELEMENT)    : "";

        // 试炼进度计数（默认 0）
        discoveredComponents   = tag.contains(T_DISCOVERED_COMPS)  ? tag.getInt (T_DISCOVERED_COMPS)  : 0;
        circleStabilizeSeconds = tag.contains(T_CIRCLE_STABLE_SEC) ? tag.getLong(T_CIRCLE_STABLE_SEC) : 0L;
        maxConcurrentCircles   = tag.contains(T_MAX_CIRCLES)       ? tag.getInt (T_MAX_CIRCLES)       : 0;
        highestArenaWave       = tag.contains(T_ARENA_WAVE)        ? tag.getInt (T_ARENA_WAVE)        : 0;
        scrollsCrafted         = tag.contains(T_SCROLLS_CRAFTED)   ? tag.getInt (T_SCROLLS_CRAFTED)   : 0;
        tabletsRestored        = tag.contains(T_TABLETS_RESTORED)  ? tag.getInt (T_TABLETS_RESTORED)  : 0;
        nexusActions           = tag.contains(T_NEXUS_ACTIONS)     ? tag.getInt (T_NEXUS_ACTIONS)     : 0;
        peakManaThroughput     = tag.contains(T_PEAK_THROUGHPUT)   ? tag.getLong(T_PEAK_THROUGHPUT)   : 0L;
    }

    public void copyFrom(PlayerAscensionData o) {
        load(o.save());
    }

    // ─── 职业 ─────────────────────────────────────────────────────────────

    public MageClass getMageClass() { return mageClass; }
    public boolean hasSelectedClass() { return mageClass.isSelected(); }

    /** 只有未选职业时可以选择（觉醒仪式中选择） */
    public boolean selectClass(MageClass c) {
        if (mageClass.isSelected()) return false;
        if (!c.isSelected()) return false;
        this.mageClass = c;
        return true;
    }

    /** 指令强制设置职业（跳过所有限制） */
    public void forceSetClass(MageClass c) {
        this.mageClass = c;
    }

    /** 指令强制设置飞升阶段（跳过仪式限制，0-4） */
    public void forceSetStage(int stage) {
        this.stage = Math.max(0, Math.min(4, stage));
    }

    // ─── 元素专精 ─────────────────────────────────────────────────────────

    public ElementMastery getMastery() { return mastery; }
    public boolean hasMastery() { return mastery.isSelected(); }

    /**
     * 选择元素专精（需要Stage >= 1）
     * 一旦选定不可更改，除非通过 respec() 重置
     */
    public boolean selectMastery(ElementMastery m, boolean forceOverride) {
        if (stage < 1 && !forceOverride) return false;
        if (mastery.isSelected() && !forceOverride) return false;
        this.mastery = m;
        return true;
    }

    /**
     * 洗点重置：清空所有节点和专精，天赋点全额返还
     * 不重置：职业、仪式进度、飞升等级/XP、计数器
     */
    public int respec() {
        int refund = 0;
        for (String nodeId : unlockedNodes) {
            NodeDefinition node = TreeRegistry.getInstance().getNode(nodeId);
            if (node != null) refund += node.getCost();
        }
        unlockedNodes.clear();
        mastery = ElementMastery.NONE;
        talentPoints += refund;
        return refund;
    }

    // ─── 仪式与阶段 ───────────────────────────────────────────────────────

    public int getStage() { return stage; }

    public boolean isRitualCompleted(AscensionRitual ritual) {
        return completedRituals.contains(ritual.name());
    }

    public boolean isRitualCompleted(String name) {
        return completedRituals.contains(name);
    }

    /**
     * 尝试完成对应阶段的仪式
     * @return true 如果仪式成功完成（首次）
     */
    public boolean tryCompleteRitual(AscensionRitual ritual) {
        if (isRitualCompleted(ritual)) return false;
        if (ritual.stageIndex != stage) return false;   // 必须按顺序完成
        if (!ritual.isMet(this)) return false;

        completedRituals.add(ritual.name());
        stage++;

        // 叠加仪式奖励属性
        AscensionStatBlock reward = switch (ritual) {
            case AWAKENING      -> AscensionStatBlock.awakeningReward();
            case TEMPERING      -> AscensionStatBlock.temperingReward();
            case PURIFICATION   -> AscensionStatBlock.purificationReward();
            case TRANSCENDENCE  -> AscensionStatBlock.transcendenceReward();
        };
        ritualStats.addFrom(reward);

        return true;
    }

    /** 当前阶段需要完成的仪式（null = 最终阶段已完成） */
    public AscensionRitual getPendingRitual() {
        return AscensionRitual.getByStage(stage);
    }

    public Set<String> getCompletedRituals() { return Collections.unmodifiableSet(completedRituals); }

    // ─── 天赋节点 ─────────────────────────────────────────────────────────

    public boolean isNodeUnlocked(String id)           { return unlockedNodes.contains(id); }
    public Set<String> getUnlockedNodes()              { return Collections.unmodifiableSet(unlockedNodes); }
    public boolean hasNode(String id)                  { return unlockedNodes.contains(id); }

    public boolean isTierUnlockedByStage(int tier) {
        return switch (tier) {
            case 0, 1, 2 -> stage >= 2;
            case 3, 4    -> stage >= 3;
            case 5       -> stage >= 4;
            default      -> false;
        };
    }

    public boolean canUnlock(String nodeId) {
        if (unlockedNodes.contains(nodeId)) return false;
        NodeDefinition node = TreeRegistry.getInstance().getNode(nodeId);
        if (node == null) return false;
        if (!isTierUnlockedByStage(node.getTier())) return false;

        TreeDefinition tree = findTreeForNode(nodeId);
        if (tree != null && tree.getTreeType() == com.huige233.transcend.ascension.tree.TreeType.TALENT) {
            if (tree.getMageClass() != mageClass) return false;
        }

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
        TreeRegistry reg = TreeRegistry.getInstance();
        TreeDefinition asc = reg.getAscensionTree();
        if (asc != null && asc.getNode(nodeId) != null) return asc;
        TreeDefinition talent = reg.getTalentTree(mageClass);
        if (talent != null && talent.getNode(nodeId) != null) return talent;
        return null;
    }

    // ─── 天赋点 ────────────────────────────────────────────────────────────

    public int  getTalentPoints()    { return talentPoints; }
    public void addTalentPoints(int n) { talentPoints = Math.max(0, talentPoints + n); }

    // ─── 飞升等级/XP ──────────────────────────────────────────────────────

    public int  getAscensionLevel()  { return ascensionLevel; }
    public long getAscensionXP()     { return ascensionXP; }

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

    /** 增加飞升XP，返回是否升级 */
    public boolean addAscensionXP(long amount) {
        if (ascensionLevel >= MAX_LEVEL) return false;
        ascensionXP += amount;
        boolean leveledUp = false;
        while (ascensionLevel < MAX_LEVEL && ascensionXP >= LEVEL_XP[ascensionLevel + 1]) {
            ascensionLevel++;
            talentPoints += POINTS_PER_LEVEL;
            leveledUp = true;
        }
        return leveledUp;
    }

    /**
     * 直接设置飞升 XP 总量 (用于 /tr_xp set 命令)。
     * 会重新计算等级并适当奖励/扣除天赋点。
     */
    public void setAscensionXP(long newXP) {
        long oldLevel = ascensionLevel;
        ascensionXP = Math.max(0, newXP);
        // 重算等级 (向上或向下)
        ascensionLevel = 0;
        for (int lv = 1; lv <= MAX_LEVEL; lv++) {
            if (ascensionXP >= LEVEL_XP[lv]) ascensionLevel = lv;
            else break;
        }
        // 调整天赋点 (按等级差值)
        long levelDelta = ascensionLevel - oldLevel;
        if (levelDelta > 0) {
            talentPoints += (int) (levelDelta * POINTS_PER_LEVEL);
        } else if (levelDelta < 0) {
            // 降级: 不扣已花费天赋,只调可用 (避免负数)
            talentPoints = Math.max(0, talentPoints + (int) (levelDelta * POINTS_PER_LEVEL));
        }
    }

    /**
     * 直接设置飞升等级 (用于 /tr_xp setlevel 命令)。
     * 会同步设置 XP 到该等级的起始值,并按等级差值调整天赋点。
     */
    public void setAscensionLevel(int newLevel) {
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

    // ─── 计数器 ────────────────────────────────────────────────────────────

    public long getTotalKills()  { return totalKills; }
    public long getTotalCasts()  { return totalCasts; }
    public long getBossKills()   { return bossKills; }

    public void addKill(boolean isBoss) {
        totalKills++;
        if (isBoss) bossKills++;
    }

    public void addCast() { totalCasts++; }

    // ─── 实时合并属性 ──────────────────────────────────────────────────────

    /**
     * 合并所有来源的属性块（等级被动 + 仪式奖励 + 天赋节点 + 专精）
     * 每次需要时实时计算，不缓存（轻量操作）
     */
    public AscensionStatBlock buildTotalStats() {
        AscensionStatBlock total = new AscensionStatBlock();
        total.addFrom(AscensionStatBlock.fromLevel(ascensionLevel, mageClass));
        total.addFrom(ritualStats);
        total.addFrom(TreeRegistry.getInstance().computeNodeStats(unlockedNodes, mageClass));
        total.addFrom(AscensionStatBlock.fromMastery(mastery));
        total.addFrom(buildVowStats());
        return total;
    }

    /**
     * Build a StatBlock summarizing all chosen vows across all 4 stages.
     * Maps AscensionVow numeric fields to AscensionStatBlock fields where applicable.
     * Fields without a StatBlock equivalent (circleUpkeepMult / circleLimitAdd /
     * manaCostMult / healingMult) are queried separately via getVowCircle*() / getVowMana*().
     */
    private AscensionStatBlock buildVowStats() {
        AscensionStatBlock v = new AscensionStatBlock();
        for (int stage = 1; stage <= 4; stage++) {
            String vowId = getVowForStage(stage);
            if (vowId == null || vowId.isEmpty()) continue;
            AscensionVow vow = VowRegistry.get(vowId);
            if (vow == null) continue;

            v.spellPowerBonus    += vow.getSpellDamageBonus();
            v.bonusMaxHealth     += vow.getHealthAdd();
            v.bonusManaCapacity  += (int) vow.getManaCapAdd();
            v.critChance         += vow.getCritChanceAdd();
            v.cooldownReduction  += vow.getCdrAdd();
            v.reactionBonus      += vow.getReactionBonus();
            v.summonDamageBonus  += vow.getSummonBonus();
            if (vow.getMoveSpeedMult() != 1.0f) {
                v.moveSpeedBonus += (vow.getMoveSpeedMult() - 1.0f);
            }
            if (vow.getCritMultMin() > v.critMultiplier) {
                v.critMultiplier = vow.getCritMultMin();
            }
        }
        return v;
    }

    /** Aggregated mana-cost multiplier from all chosen vows (1.0 = neutral). */
    public float getVowManaCostMult() {
        float mult = 1.0f;
        for (int stage = 1; stage <= 4; stage++) {
            String vowId = getVowForStage(stage);
            if (vowId == null || vowId.isEmpty()) continue;
            AscensionVow vow = VowRegistry.get(vowId);
            if (vow == null) continue;
            mult *= vow.getManaCostMult();
        }
        return mult;
    }

    /** Aggregated incoming-healing multiplier from all chosen vows (1.0 = neutral). */
    public float getVowHealingMult() {
        float mult = 1.0f;
        for (int stage = 1; stage <= 4; stage++) {
            String vowId = getVowForStage(stage);
            if (vowId == null || vowId.isEmpty()) continue;
            AscensionVow vow = VowRegistry.get(vowId);
            if (vow == null) continue;
            mult *= vow.getHealingMult();
        }
        return mult;
    }

    /** Aggregated additive circle slot bonus from all chosen vows. */
    public int getVowCircleLimitAdd() {
        int add = 0;
        for (int stage = 1; stage <= 4; stage++) {
            String vowId = getVowForStage(stage);
            if (vowId == null || vowId.isEmpty()) continue;
            AscensionVow vow = VowRegistry.get(vowId);
            if (vow == null) continue;
            add += vow.getCircleLimitAdd();
        }
        return add;
    }

    /** Aggregated multiplicative circle upkeep modifier from all chosen vows. */
    public float getVowCircleUpkeepMult() {
        float mult = 1.0f;
        for (int stage = 1; stage <= 4; stage++) {
            String vowId = getVowForStage(stage);
            if (vowId == null || vowId.isEmpty()) continue;
            AscensionVow vow = VowRegistry.get(vowId);
            if (vow == null) continue;
            mult *= vow.getCircleUpkeepMult();
        }
        return mult;
    }

    /** 获取当前元素的最终伤害倍率加成（专精+法术强度之和）*/
    public float getSpellDamageMultiplier(SpellElement element) {
        AscensionStatBlock stats = buildTotalStats();
        float masteryBonus = mastery.getDamageBonus(element);
        return 1.0f + stats.spellPowerBonus + masteryBonus;
    }

    /**
     * Player-aware overload — reads SPELL_POWER attribute (which includes equipment / curio modifiers
     * from any source, including 3rd-party mods) rather than the internal StatBlock value.
     *
     * <p>Prefer this in damage-calc sites where a Player reference is available; the attribute
     * is the authoritative aggregate after applyPersistentStats has run.
     */
    public float getSpellDamageMultiplier(SpellElement element, net.minecraft.world.entity.player.Player player) {
        float attrBonus = (float) player.getAttributeValue(com.huige233.transcend.TranscendAttributes.SPELL_POWER.get());
        float masteryBonus = mastery.getDamageBonus(element);
        return 1.0f + attrBonus + masteryBonus;
    }

    /** 获取施法冷却减少（已截断到0.75上限）*/
    public float getEffectiveCDR() {
        return buildTotalStats().getEffectiveCDR();
    }

    /** 获取魔力消耗折扣（来自专精 + 仪式/天赋 stat block 累加；上限 0.50）*/
    public float getManaCostReduction(SpellElement element) {
        AscensionStatBlock stats = buildTotalStats();
        float fromStat = stats.getEffectiveManaCostReduction();
        float fromMastery = mastery.getManaCostReduction(element);
        // 加性合并后再 cap 一次
        return Math.min(0.75f, fromStat + fromMastery);
    }

    /** 仪式奖励属性（持久化，供外部读取） */
    public AscensionStatBlock getRitualStats() { return ritualStats; }

    // ─── 誓约系统（新增） ─────────────────────────────────────────────────

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
        String v = vowId == null ? "" : vowId;
        switch (stage) {
            case 1 -> primaryVow   = v;
            case 2 -> secondaryVow = v;
            case 3 -> tertiaryVow  = v;
            case 4 -> capstoneVow  = v;
            default -> {}
        }
    }

    // ─── 二级精通 / 封印元素（新增） ──────────────────────────────────────

    public String getSecondaryMastery() { return secondaryMastery; }
    public void   setSecondaryMastery(String m) { this.secondaryMastery = m == null ? "none" : m; }

    public String getSealedElement() { return sealedElement; }
    public void   setSealedElement(String e) { this.sealedElement = e == null ? "" : e; }

    // ─── 试炼进度追踪（新增） ─────────────────────────────────────────────

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
}
