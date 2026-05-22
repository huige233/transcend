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

public class TalentTreeLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();

    public TalentTreeLoader() {
        super(GSON, "talent_trees");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager manager, ProfilerFiller profiler) {
        TreeRegistry registry = TreeRegistry.getInstance();

        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                TreeDefinition tree = TreeDefinition.fromJson(id, json);
                if (tree.getTreeType() != TreeType.TALENT) {
                    LOGGER.warn("File in talent_trees/ has tree_type '{}', expected 'talent': {}",
                            tree.getTreeType(), id);
                    continue;
                }
                registry.register(tree);
                LOGGER.info("Loaded talent tree '{}' ({}) with {} nodes",
                        id, tree.getMageClass().id, tree.getNodes().size());
            } catch (Exception e) {
                LOGGER.error("Failed to load talent tree '{}'", id, e);
            }
        }
    }
}
