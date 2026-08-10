package com.huige233.transcend.spell.data;

import com.huige233.transcend.spell.SpellCarrier;

import java.util.EnumMap;
import java.util.Map;

/** 承载器数据注册表。 */
public class CarrierStatsRegistry {

    private static final CarrierStatsRegistry INSTANCE = new CarrierStatsRegistry();

    private final Map<SpellCarrier, CarrierStats> overrides = new EnumMap<>(SpellCarrier.class);

    private CarrierStatsRegistry() {}

    public static CarrierStatsRegistry getInstance() { return INSTANCE; }

    public void clear() { overrides.clear(); }

    public void register(CarrierStats stats) { overrides.put(stats.carrier(), stats); }

    public CarrierStats get(SpellCarrier carrier) {
        return overrides.getOrDefault(carrier, CarrierStats.defaults(carrier));
    }

    public int overrideCount() { return overrides.size(); }
}
