package com.huige233.transcend.world.mana;

import com.huige233.transcend.spell.SpellAspect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public class ChunkManaSavedData extends SavedData {

    private static final String DATA_NAME = "transcend_chunk_mana";
    private static final Random RAND = new Random();

    public static final float MIN_INITIAL_MANA    = 100.0F;
    public static final float MAX_INITIAL_MANA    = 10000.0F;
    public static final float DEFAULT_MANA        = 5000.0F;
    public static final float MAX_MANA            = 15000.0F;

    public static final float REGEN_RATE          = 0.002F;

    public static final float FLUCTUATION_AMP     = 300.0F;

    private static final long DAY_TICKS           = 24000L;

    private static final long MOON_CYCLE_TICKS    = DAY_TICKS * 8L;

    public static final float MOON_FLUCTUATION_AMP = 150.0F;

    public static final float MAX_FLUX            = 1000.0F;

    public static final float FLUX_TRIGGER_FLOOR  = 1500.0F;

    public static final float FLUX_PER_OVERDRAW   = 0.5F;

    public static final float FLUX_NATURAL_DECAY  = 0.01F;

    public static final float MIN_REGEN_RATIO     = 0.1F;

    public static final float FLUX_RAMPAGE_THRESHOLD = 800.0F;

    public static final float SAME_ASPECT_DISCOUNT  = 0.15F;

    public static final float OPP_ASPECT_SURCHARGE  = 0.15F;

    public static final float TINT_PER_CAST         = 1.0F;

    public static final float TINT_FADE_PER_DAY     = 1.0F;

    public static final int   EQUALIZE_INTERVAL_TICKS = 10;
    public static final float EQUALIZE_RATE            = 0.15F;
    public static final float PER_PAIR_LIMIT_RATIO     = 0.25F;

    public static final float TIER_WEAK_FLOOR         = 1000.0F;
    public static final float TIER_STABLE_FLOOR       = 2500.0F;
    public static final float TIER_RICH_FLOOR         = 7500.0F;
    public static final float STABILIZED_EXTRACT_FLOOR = 750.0F;

    public enum Tier { EXHAUSTED, WEAK, STABLE, RICH }

    private final Map<Long, Float>       manaMap       = new HashMap<>();
    private final Map<Long, Float>       baselineMap   = new HashMap<>();
    private final Map<Long, Float>       fluxMap       = new HashMap<>();
    private final Map<Long, SpellAspect> aspectMap     = new HashMap<>();
    private final Map<Long, Float>       tintMap       = new HashMap<>();
    private final Map<Long, SpellAspect> tintAspectMap = new HashMap<>();
    private final Set<Long>              stabilizedChunks = new HashSet<>();

    public ChunkManaSavedData() {}

    public float getMana(ChunkPos pos) {
        long key = pos.toLong();
        Float v = manaMap.get(key);
        if (v == null) {
            float initial = MIN_INITIAL_MANA + RAND.nextFloat() * (MAX_INITIAL_MANA - MIN_INITIAL_MANA);
            manaMap.put(key, initial);
            setDirty();
            return initial;
        }
        return v;
    }

    public Optional<Float> peekMana(ChunkPos pos) {
        return Optional.ofNullable(manaMap.get(pos.toLong()));
    }

    public boolean hasManaState(ChunkPos pos) {
        return manaMap.containsKey(pos.toLong());
    }

    public boolean hasChunkState(ChunkPos pos) {
        long key = pos.toLong();
        return manaMap.containsKey(key)
                || baselineMap.containsKey(key)
                || fluxMap.containsKey(key)
                || aspectMap.containsKey(key)
                || tintMap.containsKey(key)
                || stabilizedChunks.contains(key);
    }

    public void setMana(ChunkPos pos, float value) {
        manaMap.put(pos.toLong(), Math.max(0, Math.min(value, MAX_MANA)));
        setDirty();
    }

    public float getManaPercent(ChunkPos pos) {
        return getMana(pos) / DEFAULT_MANA;
    }

    public float getManaBaseline(ChunkPos pos) {
        return baselineMap.getOrDefault(pos.toLong(), DEFAULT_MANA);
    }

    public boolean hasManaBaseline(ChunkPos pos) {
        return baselineMap.containsKey(pos.toLong());
    }

    public void setManaBaseline(ChunkPos pos, float baseline) {
        float clamped = Math.max(100f, Math.min(MAX_MANA, baseline));
        baselineMap.put(pos.toLong(), clamped);
        setDirty();
    }

    public float getFlux(ChunkPos pos) {
        return fluxMap.getOrDefault(pos.toLong(), 0f);
    }

    public void addFlux(ChunkPos pos, float amount) {
        if (amount <= 0) return;
        long key = pos.toLong();
        float cur = fluxMap.getOrDefault(key, 0f);
        fluxMap.put(key, Math.min(MAX_FLUX, cur + amount));
        setDirty();
    }

    public float dissolveFlux(ChunkPos pos, float amount) {
        if (amount <= 0) return 0;
        long key = pos.toLong();
        float cur = fluxMap.getOrDefault(key, 0f);
        if (cur <= 0) return 0;
        float actual = Math.min(cur, amount);
        fluxMap.put(key, cur - actual);
        setDirty();
        return actual;
    }

    public boolean isRampaging(ChunkPos pos) {
        return getFlux(pos) >= FLUX_RAMPAGE_THRESHOLD;
    }

    private float fluxRegenCoeff(float flux) {
        if (flux <= 0) return 1.0f;
        float t = Math.min(flux / MAX_FLUX, 1.0f);
        return 1.0f - t * (1.0f - MIN_REGEN_RATIO);
    }

    @Nullable
    public SpellAspect getChunkAspect(ChunkPos pos) {
        return aspectMap.get(pos.toLong());
    }

    public float getTintProgress(ChunkPos pos) {
        return tintMap.getOrDefault(pos.toLong(), 0f);
    }

    public void recordCastTint(ChunkPos pos, @Nullable SpellAspect castAspect) {
        if (castAspect == null) return;
        long key = pos.toLong();
        SpellAspect current = aspectMap.get(key);
        SpellAspect tintAspect = current != null ? current : tintAspectMap.get(key);
        if (tintAspect == null) {
            tintAspectMap.put(key, castAspect);
            tintMap.put(key, TINT_PER_CAST);
        } else if (castAspect == tintAspect) {

            float tint = tintMap.getOrDefault(key, 0f) + TINT_PER_CAST;
            tintMap.put(key, Math.min(100f, tint));
        } else {

            float tint = tintMap.getOrDefault(key, 0f) - TINT_PER_CAST * 0.5f;
            if (tint <= 0) {

                aspectMap.remove(key);
                tintMap.remove(key);
                tintAspectMap.remove(key);
            } else {
                tintMap.put(key, tint);
            }
        }

        if (tintMap.getOrDefault(key, 0f) >= 100f) {
            aspectMap.put(key, castAspect);
            tintAspectMap.remove(key);
            tintMap.put(key, 100f);
        }
        setDirty();
    }

    public float getManaCostMultiplier(ChunkPos pos, @Nullable SpellAspect castAspect) {
        if (castAspect == null) return 1.0f;
        SpellAspect chunkAspect = getChunkAspect(pos);
        if (chunkAspect == null) return 1.0f;
        if (chunkAspect == castAspect) return 1.0f - SAME_ASPECT_DISCOUNT;
        if (chunkAspect.isOpposite(castAspect)) return 1.0f + OPP_ASPECT_SURCHARGE;
        return 1.0f;
    }

    public static int applyManaCostMultiplier(int manaCost, float multiplier) {
        if (!Float.isFinite(multiplier) || multiplier <= 0.0F) {
            throw new IllegalArgumentException("chunk mana multiplier must be finite and positive");
        }
        if (manaCost <= 0) return 0;
        double adjusted = Math.floor(manaCost * (double) multiplier);
        return Math.max(1, (int) Math.min(Integer.MAX_VALUE, adjusted));
    }

    public void naturalTick(ChunkPos pos, long gameTime) {
        long key = pos.toLong();
        float mana = getMana(pos);
        float baseline = getManaBaseline(pos);
        float flux = getFlux(pos);

        float diff = baseline - mana;
        float regenCoeff = fluxRegenCoeff(flux);
        float regenAmount = diff * REGEN_RATE * regenCoeff;

        if (Math.abs(regenAmount) > 0.001f) {
            setMana(pos, mana + regenAmount);
        }

        double dayAngle = (gameTime % DAY_TICKS) * (2 * Math.PI / DAY_TICKS);
        double moonAngle = (gameTime % MOON_CYCLE_TICKS) * (2 * Math.PI / MOON_CYCLE_TICKS);
        float wave = (float)(Math.sin(dayAngle) * FLUCTUATION_AMP
                           + Math.sin(moonAngle) * MOON_FLUCTUATION_AMP);

        float targetWithWave = Math.max(0, Math.min(MAX_MANA, baseline + wave));
        float waveContrib = (targetWithWave - getMana(pos)) * 0.001f;
        if (Math.abs(waveContrib) > 0.01f) {
            setMana(pos, getMana(pos) + waveContrib);
        }

        if (flux > 0) {
            dissolveFlux(pos, FLUX_NATURAL_DECAY);
        }

        if (gameTime % DAY_TICKS == 0) {
            float tint = tintMap.getOrDefault(key, 0f);
            if (tint > 0) {
                float newTint = tint - TINT_FADE_PER_DAY;
                if (newTint <= 0) {
                    tintMap.remove(key);
                    aspectMap.remove(key);
                    tintAspectMap.remove(key);
                } else {
                    tintMap.put(key, newTint);
                }
                setDirty();
            }
        }
    }

    public float consumeMana(ChunkPos pos, float amount) {
        float current = getMana(pos);
        if (current <= 0) return 0;
        float consumed = Math.min(current, amount);
        setMana(pos, current - consumed);
        return consumed;
    }

    public boolean hasMana(ChunkPos pos, float amount) {
        return getMana(pos) >= amount;
    }

    public float consumeManaSafe(ChunkPos pos, float requested) {
        if (requested <= 0) return 0;
        float current = getMana(pos);
        float floor = getExtractFloor(pos);
        if (current <= floor) return 0;
        float multiplier = getExtractMultiplier(pos);
        if (multiplier <= 0) return 0;

        float available = (current - floor) * multiplier;
        float consumed = Math.min(requested, available);
        float newMana = current - consumed;
        setMana(pos, newMana);

        if (newMana < FLUX_TRIGGER_FLOOR) {
            float overdraw = FLUX_TRIGGER_FLOOR - newMana;
            addFlux(pos, overdraw * FLUX_PER_OVERDRAW);
        }

        return consumed;
    }

    public void regenMana(ChunkPos pos, float amount) {
        float current = getMana(pos);
        float baseline = getManaBaseline(pos);
        if (current >= baseline) return;
        setMana(pos, Math.min(baseline, current + amount));
    }

    public void equalizeMana(ChunkPos pos, ChunkPos[] neighbors) {
        Set<ChunkPos> set = new HashSet<>();
        set.add(pos);
        for (ChunkPos n : neighbors) set.add(n);
        equalizePass(set);
    }

    public void equalizePass(Set<ChunkPos> loadedChunks) {
        if (loadedChunks == null || loadedChunks.size() < 2) return;
        Map<Long, Float> deltas = new HashMap<>(loadedChunks.size());
        for (ChunkPos a : loadedChunks) {
            float manaA = getMana(a);
            ChunkPos[] outDirs = { new ChunkPos(a.x + 1, a.z), new ChunkPos(a.x, a.z + 1) };
            for (ChunkPos b : outDirs) {
                if (!loadedChunks.contains(b)) continue;
                float manaB = getMana(b);
                float diff = manaA - manaB;
                if (diff == 0F) continue;
                float transfer = diff * EQUALIZE_RATE;
                float cap = Math.max(manaA, manaB) * PER_PAIR_LIMIT_RATIO;
                if (transfer >  cap) transfer =  cap;
                if (transfer < -cap) transfer = -cap;
                deltas.merge(a.toLong(), -transfer, Float::sum);
                deltas.merge(b.toLong(),  transfer, Float::sum);
            }
        }
        if (deltas.isEmpty()) return;
        for (Map.Entry<Long, Float> e : deltas.entrySet()) {
            ChunkPos pos = new ChunkPos(e.getKey());
            setMana(pos, getMana(pos) + e.getValue());
        }
    }

    public Tier getTier(ChunkPos pos) {
        float mana = getMana(pos);
        if (mana < TIER_WEAK_FLOOR)   return Tier.EXHAUSTED;
        if (mana < TIER_STABLE_FLOOR) return Tier.WEAK;
        if (mana < TIER_RICH_FLOOR)   return Tier.STABLE;
        return Tier.RICH;
    }

    public boolean isStabilized(ChunkPos pos) { return stabilizedChunks.contains(pos.toLong()); }

    public boolean addStabilizer(ChunkPos pos) {
        boolean added = stabilizedChunks.add(pos.toLong());
        if (added) setDirty();
        return added;
    }

    public void removeStabilizer(ChunkPos pos) {
        if (stabilizedChunks.remove(pos.toLong())) setDirty();
    }

    public float getExtractMultiplier(ChunkPos pos) {
        boolean stabilized = isStabilized(pos);
        return switch (getTier(pos)) {
            case EXHAUSTED -> 0.0F;
            case WEAK      -> stabilized ? 0.9F : 0.8F;
            case STABLE    -> 1.0F;
            case RICH      -> 1.1F;
        };
    }

    public float getExtractFloor(ChunkPos pos) {
        return isStabilized(pos) ? STABILIZED_EXTRACT_FLOOR : TIER_WEAK_FLOOR;
    }

    public int getTrackedChunkCount() { return manaMap.size(); }

    public static ChunkManaSavedData load(CompoundTag tag) {
        ChunkManaSavedData data = new ChunkManaSavedData();

        ListTag list = tag.getList("Chunks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            long key = e.getLong("Pos");
            if (e.contains("Mana", Tag.TAG_FLOAT)) data.manaMap.put(key, e.getFloat("Mana"));
            if (e.contains("Baseline")) data.baselineMap.put(key, e.getFloat("Baseline"));
        }

        if (tag.contains("Flux", Tag.TAG_LIST)) {
            ListTag fluxList = tag.getList("Flux", Tag.TAG_COMPOUND);
            for (int i = 0; i < fluxList.size(); i++) {
                CompoundTag e = fluxList.getCompound(i);
                data.fluxMap.put(e.getLong("Pos"), e.getFloat("Val"));
            }
        }

        if (tag.contains("Aspects", Tag.TAG_LIST)) {
            ListTag aspectList = tag.getList("Aspects", Tag.TAG_COMPOUND);
            for (int i = 0; i < aspectList.size(); i++) {
                CompoundTag e = aspectList.getCompound(i);
                long pos = e.getLong("Pos");
                String name = e.getString("Aspect");
                SpellAspect aspect = SpellAspect.byName(name);
                if (aspect != null) {
                    data.aspectMap.put(pos, aspect);
                    data.tintMap.put(pos, e.getFloat("Tint"));
                }
            }
        }

        if (tag.contains("Tints", Tag.TAG_LIST)) {
            ListTag tintList = tag.getList("Tints", Tag.TAG_COMPOUND);
            for (int i = 0; i < tintList.size(); i++) {
                CompoundTag e = tintList.getCompound(i);
                long pos = e.getLong("Pos");
                SpellAspect aspect = SpellAspect.byName(e.getString("Aspect"));
                float tint = e.getFloat("Tint");
                if (aspect != null && tint > 0 && !data.aspectMap.containsKey(pos)) {
                    data.tintAspectMap.put(pos, aspect);
                    data.tintMap.put(pos, Math.min(100f, tint));
                }
            }
        }

        if (tag.contains("Stabilizers", Tag.TAG_LONG_ARRAY)) {
            for (long k : tag.getLongArray("Stabilizers")) data.stabilizedChunks.add(k);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {

        ListTag list = new ListTag();
        Set<Long> chunkKeys = new HashSet<>(manaMap.keySet());
        chunkKeys.addAll(baselineMap.keySet());
        for (Long key : chunkKeys) {
            CompoundTag c = new CompoundTag();
            c.putLong("Pos", key);
            Float mana = manaMap.get(key);
            if (mana != null) c.putFloat("Mana", mana);
            Float baseline = baselineMap.get(key);
            if (baseline != null) c.putFloat("Baseline", baseline);
            list.add(c);
        }
        tag.put("Chunks", list);

        ListTag fluxList = new ListTag();
        for (Map.Entry<Long, Float> e : fluxMap.entrySet()) {
            if (e.getValue() <= 0) continue;
            CompoundTag c = new CompoundTag();
            c.putLong("Pos", e.getKey());
            c.putFloat("Val", e.getValue());
            fluxList.add(c);
        }
        tag.put("Flux", fluxList);

        ListTag aspectList = new ListTag();
        for (Map.Entry<Long, SpellAspect> e : aspectMap.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putLong("Pos", e.getKey());
            c.putString("Aspect", e.getValue().name());
            c.putFloat("Tint", tintMap.getOrDefault(e.getKey(), 0f));
            aspectList.add(c);
        }
        tag.put("Aspects", aspectList);

        ListTag tintList = new ListTag();
        for (Map.Entry<Long, SpellAspect> e : tintAspectMap.entrySet()) {
            float tint = tintMap.getOrDefault(e.getKey(), 0f);
            if (tint <= 0) continue;
            CompoundTag c = new CompoundTag();
            c.putLong("Pos", e.getKey());
            c.putString("Aspect", e.getValue().name());
            c.putFloat("Tint", tint);
            tintList.add(c);
        }
        tag.put("Tints", tintList);

        long[] arr = new long[stabilizedChunks.size()];
        int i = 0;
        for (Long k : stabilizedChunks) arr[i++] = k;
        tag.put("Stabilizers", new LongArrayTag(arr));

        return tag;
    }

    public static ChunkManaSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                ChunkManaSavedData::load,
                ChunkManaSavedData::new,
                DATA_NAME
        );
    }

    @Nullable
    public static ChunkManaSavedData getIfPresent(ServerLevel level) {
        return level.getDataStorage().get(ChunkManaSavedData::load, DATA_NAME);
    }
}
