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

/** 法术定义 JSON 加载器。 */
public class SpellDefinitionLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogManager.getLogger("TranscendSpellDef");
    private static final Gson GSON = new Gson();
    private static final SpellDefinitionLoader INSTANCE = new SpellDefinitionLoader();

    private SpellDefinitionLoader() {
        super(GSON, "spells");
    }

    public static SpellDefinitionLoader getInstance() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager manager, ProfilerFiller profiler) {
        SpellDefinitionRegistry registry = SpellDefinitionRegistry.getInstance();
        registry.clear();

        int ok = 0, fail = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                SpellDefinition def = SpellDefinition.fromJson(id, json);
                registry.register(def);
                ok++;
            } catch (Exception e) {
                LOGGER.error("Failed to load spell definition '{}': {}", id, e.getMessage());
                fail++;
            }
        }
        LOGGER.info("Loaded {} spell definitions ({} failed)", ok, fail);
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }
}
