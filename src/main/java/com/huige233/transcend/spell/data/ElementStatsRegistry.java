package com.huige233.transcend.spell.data;

import com.huige233.transcend.spell.SpellElement;

import java.util.EnumMap;
import java.util.Map;

/**
 * 元素数值覆盖注册表（单例）。由 {@link ElementStatsLoader} 在 reload 时填充。
 *
 * <p>查询：{@link #get(SpellElement)} 永远不返回 null —— JSON 未定义时返回 enum 默认。
 */
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

    /** 永不返回 null：JSON 缺失时给 enum 默认。 */
    public ElementStats get(SpellElement element) {
        return overrides.getOrDefault(element, ElementStats.defaults(element));
    }

    public int overrideCount() {
        return overrides.size();
    }
}
