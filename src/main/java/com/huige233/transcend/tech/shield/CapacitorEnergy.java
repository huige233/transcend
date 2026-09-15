package com.huige233.transcend.tech.shield;

import net.minecraft.nbt.CompoundTag;


/** 统一管理独立或已安装标准电容的有限能量存取、充电和消耗，并过滤非有限数值。 */
public final class CapacitorEnergy {
    public static final float CAPACITY = 200.0F;
    private static final String ENERGY = "Energy";

    private CapacitorEnergy() {}

    public static float stored(CompoundTag tag) {
        return clamp(tag.getFloat(ENERGY));
    }

    public static void set(CompoundTag tag, float energy) {
        tag.putFloat(ENERGY, clamp(energy));
    }

    public static float drain(CompoundTag tag, float amount) {
        float before = stored(tag);
        float drained = Math.min(before, Float.isFinite(amount) ? Math.max(0.0F, amount) : 0.0F);
        set(tag, before - drained);
        return drained;
    }

    public static int receive(CompoundTag tag, int amount, boolean simulate) {
        int accepted = Math.min(Math.max(0, amount), (int) (CAPACITY - stored(tag)));
        if (!simulate && accepted > 0) set(tag, stored(tag) + accepted);
        return accepted;
    }

    private static float clamp(float value) {
        return Float.isFinite(value) ? Math.max(0.0F, Math.min(CAPACITY, value)) : 0.0F;
    }
}
