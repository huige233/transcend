package com.huige233.transcend.block.data;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Mana Dew 产能配置注册表（单例）。{@link DewProductionLoader} 在 reload 时填充。
 *
 * <p>查询：{@link #getDefault()} 永远不返回 null —— JSON 缺失时返回 {@link DewProductionConfig#hardDefault()}。
 */
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

    /** 查询默认配置，缺失时返回硬代码兜底。 */
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
