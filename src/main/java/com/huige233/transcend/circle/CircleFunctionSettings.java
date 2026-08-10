package com.huige233.transcend.circle;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 法阵功能设置（参数配置）。 */
public class CircleFunctionSettings {

    public enum SettingType {

        TOGGLE,

        SLIDER,

        ENUM_CYCLE
    }

    public record SettingDef(String id, String translationKey, SettingType type,
                             int defaultValue, int minValue, int maxValue,
                             List<String> enumValues) {

        public static SettingDef toggle(String id, String key, boolean defaultOn) {
            return new SettingDef(id, key, SettingType.TOGGLE, defaultOn ? 1 : 0, 0, 1, List.of());
        }

        public static SettingDef slider(String id, String key, int defaultVal, int min, int max) {
            return new SettingDef(id, key, SettingType.SLIDER, defaultVal, min, max, List.of());
        }

        public static SettingDef enumCycle(String id, String key, List<String> values) {
            return new SettingDef(id, key, SettingType.ENUM_CYCLE, 0, 0,
                    Math.max(0, values.size() - 1), List.copyOf(values));
        }

        public int clamp(int value) {
            if (value < minValue) return minValue;
            if (value > maxValue) return maxValue;
            return value;
        }

        public int cycleNext(int current) {
            return switch (type) {
                case TOGGLE -> current == 0 ? 1 : 0;
                case ENUM_CYCLE -> {
                    if (maxValue <= minValue) yield minValue;
                    int next = current + 1;
                    yield next > maxValue ? minValue : next;
                }
                case SLIDER -> {
                    int next = current + 1;
                    yield next > maxValue ? minValue : next;
                }
            };
        }
    }

    public static List<SettingDef> getSettingsFor(CircleFunctionType type) {
        if (type == null) return List.of();
        return switch (type) {

            case LEYLINE_SIPHON -> List.of(
                    SettingDef.enumCycle("source_mode", "gui.transcend.setting.source_mode",
                            List.of("environment_first", "storage_first", "environment_only"))
            );

            case WARDING_AEGIS -> List.of(
                    SettingDef.toggle("affect_allies", "gui.transcend.setting.affect_allies", true)
            );

            case SKY_MANTLE -> List.of(
                    SettingDef.slider("max_altitude", "gui.transcend.setting.max_altitude", 64, 16, 256)
            );

            case WEATHER_EDICT -> List.of(
                    SettingDef.enumCycle("weather_type", "gui.transcend.setting.weather_type",
                            List.of("clear", "rain", "thunder"))
            );

            case CHRONO_LOOM -> List.of(
                    SettingDef.slider("speed_mult", "gui.transcend.setting.speed_mult", 2, 1, 4)
            );

            case QUIET_BOUNDARY -> List.of(
                    SettingDef.slider("push_force", "gui.transcend.setting.push_force", 5, 1, 10),
                    SettingDef.toggle("affect_passives", "gui.transcend.setting.affect_passives", false)
            );

            case TWIN_HORIZON_GATE -> List.of(
                    SettingDef.slider("teleport_delay", "gui.transcend.setting.teleport_delay", 5, 0, 20),
                    SettingDef.toggle("teleport_pets", "gui.transcend.setting.teleport_pets", true)
            );

            case SENTINEL_ALARM -> List.of(
                    SettingDef.toggle("alarm_sound", "gui.transcend.setting.alarm_sound", true),
                    SettingDef.toggle("mark_glowing", "gui.transcend.setting.mark_glowing", true)
            );

            case VERDANT_REAPING -> List.of(
                    SettingDef.toggle("auto_replant", "gui.transcend.setting.auto_replant", true)
            );

            case DIMENSIONAL_ANCHOR -> List.of(
                    SettingDef.toggle("notify_owner", "gui.transcend.setting.notify_owner", true)
            );

            case VOID_BORE -> List.of(
                    SettingDef.toggle("use_stabilizer", "gui.transcend.setting.use_stabilizer", true),
                    SettingDef.slider("extraction_rate", "gui.transcend.setting.extraction_rate", 5, 1, 10)
            );

            default -> List.of();
        };
    }

    public static void saveSettings(CompoundTag tag, Map<String, Integer> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        CompoundTag settings = new CompoundTag();
        values.forEach(settings::putInt);
        tag.put("FunctionSettings", settings);
    }

    public static Map<String, Integer> loadSettings(CompoundTag tag) {
        Map<String, Integer> map = new HashMap<>();
        if (tag != null && tag.contains("FunctionSettings")) {
            CompoundTag settings = tag.getCompound("FunctionSettings");
            for (String key : settings.getAllKeys()) {
                map.put(key, settings.getInt(key));
            }
        }
        return map;
    }

    public static int getValue(Map<String, Integer> values, SettingDef def) {
        if (values == null) return def.defaultValue();
        return values.getOrDefault(def.id(), def.defaultValue());
    }

    public static SettingDef findDef(CircleFunctionType type, String id) {
        if (type == null || id == null) return null;
        for (SettingDef def : getSettingsFor(type)) {
            if (def.id().equals(id)) return def;
        }
        return null;
    }

    private CircleFunctionSettings() {
    }
}
