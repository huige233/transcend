package com.huige233.transcend.spell.data;

import com.google.gson.JsonObject;
import com.huige233.transcend.spell.SpellCarrier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/**
 * 数据驱动 carrier 数值覆盖。
 * <p>路径：{@code data/<ns>/carrier_stats/<carrier_id>.json}。
 *
 * <p>Schema（全部数值字段可选）:
 * <pre>{@code
 * {
 *   "carrier":         "orb",
 *   "projectile_speed": 4,        // 整数 (vanilla 0=非投射)
 *   "gravity":          0.8,      // 浮点
 *   "aoe_radius":       3.0,      // 浮点
 *   "base_cooldown":    30        // 整数 ticks
 * }
 * }</pre>
 */
public record CarrierStats(
        SpellCarrier carrier,
        int projectileSpeed,
        float gravity,
        double aoeRadius,
        int baseCooldown
) {

    public static CarrierStats fromJson(ResourceLocation id, JsonObject json) {
        String carrierId = GsonHelper.getAsString(json, "carrier");
        SpellCarrier carrier = SpellCarrier.getById(carrierId);

        int projSpeed = GsonHelper.getAsInt(json, "projectile_speed", carrier.projectileSpeed);
        float gravity = GsonHelper.getAsFloat(json, "gravity", carrier.gravity);
        double aoe = GsonHelper.getAsDouble(json, "aoe_radius", carrier.aoeRadius);
        int cd = GsonHelper.getAsInt(json, "base_cooldown", carrier.baseCooldown);

        return new CarrierStats(carrier, projSpeed, gravity, aoe, cd);
    }

    public static CarrierStats defaults(SpellCarrier c) {
        return new CarrierStats(c, c.projectileSpeed, c.gravity, c.aoeRadius, c.baseCooldown);
    }
}
