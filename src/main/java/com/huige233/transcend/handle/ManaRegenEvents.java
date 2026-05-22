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
 * 机制：
 * - 每100tick: 自然恢复（群系加成），恢复至 DEFAULT_MANA 上限
 * - 每200tick: 魔力均衡化（高→低 转移差值的 2%）
 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ManaRegenEvents {

    private static final int REGEN_INTERVAL = 100;
    private static final int EQUALIZE_INTERVAL = 200;

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;

        long gameTime = serverLevel.getGameTime();

        // 自然恢复
        if (gameTime % REGEN_INTERVAL == 0) {
            ChunkManaSavedData manaData = ChunkManaSavedData.get(serverLevel);
            List<ChunkPos> loaded = getLoadedChunkPositions(serverLevel);
            for (ChunkPos pos : loaded) {
                float biomeMultiplier = getBiomeRegenMultiplier(serverLevel, pos);
                float regenAmount = ChunkManaSavedData.REGEN_AMOUNT * biomeMultiplier;
                manaData.regenMana(pos, regenAmount);
            }
        }

        // 魔力均衡化
        if (gameTime % EQUALIZE_INTERVAL == 0) {
            ChunkManaSavedData manaData = ChunkManaSavedData.get(serverLevel);
            Set<ChunkPos> loaded = new HashSet<>(getLoadedChunkPositions(serverLevel));
            for (ChunkPos pos : loaded) {
                // 只对4个直接相邻区块均衡化（且它们必须也已加载）
                List<ChunkPos> neighbors = new ArrayList<>();
                ChunkPos[] candidates = {
                        new ChunkPos(pos.x + 1, pos.z),
                        new ChunkPos(pos.x - 1, pos.z),
                        new ChunkPos(pos.x, pos.z + 1),
                        new ChunkPos(pos.x, pos.z - 1)
                };
                for (ChunkPos c : candidates) {
                    if (loaded.contains(c)) neighbors.add(c);
                }
                if (!neighbors.isEmpty()) {
                    manaData.equalizeMana(pos, neighbors.toArray(new ChunkPos[0]));
                }

                // 地脉环境反应：与均衡化同间隔（200tick）触发
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
