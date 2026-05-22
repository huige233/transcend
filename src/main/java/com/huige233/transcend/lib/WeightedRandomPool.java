package com.huige233.transcend.lib;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 通用加权随机池。
 * <p>
 * 可用于战利品、技能轮盘、阶段行为选择等场景。
 * 只接受正权重，权重越高被选中概率越大。
 */
public final class WeightedRandomPool<T> {

    private final List<Entry<T>> entries = new ArrayList<>();
    private int totalWeight = 0;

    /**
     * 添加一个候选项。
     *
     * @param value  候选值
     * @param weight 权重（必须 > 0）
     */
    public WeightedRandomPool<T> add(T value, int weight) {
        if (weight <= 0) {
            throw new IllegalArgumentException("weight must be > 0");
        }
        entries.add(new Entry<>(value, weight));
        totalWeight += weight;
        return this;
    }

    /**
     * 随机选择一个候选项。
     *
     * @return 选中的值；若池为空则返回 null
     */
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

    /**
     * 随机选择一个候选项；若为空则返回默认值。
     */
    public T pickOrDefault(RandomSource random, T fallback) {
        T value = pick(random);
        return value != null ? value : fallback;
    }

    /**
     * 当前总权重。
     */
    public int totalWeight() {
        return totalWeight;
    }

    /**
     * 是否为空。
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * 清空随机池。
     */
    public void clear() {
        entries.clear();
        totalWeight = 0;
    }

    /**
     * 只读视图，便于调试或序列化。
     */
    public List<Entry<T>> entries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * 权重条目。
     */
    public record Entry<T>(T value, int weight) {
    }
}
