package com.huige233.transcend.lib;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 加权随机抽取池<T>。 */
public final class WeightedRandomPool<T> {

    private final List<Entry<T>> entries = new ArrayList<>();
    private int totalWeight = 0;

    public WeightedRandomPool<T> add(T value, int weight) {
        if (weight <= 0) {
            throw new IllegalArgumentException("weight must be > 0");
        }
        entries.add(new Entry<>(value, weight));
        totalWeight += weight;
        return this;
    }

    public T pick(RandomSource random) {
        if (entries.isEmpty() || totalWeight <= 0) {
            return null;
        }
        int point = random.nextInt(totalWeight);
        int cursor = 0;
        for (Entry<T> entry : entries) {
            cursor += entry.weight;
            if (point < cursor) {
                return entry.value;
            }
        }
        return entries.get(entries.size() - 1).value;
    }

    public T pickOrDefault(RandomSource random, T fallback) {
        T value = pick(random);
        return value != null ? value : fallback;
    }

    public int totalWeight() {
        return totalWeight;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public void clear() {
        entries.clear();
        totalWeight = 0;
    }

    public List<Entry<T>> entries() {
        return Collections.unmodifiableList(entries);
    }

    public record Entry<T>(T value, int weight) {
    }
}
