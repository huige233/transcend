package com.huige233.transcend.handle;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.world.mana.ChunkManaSavedData;
import com.huige233.transcend.world.mana.LeylineEnvironmentReaction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 魔力浓度自然恢复 + 区块间均衡化事件处理器。
 *
 * <p>三套独立计时：
 * <ul>
 *   <li>{@link #REGEN_INTERVAL} = 100 tick — 自然恢复（带群系系数，不超 DEFAULT_MANA）</li>
 *   <li>{@link ChunkManaSavedData#EQUALIZE_INTERVAL_TICKS} = 10 tick — 区块间魔力均衡化
 *       （新算法：cardinal 邻居对称差值填补，无方向）</li>
 *   <li>{@link #ENV_REACTION_INTERVAL} = 200 tick — 地脉环境反应（粒子/作物/怪物等）</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ManaRegenEvents {

    private static final int REGEN_INTERVAL = 100;
    private static final int ENV_REACTION_INTERVAL = 200;

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;

        long gameTime = serverLevel.getGameTime();

        // ── 区块间魔力均衡化（高频） ─────────────────────────
        Set<ChunkPos> loadedSet = null;
        if (gameTime % ChunkManaSavedData.EQUALIZE_INTERVAL_TICKS == 0) {
            ChunkManaSavedData manaData = ChunkManaSavedData.get(serverLevel);
            loadedSet = new HashSet<>(getLoadedChunkPositions(serverLevel));
            if (!loadedSet.isEmpty()) {
                manaData.equalizePass(loadedSet);
            }
        }

        // ── 自然恢复 ─────────────────────────────────────────
        if (gameTime % REGEN_INTERVAL == 0) {
            ChunkManaSavedData manaData = ChunkManaSavedData.get(serverLevel);
            // 用 Set 去重 — 多玩家视图覆盖时同区块只 regen 一次
            Set<ChunkPos> loaded = (loadedSet != null)
                    ? loadedSet
                    : new HashSet<>(getLoadedChunkPositions(serverLevel));
            for (ChunkPos pos : loaded) {
                float biomeMultiplier = getBiomeRegenMultiplier(serverLevel, pos);
                float regenAmount = ChunkManaSavedData.REGEN_AMOUNT * biomeMultiplier;
                manaData.regenMana(pos, regenAmount);
            }
        }

        // ── 地脉环境反应（独立 200 tick） ───────────────────
        if (gameTime % ENV_REACTION_INTERVAL == 0) {
            ChunkManaSavedData manaData = ChunkManaSavedData.get(serverLevel);
            Set<ChunkPos> loaded = new HashSet<>(getLoadedChunkPositions(serverLevel));
            for (ChunkPos pos : loaded) {
                LeylineEnvironmentReaction.tickChunkReaction(serverLevel, pos, manaData.getMana(pos));
            }
        }
    }

    private static float getBiomeRegenMultiplier(ServerLevel level, ChunkPos chunkPos) {
        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();
        Holder<Biome> biomeHolder = level.getBiome(new net.minecraft.core.BlockPos(x, 64, z));

        if (biomeHolder.is(Biomes.FOREST) || biomeHolder.is(Biomes.FLOWER_FOREST)
                || biomeHolder.is(Biomes.BIRCH_FOREST) || biomeHolder.is(Biomes.OLD_GROWTH_BIRCH_FOREST)
                || biomeHolder.is(Biomes.DARK_FOREST)
                || biomeHolder.is(Biomes.JUNGLE) || biomeHolder.is(Biomes.BAMBOO_JUNGLE)
                || biomeHolder.is(Biomes.SPARSE_JUNGLE)) {
            return 2.0F;
        }
        if (biomeHolder.is(Biomes.MUSHROOM_FIELDS)
                || biomeHolder.is(Biomes.END_HIGHLANDS)
                || biomeHolder.is(Biomes.END_MIDLANDS)) {
            return 3.0F;
        }
        if (biomeHolder.is(Biomes.DESERT) || biomeHolder.is(Biomes.BADLANDS)
                || biomeHolder.is(Biomes.ERODED_BADLANDS)
                || biomeHolder.is(Biomes.THE_VOID)) {
            return 0.5F;
        }
        if (biomeHolder.is(Biomes.NETHER_WASTES) || biomeHolder.is(Biomes.SOUL_SAND_VALLEY)
                || biomeHolder.is(Biomes.CRIMSON_FOREST) || biomeHolder.is(Biomes.WARPED_FOREST)
                || biomeHolder.is(Biomes.BASALT_DELTAS)) {
            return 0.3F;
        }
        return 1.0F;
    }

    private static List<ChunkPos> getLoadedChunkPositions(ServerLevel level) {
        List<ChunkPos> positions = new ArrayList<>();
        int viewDist = level.getServer().getPlayerList().getViewDistance();

        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            ChunkPos playerChunk = player.chunkPosition();
            int range = Math.min(viewDist, 4);
            for (int dx = -range; dx <= range; dx++) {
                for (int dz = -range; dz <= range; dz++) {
                    ChunkPos cp = new ChunkPos(playerChunk.x + dx, playerChunk.z + dz);
                    if (level.hasChunk(cp.x, cp.z)) {
                        positions.add(cp);
                    }
                }
            }
        }
        return positions;
    }
}
