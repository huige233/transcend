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

public class CarrierStatsLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogManager.getLogger("TranscendCarrierStats");
    private static final Gson GSON = new Gson();
    private static final CarrierStatsLoader INSTANCE = new CarrierStatsLoader();

    private CarrierStatsLoader() { super(GSON, "carrier_stats"); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager manager, ProfilerFiller profiler) {
        CarrierStatsRegistry registry = CarrierStatsRegistry.getInstance();
        registry.clear();

        int ok = 0, fail = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                CarrierStats stats = CarrierStats.fromJson(id, json);
                registry.register(stats);
                ok++;
            } catch (Exception e) {
                LOGGER.error("Failed to load carrier stats '{}': {}", id, e.getMessage());
                fail++;
            }
        }
        LOGGER.info("Loaded {} carrier stat overrides ({} failed)", ok, fail);
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }
}
