package com.huige233.transcend.block.data;

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
 * Mana Dew 产能配置加载器 — 读取 {@code data/<ns>/dew_production/*.json}，
 * 填充 {@link DewProductionRegistry}。
 */
public class DewProductionLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogManager.getLogger("TranscendDewProduction");
    private static final Gson GSON = new Gson();
    private static final DewProductionLoader INSTANCE = new DewProductionLoader();

    private DewProductionLoader() {
        super(GSON, "dew_production");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager manager, ProfilerFiller profiler) {
        DewProductionRegistry registry = DewProductionRegistry.getInstance();
        registry.clear();

        int ok = 0, fail = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                DewProductionConfig cfg = DewProductionConfig.fromJson(id, json);
                registry.register(cfg);
                ok++;
            } catch (Exception e) {
                LOGGER.error("Failed to load dew production config '{}': {}", id, e.getMessage());
                fail++;
            }
        }
        LOGGER.info("Loaded {} dew production configs ({} failed)", ok, fail);
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }
}
