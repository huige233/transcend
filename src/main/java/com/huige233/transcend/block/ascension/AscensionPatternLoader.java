package com.huige233.transcend.block.ascension;

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
 * R69: 进阶图案配置加载器 — 读取 {@code data/<ns>/ascension_patterns/*.json}，
 * 填充 {@link AscensionPatternRegistry}。
 *
 * <p>每条 JSON = 一个仪式的图案配置。datapack 作者可任意调整水晶数 / 半径 / 公差 / mana 消耗。
 */
public class AscensionPatternLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogManager.getLogger("TranscendAscensionPatterns");
    private static final Gson GSON = new Gson();
    private static final AscensionPatternLoader INSTANCE = new AscensionPatternLoader();

    private AscensionPatternLoader() {
        super(GSON, "ascension_patterns");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager manager, ProfilerFiller profiler) {
        AscensionPatternRegistry registry = AscensionPatternRegistry.getInstance();
        registry.clear();

        int ok = 0, fail = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                AscensionPatternConfig config = AscensionPatternConfig.fromJson(id, json);
                registry.register(config);
                ok++;
            } catch (Exception e) {
                LOGGER.error("Failed to load ascension pattern '{}': {}", id, e.getMessage());
                fail++;
            }
        }
        LOGGER.info("Loaded {} ascension patterns ({} failed)", ok, fail);
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }
}
