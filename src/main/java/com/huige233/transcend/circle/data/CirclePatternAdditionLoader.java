package com.huige233.transcend.circle.data;

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
 * 法环结构追加条目加载器 — 读取 {@code data/<namespace>/circle_patterns/*.json}，
 * 填充 {@link CirclePatternAdditionRegistry}。
 */
public class CirclePatternAdditionLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogManager.getLogger("TranscendCirclePattern");
    private static final Gson GSON = new Gson();
    private static final CirclePatternAdditionLoader INSTANCE = new CirclePatternAdditionLoader();

    private CirclePatternAdditionLoader() {
        super(GSON, "circle_patterns");
    }

    public static CirclePatternAdditionLoader getInstance() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager manager, ProfilerFiller profiler) {
        CirclePatternAdditionRegistry registry = CirclePatternAdditionRegistry.getInstance();
        registry.clear();

        int ok = 0, fail = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                CirclePatternAddition addition = CirclePatternAddition.fromJson(id, json);
                registry.register(addition);
                ok++;
            } catch (Exception e) {
                LOGGER.error("Failed to load circle pattern addition '{}': {}", id, e.getMessage());
                fail++;
            }
        }
        LOGGER.info("Loaded {} circle pattern additions ({} entries total, {} failed)",
                ok, registry.totalEntries(), fail);
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }
}
