package com.huige233.transcend.circle;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 法环功能设置 — 每种功能可以有不同的可配置项。
 *
 * <p>这套元数据驱动的设置系统让我们可以为每个 {@link CircleFunctionType} 声明
 * 一组可配置项（开关 / 滑块 / 枚举），由 GUI 自动渲染、网络数据包统一传输、
 * NBT 统一持久化。</p>
 */
public class CircleFunctionSettings {

    /** 设置类型 */
    public enum SettingType {
        /** 开关 0/1 */
        TOGGLE,
        /** 滑块 int (min-max) */
        SLIDER,
        /** 枚举循环（点击在多个字符串值之间循环） */
        ENUM_CYCLE
    }

    /**
     * 单个设置定义。
     *
     * @param id              设置 ID（NBT/网络键）
     * @param translationKey  显示用的本地化键
     * @param type            设置类型
     * @param defaultValue    默认整型值（TOGGLE：0/1；SLIDER：实际值；ENUM_CYCLE：索引）
     * @param minValue        最小值
     * @param maxValue        最大值
     * @param enumValues      ENUM_CYCLE 类型的可选值（其他类型为空列表）
     */
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

        /** 将给定值约束在合法范围内 */
        public int clamp(int value) {
            if (value < minValue) return minValue;
            if (value > maxValue) return maxValue;
            return value;
        }

        /** 计算下一个循环值（用于 GUI 点击） */
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

    /** 获取指定功能类型的可用设置列表 */
    public static List<SettingDef> getSettingsFor(CircleFunctionType type) {
        if (type == null) return List.of();
        return switch (type) {
            // 地脉虹吸：选择抽取模式
            case LEYLINE_SIPHON -> List.of(
                    SettingDef.enumCycle("source_mode", "gui.transcend.setting.source_mode",
                            List.of("environment_first", "storage_first", "environment_only"))
            );

            // 护界法环：选择是否包含友方
            case WARDING_AEGIS -> List.of(
                    SettingDef.toggle("affect_allies", "gui.transcend.setting.affect_allies", true)
            );

            // 天幕飞行：飞行高度限制
            case SKY_MANTLE -> List.of(
                    SettingDef.slider("max_altitude", "gui.transcend.setting.max_altitude", 64, 16, 256)
            );

            // 天候敕令：选择天气类型
            case WEATHER_EDICT -> List.of(
                    SettingDef.enumCycle("weather_type", "gui.transcend.setting.weather_type",
                            List.of("clear", "rain", "thunder"))
            );

            // 时序织机：加速倍率
            case CHRONO_LOOM -> List.of(
                    SettingDef.slider("speed_mult", "gui.transcend.setting.speed_mult", 2, 1, 4)
            );

            // 静界退魔：推力强度 + 是否驱离温和生物
            case QUIET_BOUNDARY -> List.of(
                    SettingDef.slider("push_force", "gui.transcend.setting.push_force", 5, 1, 10),
                    SettingDef.toggle("affect_passives", "gui.transcend.setting.affect_passives", false)
            );

            // 双界之门：传送延迟 + 是否传送宠物
            case TWIN_HORIZON_GATE -> List.of(
                    SettingDef.slider("teleport_delay", "gui.transcend.setting.teleport_delay", 5, 0, 20),
                    SettingDef.toggle("teleport_pets", "gui.transcend.setting.teleport_pets", true)
            );

            // 哨戒鸣钟：是否播放警报声 / 标记发光
            case SENTINEL_ALARM -> List.of(
                    SettingDef.toggle("alarm_sound", "gui.transcend.setting.alarm_sound", true),
                    SettingDef.toggle("mark_glowing", "gui.transcend.setting.mark_glowing", true)
            );

            // 翠收法阵：是否自动补种
            case VERDANT_REAPING -> List.of(
                    SettingDef.toggle("auto_replant", "gui.transcend.setting.auto_replant", true)
            );

            // 维度锚：是否发送通知
            case DIMENSIONAL_ANCHOR -> List.of(
                    SettingDef.toggle("notify_owner", "gui.transcend.setting.notify_owner", true)
            );

            // 虚空钻井：是否启用稳定器 + 抽取速率
            case VOID_BORE -> List.of(
                    SettingDef.toggle("use_stabilizer", "gui.transcend.setting.use_stabilizer", true),
                    SettingDef.slider("extraction_rate", "gui.transcend.setting.extraction_rate", 5, 1, 10)
            );

            // 默认：大多数功能没有额外设置
            default -> List.of();
        };
    }

    /** 将设置值保存到 NBT */
    public static void saveSettings(CompoundTag tag, Map<String, Integer> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        CompoundTag settings = new CompoundTag();
        values.forEach(settings::putInt);
        tag.put("FunctionSettings", settings);
    }

    /** 从 NBT 读取设置值 */
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

    /** 获取设置值（如果不存在则返回默认值） */
    public static int getValue(Map<String, Integer> values, SettingDef def) {
        if (values == null) return def.defaultValue();
        return values.getOrDefault(def.id(), def.defaultValue());
    }

    /** 在指定功能类型的设置中按 ID 查找定义 */
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
