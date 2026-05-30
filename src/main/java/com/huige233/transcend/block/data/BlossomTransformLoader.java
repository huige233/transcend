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
 * Mana Blossom 转化表加载器 — 读取 {@code data/<ns>/blossom_transforms/*.json}，
 * 填充 {@link BlossomTransformRegistry}。
 *
 * <p>每条 JSON 文件 = 一条转化规则。文件名（去 .json）即条目 id。
 * datapack 作者可任意添加/覆盖/删除规则，无需改 Java 代码。
 */
public class BlossomTransformLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogManager.getLogger("TranscendBlossomTransforms");
    private static final Gson GSON = new Gson();
    private static final BlossomTransformLoader INSTANCE = new BlossomTransformLoader();

    private BlossomTransformLoader() {
        super(GSON, "blossom_transforms");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager manager, ProfilerFiller profiler) {
        BlossomTransformRegistry registry = BlossomTransformRegistry.getInstance();
        registry.clear();

        int ok = 0, fail = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                BlossomTransform transform = BlossomTransform.fromJson(id, json);
                registry.register(transform);
                ok++;
            } catch (Exception e) {
                LOGGER.error("Failed to load blossom transform '{}': {}", id, e.getMessage());
                fail++;
            }
        }
        LOGGER.info("Loaded {} blossom transforms ({} failed)", ok, fail);
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }
}
