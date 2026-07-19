package com.huige233.transcend.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TranscendParticleConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ConfigData data;

    public static int getParticleCountLimit() {
        return getData().particleCountLimit;
    }

    public static boolean isParticleCountInjectEnabled() {
        return getData().enableParticleCountInject;
    }

    private static ConfigData getData() {
        if (data == null) {
            load();
        }
        return data;
    }

    private static void load() {
        Path configDir = FMLPaths.CONFIGDIR.get();
        Path configFile = configDir.resolve("transcend-particles.json");

        if (Files.exists(configFile)) {
            try {
                String json = Files.readString(configFile);
                data = GSON.fromJson(json, ConfigData.class);
                if (data == null) {
                    data = new ConfigData();
                }
                return;
            } catch (Exception e) {
                System.err.println("[Transcend] Failed to read particle config, using defaults: " + e.getMessage());
            }
        }

        data = new ConfigData();
        try {
            Files.createDirectories(configDir);
            Files.writeString(configFile, GSON.toJson(data));
        } catch (IOException e) {
            System.err.println("[Transcend] Failed to write default particle config: " + e.getMessage());
        }
    }

    private static class ConfigData {
        boolean enableParticleCountInject = true;
        int particleCountLimit = 131072;
    }
}
