package com.huige233.transcend.mana;

import net.minecraft.nbt.CompoundTag;

/** 简易魔力存储实现（含容量）。 */
public class SimpleManaStorage implements IManaHandler {
    private int mana;
    private final int capacity;
    private final int maxReceive;
    private final int maxExtract;

    public SimpleManaStorage(int capacity, int maxReceive, int maxExtract) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.mana = 0;
    }

    @Override
    public int getManaStored() {
        return mana;
    }

    @Override
    public int getMaxManaStored() {
        return capacity;
    }

    @Override
    public int receiveMana(int amount, boolean simulate) {
        if (!canReceive() || amount <= 0) {
            return 0;
        }
        int accepted = Math.min(capacity - mana, Math.min(maxReceive, amount));
        if (!simulate) {
            mana += accepted;
        }
        return accepted;
    }

    @Override
    public int extractMana(int amount, boolean simulate) {
        if (!canExtract() || amount <= 0) {
            return 0;
        }
        int extracted = Math.min(mana, Math.min(maxExtract, amount));
        if (!simulate) {
            mana -= extracted;
        }
        return extracted;
    }

    @Override
    public boolean canReceive() {
        return maxReceive > 0;
    }

    @Override
    public boolean canExtract() {
        return maxExtract > 0;
    }

    public void setMana(int amount) {
        this.mana = Math.max(0, Math.min(capacity, amount));
    }

    private void clamp() {
        if (mana < 0) {
            mana = 0;
        } else if (mana > capacity) {
            mana = capacity;
        }
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Mana", mana);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        mana = tag.getInt("Mana");
        clamp();
    }
}
