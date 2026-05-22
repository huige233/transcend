package com.huige233.transcend.spell.data;

import com.huige233.transcend.spell.SpellEffect;

import java.util.EnumMap;
import java.util.Map;

public class EffectStatsRegistry {

    private static final EffectStatsRegistry INSTANCE = new EffectStatsRegistry();

    private final Map<SpellEffect, EffectStats> overrides = new EnumMap<>(SpellEffect.class);

    private EffectStatsRegistry() {}

    public static EffectStatsRegistry getInstance() { return INSTANCE; }

    public void clear() { overrides.clear(); }

    public void register(EffectStats stats) { overrides.put(stats.effect(), stats); }

    /** 永不返回 null。 */
    public EffectStats get(SpellEffect effect) {
        return overrides.getOrDefault(effect, EffectStats.defaults(effect));
    }

    public int overrideCount() { return overrides.size(); }
}
