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

@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
/** 魔力恢复事件处理。 */
public class ManaRegenEvents {

    private static final int ENV_REACTION_INTERVAL = 200;

    private static final int NATURAL_TICK_INTERVAL = 20;

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;

        long gameTime = serverLevel.getGameTime();
        ChunkManaSavedData manaData = ChunkManaSavedData.get(serverLevel);
        Set<ChunkPos> loaded = new HashSet<>(getLoadedChunkPositions(serverLevel));

        if (gameTime % ChunkManaSavedData.EQUALIZE_INTERVAL_TICKS == 0 && !loaded.isEmpty()) {
            manaData.equalizePass(loaded);
        }

        if (gameTime % NATURAL_TICK_INTERVAL == 0 && !loaded.isEmpty()) {
            for (ChunkPos pos : loaded) {
                ensureBaseline(serverLevel, manaData, pos);
                manaData.naturalTick(pos, gameTime);
            }
        }

        if (gameTime % ENV_REACTION_INTERVAL == 0 && !loaded.isEmpty()) {
            for (ChunkPos pos : loaded) {
                LeylineEnvironmentReaction.tickChunkReaction(serverLevel, pos, manaData.getMana(pos));
            }
        }
    }

    private static void ensureBaseline(ServerLevel level, ChunkManaSavedData data, ChunkPos pos) {
        if (data.hasManaBaseline(pos)) return;
        float multiplier = getBiomeBaselineMultiplier(level, pos);
        float baseline = ChunkManaSavedData.DEFAULT_MANA * multiplier;
        data.setManaBaseline(pos, baseline);
    }

    private static float getBiomeBaselineMultiplier(ServerLevel level, ChunkPos chunkPos) {
        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();
        Holder<Biome> biome = level.getBiome(new net.minecraft.core.BlockPos(x, 64, z));

        if (biome.is(Biomes.FOREST) || biome.is(Biomes.FLOWER_FOREST)
                || biome.is(Biomes.BIRCH_FOREST) || biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST)
                || biome.is(Biomes.DARK_FOREST)
                || biome.is(Biomes.JUNGLE) || biome.is(Biomes.BAMBOO_JUNGLE)
                || biome.is(Biomes.SPARSE_JUNGLE)) {
            return 1.5F;
        }
        if (biome.is(Biomes.MUSHROOM_FIELDS)
                || biome.is(Biomes.END_HIGHLANDS) || biome.is(Biomes.END_MIDLANDS)) {
            return 2.0F;
        }
        if (biome.is(Biomes.DESERT) || biome.is(Biomes.BADLANDS)
                || biome.is(Biomes.ERODED_BADLANDS) || biome.is(Biomes.THE_VOID)) {
            return 0.5F;
        }
        if (biome.is(Biomes.NETHER_WASTES) || biome.is(Biomes.SOUL_SAND_VALLEY)
                || biome.is(Biomes.CRIMSON_FOREST) || biome.is(Biomes.WARPED_FOREST)
                || biome.is(Biomes.BASALT_DELTAS)) {
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
                    if (level.hasChunk(cp.x, cp.z)) positions.add(cp);
                }
            }
        }
        return positions;
    }
}
