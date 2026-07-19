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

public final class AncientScrollSynergy {

    private static final Map<String, SpellElement> SCROLL_ELEMENT = buildElementMap();

    private static final Map<String, Integer> SCROLL_STAGE = buildStageMap();

    private AncientScrollSynergy() {}

    private static Map<String, SpellElement> buildElementMap() {
        Map<String, SpellElement> m = new HashMap<>();

        m.put("solar_judgement",        SpellElement.METAL);
        m.put("eclipse_veil",           SpellElement.EARTH);
        m.put("chronal_stillness",      SpellElement.WATER);
        m.put("storm_king_writ",        SpellElement.FIRE);
        m.put("leyline_eruption",       SpellElement.WOOD);
        m.put("sovereign_aegis",        SpellElement.METAL);
        m.put("thousand_league_return", SpellElement.CHAOS);
        m.put("void_exile_mandate",     SpellElement.CHAOS);
        m.put("worldmender_edict",      SpellElement.WOOD);
        m.put("avatar_fall",            null);

        m.put("inverted_heaven",        SpellElement.CHAOS);
        m.put("leyline_resync",         SpellElement.WOOD);
        m.put("oreblood_revelation",    SpellElement.EARTH);
        m.put("ordered_vault",          SpellElement.CHAOS);
        m.put("paper_legion",           SpellElement.WOOD);
        m.put("unbroken_arsenal",       SpellElement.EARTH);
        m.put("unremembered_fog",       SpellElement.EARTH);
        m.put("eighteenfold_dragon",    null);

        m.put("forbidden_hollow_quarry", SpellElement.EARTH);
        m.put("forbidden_black_sun",    SpellElement.CHAOS);
        return m;
    }

    private static Map<String, Integer> buildStageMap() {
        Map<String, Integer> m = new HashMap<>();

        m.put("solar_judgement", 1);
        m.put("leyline_eruption", 1);
        m.put("storm_king_writ", 1);
        m.put("sovereign_aegis", 1);
        m.put("thousand_league_return", 1);

        m.put("chronal_stillness", 2);
        m.put("eclipse_veil", 2);
        m.put("worldmender_edict", 2);
        m.put("void_exile_mandate", 2);
        m.put("paper_legion", 2);
        m.put("unbroken_arsenal", 2);
        m.put("ordered_vault", 2);
        m.put("oreblood_revelation", 2);

        m.put("avatar_fall", 3);
        m.put("inverted_heaven", 3);
        m.put("eighteenfold_dragon", 3);
        m.put("leyline_resync", 3);
        m.put("unremembered_fog", 3);

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

    public static boolean isMasteryMatched(Player player, String scrollType) {
        SpellElement scrollElement = getScrollElement(scrollType);
        PlayerAscensionData data = AscensionCapability.get(player);
        ElementMastery mastery = data.getMastery();
        if (mastery == ElementMastery.OMNI) return true;
        if (scrollElement == null) return true;
        return mastery.element == scrollElement;
    }

    public static float getMasteryCostMultiplier(Player player, String scrollType) {
        return isMasteryMatched(player, scrollType) ? 0.70f : 1.0f;
    }

    public static float getCircleDrainBoost(Player player, Level level) {
        BlockPos playerPos = player.blockPosition();
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;

        float bestBoost = 1.0f;
        int chunkRadius = 4;

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

    public static int getEffectiveCost(Player player, String scrollType, int rawCost) {
        return Math.max(1, (int) (rawCost * getMasteryCostMultiplier(player, scrollType)));
    }

    public static int getDrainPerTick(Player player, Level level) {
        int baseDrain = 30;
        int insightLevel = AscensionCapability.get(player).getInsightLevel();
        float circleBoost = getCircleDrainBoost(player, level);
        return Math.max(1, (int) ((baseDrain + insightLevel * 2) * circleBoost));
    }
}
