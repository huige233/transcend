package com.huige233.transcend.spell.data;

import com.google.gson.JsonObject;
import com.huige233.transcend.spell.SpellCarrier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/** 承载器数据记录。 */
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
        if (carrier == null) {
            throw new IllegalArgumentException("Unknown spell carrier id: " + carrierId);
        }

        int projSpeed = GsonHelper.getAsInt(json, "projectile_speed", carrier.getDefaultProjectileSpeed());
        float gravity = GsonHelper.getAsFloat(json, "gravity", carrier.getDefaultGravity());
        double aoe = GsonHelper.getAsDouble(json, "aoe_radius", carrier.getDefaultAoeRadius());
        int cd = GsonHelper.getAsInt(json, "base_cooldown", carrier.getDefaultBaseCooldown());

        return new CarrierStats(carrier,
                Math.max(0, Math.min(1000, projSpeed)),
                Float.isFinite(gravity) ? Math.max(-10.0F, Math.min(10.0F, gravity)) : carrier.getDefaultGravity(),
                Double.isFinite(aoe) ? Math.max(0.0D, Math.min(128.0D, aoe)) : carrier.getDefaultAoeRadius(),
                Math.max(1, Math.min(72_000, cd)));
    }

    public static CarrierStats defaults(SpellCarrier c) {
        return new CarrierStats(c, c.getDefaultProjectileSpeed(), c.getDefaultGravity(),
                c.getDefaultAoeRadius(), c.getDefaultBaseCooldown());
    }
}
