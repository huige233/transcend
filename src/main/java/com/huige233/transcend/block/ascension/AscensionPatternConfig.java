package com.huige233.transcend.block.ascension;

import com.google.gson.JsonObject;
import com.huige233.transcend.ascension.AscensionRitual;
import net.minecraft.resources.ResourceLocation;

/**
 * R69: 数据驱动的进阶图案配置（一条配置 = 一个 {@link AscensionRitual} 的图案规格）。
 *
 * <p>JSON 格式（{@code data/<ns>/ascension_patterns/*.json}）：
 * <pre>
 * {
 *   "ritual": "AWAKENING",
 *   "crystal_count": 3,
 *   "radius": 3.0,
 *   "angle_tolerance_degrees": 5.0,
 *   "y_tolerance": 1,
 *   "mana_base_cost": 200,
 *   "mana_per_crystal": 100,
 *   "duration_ticks": 100
 * }
 * </pre>
 *
 * <p>数值约束：crystal_count ≥ 3, radius ≥ 2, mana costs ≥ 0, duration_ticks ≥ 20。
 *
 * @param id              数据包路径 id（来自文件名）
 * @param ritual          对应的 {@link AscensionRitual}
 * @param crystalCount    需要的水晶数量 (N)
 * @param radius          水晶到 anchor 中心的距离 (R)
 * @param angleToleranceRad 角度公差（弧度）
 * @param radialTolerance 径向距离公差（块）
 * @param yTolerance      Y 高度差公差（块）
 * @param manaBaseCost    基础 mana 消耗（平均分摊给每个水晶）
 * @param manaPerCrystal  每个水晶额外承担的 mana
 * @param durationTicks   仪式动画总时长
 */
public record AscensionPatternConfig(
        ResourceLocation id,
        AscensionRitual ritual,
        int crystalCount,
        double radius,
        double angleToleranceRad,
        double radialTolerance,
        int yTolerance,
        int manaBaseCost,
        int manaPerCrystal,
        int durationTicks
) {

    public static AscensionPatternConfig fromJson(ResourceLocation id, JsonObject json) {
        String ritualName = requireString(json, "ritual");
        AscensionRitual ritual;
        try {
            ritual = AscensionRitual.valueOf(ritualName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown ritual: " + ritualName);
        }

        int crystalCount = requireInt(json, "crystal_count");
        if (crystalCount < 3) throw new IllegalArgumentException("crystal_count must be >= 3");

        double radius = requireDouble(json, "radius");
        if (radius < 2.0) throw new IllegalArgumentException("radius must be >= 2.0");

        double angleToleranceDeg = json.has("angle_tolerance_degrees")
                ? json.get("angle_tolerance_degrees").getAsDouble() : 5.0;
        if (angleToleranceDeg <= 0) throw new IllegalArgumentException("angle_tolerance must be > 0");
        double angleToleranceRad = Math.toRadians(angleToleranceDeg);

        double radialTolerance = json.has("radial_tolerance")
                ? json.get("radial_tolerance").getAsDouble() : 0.5;

        int yTolerance = json.has("y_tolerance") ? json.get("y_tolerance").getAsInt() : 1;

        int manaBaseCost = requireInt(json, "mana_base_cost");
        int manaPerCrystal = requireInt(json, "mana_per_crystal");
        if (manaBaseCost < 0 || manaPerCrystal < 0) throw new IllegalArgumentException("mana costs must be >= 0");

        int durationTicks = json.has("duration_ticks") ? json.get("duration_ticks").getAsInt() : 100;
        if (durationTicks < 20) throw new IllegalArgumentException("duration_ticks must be >= 20");

        return new AscensionPatternConfig(id, ritual, crystalCount, radius,
                angleToleranceRad, radialTolerance, yTolerance,
                manaBaseCost, manaPerCrystal, durationTicks);
    }

    private static String requireString(JsonObject json, String key) {
        if (!json.has(key)) throw new IllegalArgumentException("Missing field: " + key);
        return json.get(key).getAsString();
    }

    private static int requireInt(JsonObject json, String key) {
        if (!json.has(key)) throw new IllegalArgumentException("Missing field: " + key);
        return json.get(key).getAsInt();
    }

    private static double requireDouble(JsonObject json, String key) {
        if (!json.has(key)) throw new IllegalArgumentException("Missing field: " + key);
        return json.get(key).getAsDouble();
    }
}
