package com.huige233.transcend.spell.data;

import com.huige233.transcend.spell.SpellElement;

import java.util.EnumMap;
import java.util.Map;

/** 元素数据注册表。 */
public class ElementStatsRegistry {

    private static final ElementStatsRegistry INSTANCE = new ElementStatsRegistry();

    private final Map<SpellElement, ElementStats> overrides = new EnumMap<>(SpellElement.class);

    private ElementStatsRegistry() {}

    public static ElementStatsRegistry getInstance() {
        return INSTANCE;
    }

    public void clear() {
        overrides.clear();
    }

    public void register(ElementStats stats) {
        overrides.put(stats.element(), stats);
    }

    public ElementStats get(SpellElement element) {
        return overrides.getOrDefault(element, ElementStats.defaults(element));
    }

    public int overrideCount() {
        return overrides.size();
    }
}
