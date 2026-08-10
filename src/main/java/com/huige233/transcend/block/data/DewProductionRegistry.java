package com.huige233.transcend.block.data;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** 魔力露产出配置注册表。 */
public class DewProductionRegistry {

    private static final DewProductionRegistry INSTANCE = new DewProductionRegistry();

    private final Map<ResourceLocation, DewProductionConfig> configs = new HashMap<>();

    private DewProductionRegistry() {}

    public static DewProductionRegistry getInstance() {
        return INSTANCE;
    }

    public void clear() {
        configs.clear();
    }

    public void register(DewProductionConfig config) {
        configs.put(config.id(), config);
    }

    public DewProductionConfig getDefault() {
        return configs.getOrDefault(DewProductionConfig.DEFAULT_ID, DewProductionConfig.hardDefault());
    }

    public DewProductionConfig get(ResourceLocation id) {
        return configs.getOrDefault(id, DewProductionConfig.hardDefault());
    }

    public int size() {
        return configs.size();
    }
}
