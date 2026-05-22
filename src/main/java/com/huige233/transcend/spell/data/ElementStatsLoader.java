package com.huige233.transcend.spell.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * 元素数值覆盖加载器 — 读取 {@code data/<ns>/element_stats/*.json}，
 * 填充 {@link ElementStatsRegistry}。
 */
public class ElementStatsLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogManager.getLogger("TranscendElementStats");
    private static final Gson GSON = new Gson();
    private static final ElementStatsLoader INSTANCE = new ElementStatsLoader();

    private ElementStatsLoader() {
        super(GSON, "element_stats");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager manager, ProfilerFiller profiler) {
        ElementStatsRegistry registry = ElementStatsRegistry.getInstance();
        registry.clear();

        int ok = 0, fail = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                ElementStats stats = ElementStats.fromJson(id, json);
                registry.register(stats);
                ok++;
            } catch (Exception e) {
                LOGGER.error("Failed to load element stats '{}': {}", id, e.getMessage());
                fail++;
            }
        }
        LOGGER.info("Loaded {} element stat overrides ({} failed)", ok, fail);
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }
}
