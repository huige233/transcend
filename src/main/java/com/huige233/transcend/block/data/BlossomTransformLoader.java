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

/** 加载魔力花转化配方 JSON 的加载器。 */
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
