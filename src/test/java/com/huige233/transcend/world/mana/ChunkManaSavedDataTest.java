package com.huige233.transcend.world.mana;

import com.huige233.transcend.spell.SpellAspect;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkManaSavedDataTest {
    private static final String DATA_NAME = "transcend_chunk_mana";
    private static final ChunkPos TEST_POS = new ChunkPos(7, -11);

    @Test
    void currentManaBaselineFluxAndStabilizerSaveLoadRoundTrip() {
        ChunkManaSavedData original = new ChunkManaSavedData();
        original.setMana(TEST_POS, 4_200.0F);
        original.setManaBaseline(TEST_POS, 6_200.0F);
        original.addFlux(TEST_POS, 37.5F);
        original.addStabilizer(TEST_POS);

        CompoundTag serialized = original.save(new CompoundTag());
        ChunkManaSavedData restored = ChunkManaSavedData.load(serialized);

        assertEquals(4_200.0F, restored.getMana(TEST_POS));
        assertEquals(6_200.0F, restored.getManaBaseline(TEST_POS));
        assertEquals(37.5F, restored.getFlux(TEST_POS));
        assertTrue(restored.isStabilized(TEST_POS));
        assertEquals(serialized, restored.save(new CompoundTag()));
    }

    @Test
    void absentAndInitializedDefaultManaHaveDistinctSerializedPresence() {
        ChunkManaSavedData absent = new ChunkManaSavedData();
        CompoundTag absentNbt = absent.save(new CompoundTag());

        ChunkManaSavedData initialized = new ChunkManaSavedData();
        initialized.setMana(TEST_POS, ChunkManaSavedData.DEFAULT_MANA);
        CompoundTag initializedNbt = initialized.save(new CompoundTag());

        assertEquals(0, chunks(absentNbt).size(), absentNbt.toString());
        assertEquals(1, chunks(initializedNbt).size(), initializedNbt.toString());
        assertEquals(ChunkManaSavedData.DEFAULT_MANA, chunks(initializedNbt).getCompound(0).getFloat("Mana"));
    }

    @Test
    void repeatedPurePeekDoesNotMaterializeUnknownChunkOrChangeSerializedCounts() {
        ChunkManaSavedData data = new ChunkManaSavedData();
        CompoundTag before = data.save(new CompoundTag());

        assertFalse(data.hasChunkState(TEST_POS));
        assertFalse(data.hasManaState(TEST_POS));
        var first = data.peekMana(TEST_POS);
        var second = data.peekMana(TEST_POS);
        CompoundTag after = data.save(new CompoundTag());

        assertTrue(first.isEmpty(), "absent mana must observe as unknown");
        assertTrue(second.isEmpty(), "repeated absent observation must remain unknown");
        assertFalse(data.hasChunkState(TEST_POS));
        assertFalse(data.hasManaState(TEST_POS));
        assertEquals(chunks(before).size(), chunks(after).size(), () -> nbtDiff("pure-peek", before, after));
        assertEquals(totalKeyCount(before), totalKeyCount(after), () -> nbtDiff("pure-peek", before, after));
        assertEquals(before, after, () -> nbtDiff("pure-peek", before, after));
    }

    @Test
    void trackedChunkCurrentObservableIsExactAndStable() {
        ChunkManaSavedData data = new ChunkManaSavedData();
        data.setMana(TEST_POS, 4_321.0F);
        CompoundTag before = data.save(new CompoundTag());

        ChunkManaObservation.Sample sample = ChunkManaObservation.observe(data, TEST_POS);
        CompoundTag after = data.save(new CompoundTag());

        assertEquals(4_321.0F, sample.mana().orElseThrow());
        assertEquals(ChunkManaSavedData.Tier.STABLE, sample.tier());
        assertEquals(1, data.getTrackedChunkCount());
        assertEquals(before, after, () -> nbtDiff("tracked-current-observable", before, after));
        System.out.println("TASK13_SURFACE_QA tracked=PASS mana=" + sample.mana().orElseThrow()
                + " trackedBefore=1 trackedAfter=" + data.getTrackedChunkCount()
                + " keysBefore=" + totalKeyCount(before) + " keysAfter=" + totalKeyCount(after));
    }

    @Test
    void radiusEightMapObservationLeavesAbsentChunksUnknownAndUntracked() {
        ChunkManaSavedData data = new ChunkManaSavedData();
        CompoundTag before = data.save(new CompoundTag());
        ChunkManaObservation.Grid observation =
                ChunkManaObservation.observeSquare(data, new ChunkPos(0, 0), 8);

        CompoundTag after = data.save(new CompoundTag());
        assertEquals(289, observation.mana().length);
        assertEquals(289, observation.unknownCount());
        assertEquals(289, observation.tier().length);
        assertEquals(289, observation.stabilized().length);
        for (float mana : observation.mana()) assertTrue(Float.isNaN(mana));
        for (byte tier : observation.tier()) assertEquals(ChunkManaObservation.UNKNOWN_TIER, tier);
        for (boolean stabilized : observation.stabilized()) assertFalse(stabilized);
        assertEquals(0, data.getTrackedChunkCount(),
                "a real radius-eight map observation must not materialize absent chunks");
        assertEquals(chunks(before).size(), chunks(after).size(),
                () -> nbtDiff("radius-eight-map-observation", before, after));
        assertEquals(totalKeyCount(before), totalKeyCount(after),
                () -> nbtDiff("radius-eight-map-observation", before, after));
        assertEquals(before, after, () -> nbtDiff("radius-eight-map-observation", before, after));
        System.out.println("TASK13_SURFACE_QA absent=PASS coordinates=" + observation.mana().length
                + " unknown=289 trackedBefore=0 trackedAfter=" + data.getTrackedChunkCount()
                + " chunksBefore=" + chunks(before).size() + " chunksAfter=" + chunks(after).size()
                + " keysBefore=" + totalKeyCount(before) + " keysAfter=" + totalKeyCount(after));
    }

    @Test
    void mapObservationRejectsMalformedRadiusWithoutMutation() {
        ChunkManaSavedData data = new ChunkManaSavedData();
        CompoundTag before = data.save(new CompoundTag());

        assertThrows(IllegalArgumentException.class,
                () -> ChunkManaObservation.observeSquare(data, TEST_POS, 0));
        assertThrows(IllegalArgumentException.class,
                () -> ChunkManaObservation.observeSquare(data, TEST_POS, 9));
        assertThrows(ArithmeticException.class,
                () -> ChunkManaObservation.observeSquare(data,
                        new ChunkPos(Integer.MAX_VALUE, Integer.MAX_VALUE), 1));

        assertEquals(0, data.getTrackedChunkCount());
        assertEquals(before, data.save(new CompoundTag()));
    }

    @Test
    void gameplayRegenAndEqualizationStillMaterializeManaState() {
        ChunkManaSavedData regen = new ChunkManaSavedData();
        regen.regenMana(TEST_POS, 1.0F);
        assertTrue(regen.hasManaState(TEST_POS), "gameplay regeneration intentionally initializes mana");

        ChunkManaSavedData equalized = new ChunkManaSavedData();
        ChunkPos neighbor = new ChunkPos(TEST_POS.x + 1, TEST_POS.z);
        equalized.equalizeMana(TEST_POS, new ChunkPos[]{neighbor});
        assertTrue(equalized.hasManaState(TEST_POS), "equalization intentionally initializes its origin");
        assertTrue(equalized.hasManaState(neighbor), "equalization intentionally initializes its neighbor");
        assertEquals(2, equalized.getTrackedChunkCount());
    }

    @Test
    void absentDimensionLookupDoesNotCreateSavedDataContainer(@TempDir Path tempDir) throws Exception {
        SharedConstants.tryDetectVersion();
        markMinecraftBootstrappedForIsolatedTest();
        BuiltInRegistries.REGISTRY.size();
        DimensionDataStorage storage = new DimensionDataStorage(tempDir.toFile(), null);
        Map<String, SavedData> cache = savedDataCache(storage);
        TestServerLevel level = TestServerLevel.allocate(storage);
        Path dataFile = tempDir.resolve(DATA_NAME + ".dat");

        assertFalse(cache.containsKey(DATA_NAME), () -> "unexpected cache before lookup: " + cache);
        assertFalse(dataFile.toFile().exists(), () -> "unexpected data file before lookup: " + dataFile);

        assertNull(ChunkManaSavedData.getIfPresent(level));

        assertNull(cache.get(DATA_NAME), () -> "absent lookup created a SavedData container: " + cache);
        assertFalse(cache.values().stream().anyMatch(ChunkManaSavedData.class::isInstance),
                () -> "absent lookup materialized ChunkManaSavedData: " + cache);
        assertFalse(dataFile.toFile().exists(), () -> "absent lookup created a data file: " + dataFile);
    }

    @Test
    void explicitDefaultBaselineIsPresentAndDistinctFromAbsentBaseline() {
        CompoundTag absent = new ChunkManaSavedData().save(new CompoundTag());
        ChunkManaSavedData explicit = new ChunkManaSavedData();
        explicit.setManaBaseline(TEST_POS, ChunkManaSavedData.DEFAULT_MANA);
        CompoundTag explicitNbt = explicit.save(new CompoundTag());

        assertFalse(new ChunkManaSavedData().hasManaBaseline(TEST_POS));
        assertTrue(explicit.hasManaBaseline(TEST_POS));
        assertTrue(explicit.hasChunkState(TEST_POS));
        assertFalse(explicit.hasManaState(TEST_POS));
        assertEquals(0, chunks(absent).size(), absent.toString());
        assertEquals(1, chunks(explicitNbt).size(),
                () -> "explicit 5000 baseline must have serialized presence; "
                        + nbtDiff("baseline-presence", absent, explicitNbt));
        CompoundTag entry = chunks(explicitNbt).getCompound(0);
        assertEquals(TEST_POS.toLong(), entry.getLong("Pos"));
        assertTrue(entry.contains("Baseline", Tag.TAG_FLOAT), entry.toString());
        assertEquals(ChunkManaSavedData.DEFAULT_MANA, entry.getFloat("Baseline"));
    }

    @Test
    void firstTintCastStartsPositiveProgressWithoutPrematureEstablishedAspect() {
        ChunkManaSavedData data = new ChunkManaSavedData();

        data.recordCastTint(TEST_POS, SpellAspect.BLAZE);
        CompoundTag serialized = data.save(new CompoundTag());

        assertEquals(ChunkManaSavedData.TINT_PER_CAST, data.getTintProgress(TEST_POS), 0.0001F,
                "first tint must increase from zero instead of subtracting and being removed");
        assertNull(data.getChunkAspect(TEST_POS), "partial tint must not establish an aspect before 100");
        assertEquals(1, serialized.getList("Tints", Tag.TAG_COMPOUND).size(), serialized.toString());
        nbtDiff("first-tint", new ChunkManaSavedData().save(new CompoundTag()), serialized);
    }

    @Test
    void partialTintSurvivesSaveLoadBeforeAspectIsEstablished() {
        ChunkManaSavedData data = new ChunkManaSavedData();
        for (int i = 0; i < 25; i++) {
            data.recordCastTint(TEST_POS, SpellAspect.VERDANT);
        }

        CompoundTag serialized = data.save(new CompoundTag());
        ChunkManaSavedData restored = ChunkManaSavedData.load(serialized);
        CompoundTag roundTrip = restored.save(new CompoundTag());

        assertEquals(25.0F, restored.getTintProgress(TEST_POS), 0.0001F,
                () -> "partial tint must survive save/load; " + nbtDiff("partial-tint", serialized, roundTrip));
        assertNull(restored.getChunkAspect(TEST_POS), "25% tint is not an established aspect");
        assertEquals(1, roundTrip.getList("Tints", Tag.TAG_COMPOUND).size(), roundTrip.toString());
        assertEquals("VERDANT", roundTrip.getList("Tints", Tag.TAG_COMPOUND)
                .getCompound(0).getString("Aspect"));
        assertEquals(serialized, roundTrip, () -> nbtDiff("partial-tint-round-trip", serialized, roundTrip));
    }

    @Test
    void establishedAspectAndTintFromLegacyFixtureRemainCompatible() {
        CompoundTag fixture = legacyFixture(3_750.0F);
        ListTag aspects = new ListTag();
        CompoundTag aspect = new CompoundTag();
        aspect.putLong("Pos", TEST_POS.toLong());
        aspect.putString("Aspect", "frost");
        aspect.putFloat("Tint", 100.0F);
        aspects.add(aspect);
        fixture.put("Aspects", aspects);

        ChunkManaSavedData restored = ChunkManaSavedData.load(fixture);
        CompoundTag roundTrip = restored.save(new CompoundTag());

        assertEquals(SpellAspect.FROST, restored.getChunkAspect(TEST_POS));
        assertEquals(100.0F, restored.getTintProgress(TEST_POS));
        assertEquals(SpellAspect.FROST.name(), roundTrip.getList("Aspects", Tag.TAG_COMPOUND)
                .getCompound(0).getString("Aspect"));
        assertEquals(100.0F, roundTrip.getList("Aspects", Tag.TAG_COMPOUND)
                .getCompound(0).getFloat("Tint"));
        nbtDiff("legacy-established-round-trip", fixture, roundTrip);
    }

    @Test
    void legacyChunkWithoutNewPresenceFieldsLoadsWithoutMutatingFixture() {
        CompoundTag fixture = legacyFixture(2_750.0F);
        CompoundTag untouched = fixture.copy();

        ChunkManaSavedData restored = ChunkManaSavedData.load(fixture);
        CompoundTag roundTrip = restored.save(new CompoundTag());

        assertEquals(untouched, fixture, "load must not mutate the legacy input fixture");
        assertEquals(2_750.0F, restored.getMana(TEST_POS));
        assertEquals(ChunkManaSavedData.DEFAULT_MANA, restored.getManaBaseline(TEST_POS));
        CompoundTag chunk = chunks(roundTrip).getCompound(0);
        assertFalse(chunk.contains("Baseline"), "missing legacy baseline must remain absent: " + chunk);
        assertEquals(2_750.0F, chunk.getFloat("Mana"));
        nbtDiff("legacy-chunk-round-trip", fixture, roundTrip);
    }

    private static CompoundTag legacyFixture(float mana) {
        CompoundTag root = new CompoundTag();
        ListTag entries = new ListTag();
        CompoundTag chunk = new CompoundTag();
        chunk.putLong("Pos", TEST_POS.toLong());
        chunk.putFloat("Mana", mana);
        entries.add(chunk);
        root.put("Chunks", entries);
        return root;
    }

    private static ListTag chunks(CompoundTag root) {
        return root.getList("Chunks", Tag.TAG_COMPOUND);
    }

    private static int totalKeyCount(CompoundTag root) {
        int count = root.getAllKeys().size();
        for (String key : root.getAllKeys()) {
            Tag value = root.get(key);
            if (value instanceof CompoundTag compound) {
                count += totalKeyCount(compound);
            } else if (value instanceof ListTag list) {
                for (Tag entry : list) {
                    if (entry instanceof CompoundTag compound) count += totalKeyCount(compound);
                }
            }
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, SavedData> savedDataCache(DimensionDataStorage storage) throws Exception {
        Field cacheField = DimensionDataStorage.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        return (Map<String, SavedData>) cacheField.get(storage);
    }

    private static void markMinecraftBootstrappedForIsolatedTest() throws Exception {
        Field bootstrappedField = Bootstrap.class.getDeclaredField("isBootstrapped");
        sun.misc.Unsafe unsafe = unsafe();
        unsafe.putBooleanVolatile(unsafe.staticFieldBase(bootstrappedField),
                unsafe.staticFieldOffset(bootstrappedField), true);
    }

    private static sun.misc.Unsafe unsafe() throws Exception {
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (sun.misc.Unsafe) unsafeField.get(null);
    }

    private static final class TestServerLevel extends ServerLevel {
        private DimensionDataStorage testDataStorage;

        private TestServerLevel() {
            super((MinecraftServer) null, (Executor) null, (LevelStorageSource.LevelStorageAccess) null,
                    (ServerLevelData) null, (ResourceKey<Level>) null, (LevelStem) null,
                    (ChunkProgressListener) null, false, 0L, List.<CustomSpawner>of(), false,
                    (RandomSequences) null);
            throw new UnsupportedOperationException("allocate without invoking the ServerLevel constructor");
        }

        private static TestServerLevel allocate(DimensionDataStorage storage) throws Exception {
            TestServerLevel level = (TestServerLevel) unsafe().allocateInstance(TestServerLevel.class);
            level.testDataStorage = storage;
            return level;
        }

        @Override
        public DimensionDataStorage getDataStorage() {
            return testDataStorage;
        }
    }

    private static String nbtDiff(String label, CompoundTag before, CompoundTag after) {
        String diff = label + " before(keys=" + totalKeyCount(before) + ")=" + before
                + " after(keys=" + totalKeyCount(after) + ")=" + after;
        System.out.println("TASK6_NBT_AUDIT " + diff);
        return diff;
    }
}
