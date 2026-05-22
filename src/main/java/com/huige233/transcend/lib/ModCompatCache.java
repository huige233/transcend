package com.huige233.transcend.lib;

import net.minecraftforge.fml.ModList;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模组存在性缓存工具。
 * <p>
 * 频繁调用 ModList#get().isLoaded(...) 会有额外开销，
 * 该类将查询结果缓存后复用，适合在 Tick 级逻辑中使用。
 */
public final class ModCompatCache {

    private static final Map<String, Boolean> LOADED_CACHE = new ConcurrentHashMap<>();

    private ModCompatCache() {
    }

    /**
     * 查询某个模组是否已加载（带缓存）。
     *
     * @param modId 模组 ID
     * @return true 表示已加载
     */
    public static boolean isLoaded(String modId) {
        if (modId == null || modId.isBlank()) {
            return false;
        }
        return LOADED_CACHE.computeIfAbsent(modId, id -> ModList.get().isLoaded(id));
    }

    /**
     * 清空缓存。
     * <p>
     * 一般情况下不需要调用；仅在测试或特殊热切换场景使用。
     */
    public static void clear() {
        LOADED_CACHE.clear();
    }
}
