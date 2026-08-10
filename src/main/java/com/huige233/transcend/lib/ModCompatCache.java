package com.huige233.transcend.lib;

import net.minecraftforge.fml.ModList;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 模组兼容性缓存类。 */
public final class ModCompatCache {

    private static final Map<String, Boolean> LOADED_CACHE = new ConcurrentHashMap<>();

    private ModCompatCache() {
    }

    public static boolean isLoaded(String modId) {
        if (modId == null || modId.isBlank()) {
            return false;
        }
        return LOADED_CACHE.computeIfAbsent(modId, id -> ModList.get().isLoaded(id));
    }

    public static void clear() {
        LOADED_CACHE.clear();
    }
}
