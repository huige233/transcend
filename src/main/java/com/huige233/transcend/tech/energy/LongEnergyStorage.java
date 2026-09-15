package com.huige233.transcend.tech.energy;


/** 维护容量受限的长整型能量缓冲，提供无溢出的模拟及实际充放电操作。 */
public final class LongEnergyStorage {
    private long energy;
    private final long capacity;
    public LongEnergyStorage(long capacity) { this.capacity = Math.max(0L, capacity); }
    public long stored() { return energy; }
    public long capacity() { return capacity; }
    public long receive(long amount) { return receive(amount, false); }
    public long receive(long amount, boolean simulate) {
        long accepted = Math.min(Math.max(0L, amount), capacity - energy);
        if (!simulate) energy += accepted;
        return accepted;
    }
    public long extract(long amount) { return extract(amount, false); }
    public long extract(long amount, boolean simulate) {
        long taken = Math.min(Math.max(0L, amount), energy);
        if (!simulate) energy -= taken;
        return taken;
    }
    public void set(long value) { energy = Math.max(0L, Math.min(capacity, value)); }
}
