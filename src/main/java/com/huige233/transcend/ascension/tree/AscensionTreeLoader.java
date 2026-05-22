package com.huige233.transcend.ascension.tree;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class AscensionTreeLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();

    public AscensionTreeLoader() {
        super(GSON, "ascension_trees");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager manager, ProfilerFiller profiler) {
        TreeRegistry registry = TreeRegistry.getInstance();
        registry.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                TreeDefinition tree = TreeDefinition.fromJson(id, json);
                if (tree.getTreeType() != TreeType.ASCENSION) {
                    LOGGER.warn("File in ascension_trees/ has tree_type '{}', expected 'ascension': {}",
                            tree.getTreeType(), id);
                    continue;
                }
                registry.register(tree);
                LOGGER.info("Loaded ascension tree '{}' with {} nodes", id, tree.getNodes().size());
            } catch (Exception e) {
                LOGGER.error("Failed to load ascension tree '{}'", id, e);
            }
        }
    }
}
