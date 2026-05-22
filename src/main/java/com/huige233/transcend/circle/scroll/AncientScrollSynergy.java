package com.huige233.transcend.circle.scroll;

import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.ElementMastery;
import com.huige233.transcend.ascension.PlayerAscensionData;
import com.huige233.transcend.block.circle.MagicCircleCoreBlockEntity;
import com.huige233.transcend.circle.CircleTier;
import com.huige233.transcend.spell.SpellElement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Round 15: 古法咒卷与三大系统(法术 / 法阵 / 飞升)的融合层。
 * <p>纯静态查询库，不持有状态。所有逻辑都基于卷轴 type 字符串。
 */
public final class AncientScrollSynergy {

    /** 每张古法咒卷绑定一个主元素 — 用于 mage class 共鸣检测。null = 全元素 (OMNI). */
    private static final Map<String, SpellElement> SCROLL_ELEMENT = buildElementMap();

    /** 每张古法咒卷需要的最低飞升阶段。Stage 0=未飞升 ~ Stage 4=完整飞升. */
    private static final Map<String, Integer> SCROLL_STAGE = buildStageMap();

    private AncientScrollSynergy() {}

    private static Map<String, SpellElement> buildElementMap() {
        Map<String, SpellElement> m = new HashMap<>();
        // 基础古卷
        m.put("solar_judgement",        SpellElement.HOLY);
        m.put("eclipse_veil",           SpellElement.DARK);
        m.put("chronal_stillness",      SpellElement.TIME);
        m.put("storm_king_writ",        SpellElement.THUNDER);
        m.put("leyline_eruption",       SpellElement.NATURE);
        m.put("sovereign_aegis",        SpellElement.HOLY);
        m.put("thousand_league_return", SpellElement.SPACE);
        m.put("void_exile_mandate",     SpellElement.VOID);
        m.put("worldmender_edict",      SpellElement.NATURE);
        m.put("avatar_fall",            null);                     // 全元素
        // 扩展
        m.put("inverted_heaven",        SpellElement.CHAOS);
        m.put("leyline_resync",         SpellElement.NATURE);
        m.put("oreblood_revelation",    SpellElement.EARTH);
        m.put("ordered_vault",          SpellElement.SPACE);
        m.put("paper_legion",           SpellElement.NATURE);
        m.put("unbroken_arsenal",       SpellElement.EARTH);
        m.put("unremembered_fog",       SpellElement.DARK);
        m.put("eighteenfold_dragon",    null);                     // 全元素
        // 禁咒
        m.put("forbidden_hollow_quarry", SpellElement.EARTH);
        m.put("forbidden_black_sun",    SpellElement.VOID);
        return m;
    }

    private static Map<String, Integer> buildStageMap() {
        Map<String, Integer> m = new HashMap<>();
        // Tier 1 基础古卷: Stage 1+
        m.put("solar_judgement", 1);
        m.put("leyline_eruption", 1);
        m.put("storm_king_writ", 1);
        m.put("sovereign_aegis", 1);
        m.put("thousand_league_return", 1);
        // Tier 2 高阶古卷: Stage 2+
        m.put("chronal_stillness", 2);
        m.put("eclipse_veil", 2);
        m.put("worldmender_edict", 2);
        m.put("void_exile_mandate", 2);
        m.put("paper_legion", 2);
        m.put("unbroken_arsenal", 2);
        m.put("ordered_vault", 2);
        m.put("oreblood_revelation", 2);
        // Tier 3 究极古卷: Stage 3+
        m.put("avatar_fall", 3);
        m.put("inverted_heaven", 3);
        m.put("eighteenfold_dragon", 3);
        m.put("leyline_resync", 3);
        m.put("unremembered_fog", 3);
        // Tier 4 禁咒: Stage 4
        m.put("forbidden_hollow_quarry", 4);
        m.put("forbidden_black_sun", 4);
        return m;
    }

    @Nullable
    public static SpellElement getScrollElement(String scrollType) {
        return SCROLL_ELEMENT.get(scrollType);
    }

    public static int getRequiredStage(String scrollType) {
        return SCROLL_STAGE.getOrDefault(scrollType, 0);
    }

    /** 玩家的 mastery 是否与卷轴元素共鸣。OMNI mastery 永远算共鸣。null 元素卷轴永远共鸣。 */
    public static boolean isMasteryMatched(Player player, String scrollType) {
        SpellElement scrollElement = getScrollElement(scrollType);
        PlayerAscensionData data = AscensionCapability.get(player);
        ElementMastery mastery = data.getMastery();
        if (mastery == ElementMastery.OMNI) return true;
        if (scrollElement == null) return true; // OMNI 卷轴
        return mastery.element == scrollElement;
    }

    /** Mastery 匹配时返回 0.70 (省 30% 总魔力消耗); 否则 1.0。 */
    public static float getMasteryCostMultiplier(Player player, String scrollType) {
        return isMasteryMatched(player, scrollType) ? 0.70f : 1.0f;
    }

    /**
     * 扫描玩家附近 9×9 chunk 内的所有活跃法环, 返回 drain 速率倍率。
     *
     * <p>Tier 影响响应半径与倍率：
     * <ul>
     *   <li>T1 INITIATE within 16: ×1.2</li>
     *   <li>T2 ADEPT within 24: ×1.4</li>
     *   <li>T3 MASTER within 32: ×1.6</li>
     *   <li>T4 ARCHON within 40: ×1.8</li>
     *   <li>T5 PRIMORDIAL within 48: ×2.0</li>
     * </ul>
     * 多个法环取最强加成。无法环 = 1.0。
     */
    public static float getCircleDrainBoost(Player player, Level level) {
        BlockPos playerPos = player.blockPosition();
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;

        float bestBoost = 1.0f;
        int chunkRadius = 4; // 9×9 chunks ≈ 144 blocks

        for (int cx = playerChunkX - chunkRadius; cx <= playerChunkX + chunkRadius; cx++) {
            for (int cz = playerChunkZ - chunkRadius; cz <= playerChunkZ + chunkRadius; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunk(cx, cz);
                for (Map.Entry<BlockPos, BlockEntity> e : chunk.getBlockEntities().entrySet()) {
                    BlockEntity be = e.getValue();
                    if (!(be instanceof MagicCircleCoreBlockEntity core)) continue;
                    if (!core.isActive() || !core.isStructureValid()) continue;
                    CircleTier tier = core.getDetectedTier();
                    if (tier == null) continue;

                    double distSq = e.getKey().distSqr(playerPos);
                    int tierLevel = tier.getLevel();
                    int responseRadius = 8 + tierLevel * 8;
                    if (distSq > (double) responseRadius * responseRadius) continue;

                    float boost = 1.0f + tierLevel * 0.2f;
                    if (boost > bestBoost) bestBoost = boost;
                }
            }
        }
        return bestBoost;
    }

    /** 计算最终魔力消耗（应用 mastery 优惠）。 */
    public static int getEffectiveCost(Player player, String scrollType, int rawCost) {
        return Math.max(1, (int) (rawCost * getMasteryCostMultiplier(player, scrollType)));
    }

    /**
     * 计算每 tick drain 速率。
     * 公式: (baseDrain + ascLevel × 2) × circleBoost。
     * baseDrain=30 means ~ 600 mana/sec @ 1.0 boost.
     */
    public static int getDrainPerTick(Player player, Level level) {
        int baseDrain = 30;
        int ascLevel = AscensionCapability.get(player).getAscensionLevel(); // 0-10
        float circleBoost = getCircleDrainBoost(player, level);
        return Math.max(1, (int) ((baseDrain + ascLevel * 2) * circleBoost));
    }
}
