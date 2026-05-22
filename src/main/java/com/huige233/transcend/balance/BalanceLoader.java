package com.huige233.transcend.balance;

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
 * Round 35: 数值平衡加载器 — 监听 data/transcend/balance/*.json reload。
 *
 * <p>仅识别 {@code data/transcend/balance/values.json}（id = transcend:values）。
 * 其余文件忽略，避免误识别。
 *
 * <p>失败时静默回退默认值，不阻塞 reload。
 */
public class BalanceLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogManager.getLogger("TranscendBalance");
    private static final Gson GSON = new Gson();
    private static final BalanceLoader INSTANCE = new BalanceLoader();
    private static final ResourceLocation VALUES_ID = new ResourceLocation("transcend", "values");

    private BalanceLoader() { super(GSON, "balance"); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager manager, ProfilerFiller profiler) {
        JsonElement el = objects.get(VALUES_ID);
        if (el == null || !el.isJsonObject()) {
            LOGGER.info("No balance/values.json found, using built-in defaults.");
            return;
        }
        try {
            BalanceConfig.get().applyJson(el.getAsJsonObject());
            LOGGER.info("Applied R19-R30 balance overrides from data/transcend/balance/values.json");
        } catch (Exception e) {
            LOGGER.error("Failed to apply balance overrides: {}", e.getMessage());
        }
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }
}
