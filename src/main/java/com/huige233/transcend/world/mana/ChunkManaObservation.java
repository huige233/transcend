package com.huige233.transcend.world.mana;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import java.util.Optional;

/** 区块魔力观测数据类。 */
public final class ChunkManaObservation {
    public static final float UNKNOWN_MANA = Float.NaN;
    public static final byte UNKNOWN_TIER = -1;
    public static final int MAX_MAP_RADIUS = 8;

    private ChunkManaObservation() {}

    public record Sample(Optional<Float> mana, @Nullable ChunkManaSavedData.Tier tier,
                         boolean stabilized) {
        public boolean known() { return mana.isPresent(); }
        public float manaOrUnknown() { return mana.orElse(UNKNOWN_MANA); }
        public byte tierOrUnknown() { return tier == null ? UNKNOWN_TIER : (byte) tier.ordinal(); }

        public Optional<Float> extractMultiplier() {
            if (tier == null) return Optional.empty();
            return Optional.of(switch (tier) {
                case EXHAUSTED -> 0.0F;
                case WEAK -> stabilized ? 0.9F : 0.8F;
                case STABLE -> 1.0F;
                case RICH -> 1.1F;
            });
        }
    }

    public record Grid(int radius, float[] mana, byte[] tier, boolean[] stabilized) {
        public int unknownCount() {
            int count = 0;
            for (float value : mana) {
                if (Float.isNaN(value)) count++;
            }
            return count;
        }
    }

    public static Sample observe(ServerLevel level, ChunkPos pos) {
        return observe(ChunkManaSavedData.getIfPresent(level), pos);
    }

    public static Sample observe(@Nullable ChunkManaSavedData data, ChunkPos pos) {
        if (data == null) return new Sample(Optional.empty(), null, false);
        Optional<Float> mana = data.peekMana(pos);
        return new Sample(mana, mana.map(ChunkManaObservation::tierFor).orElse(null),
                data.isStabilized(pos));
    }

    public static Grid observeSquare(@Nullable ChunkManaSavedData data, ChunkPos center, int radius) {
        if (radius < 1 || radius > MAX_MAP_RADIUS) {
            throw new IllegalArgumentException("radius must be between 1 and " + MAX_MAP_RADIUS);
        }
        int side = Math.addExact(Math.multiplyExact(radius, 2), 1);
        int total = Math.multiplyExact(side, side);
        float[] mana = new float[total];
        byte[] tier = new byte[total];
        boolean[] stabilized = new boolean[total];
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                ChunkPos pos = new ChunkPos(Math.addExact(center.x, dx), Math.addExact(center.z, dz));
                int index = (dz + radius) * side + dx + radius;
                Sample sample = observe(data, pos);
                mana[index] = sample.manaOrUnknown();
                tier[index] = sample.tierOrUnknown();
                stabilized[index] = sample.stabilized();
            }
        }
        return new Grid(radius, mana, tier, stabilized);
    }

    public static ChunkManaSavedData.Tier tierFor(float mana) {
        if (mana < ChunkManaSavedData.TIER_WEAK_FLOOR) return ChunkManaSavedData.Tier.EXHAUSTED;
        if (mana < ChunkManaSavedData.TIER_STABLE_FLOOR) return ChunkManaSavedData.Tier.WEAK;
        if (mana < ChunkManaSavedData.TIER_RICH_FLOOR) return ChunkManaSavedData.Tier.STABLE;
        return ChunkManaSavedData.Tier.RICH;
    }
}
