package com.huige233.transcend.circle.data;

import com.huige233.transcend.circle.CircleStructurePattern;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 法环结构追加条目注册表（单例）。由 {@link CirclePatternAdditionLoader} 在 reload 时填充。
 *
 * <p>提供 {@link #getAdditionsForTier(CircleTier)} 供
 * {@link CircleStructurePattern#getPatternForTier(CircleTier)} 在合并时调用。
 */
public class CirclePatternAdditionRegistry {

    private static final CirclePatternAdditionRegistry INSTANCE = new CirclePatternAdditionRegistry();

    private final Map<ResourceLocation, CirclePatternAddition> all = new LinkedHashMap<>();
    private final EnumMap<CircleTier, List<CircleStructurePattern.PatternEntry>> byTier = new EnumMap<>(CircleTier.class);

    private CirclePatternAdditionRegistry() {
        for (CircleTier t : CircleTier.values()) {
            byTier.put(t, new ArrayList<>());
        }
    }

    public static CirclePatternAdditionRegistry getInstance() {
        return INSTANCE;
    }

    public void clear() {
        all.clear();
        for (List<CircleStructurePattern.PatternEntry> l : byTier.values()) {
            l.clear();
        }
    }

    public void register(CirclePatternAddition addition) {
        all.put(addition.id(), addition);
        byTier.get(addition.tier()).addAll(addition.entries());
    }

    /** 返回该 tier 的全部数据驱动追加条目（不可变）。 */
    public List<CircleStructurePattern.PatternEntry> getAdditionsForTier(CircleTier tier) {
        return Collections.unmodifiableList(byTier.get(tier));
    }

    public int totalEntries() {
        return byTier.values().stream().mapToInt(List::size).sum();
    }
}
