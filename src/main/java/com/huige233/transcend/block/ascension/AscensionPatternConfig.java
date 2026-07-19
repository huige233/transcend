package com.huige233.transcend.block.ascension;

import com.google.gson.JsonObject;
import com.huige233.transcend.ascension.AscensionRitual;
import net.minecraft.resources.ResourceLocation;

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
