package com.huige233.transcend.spell.data;

import com.google.gson.JsonObject;
import com.huige233.transcend.spell.SpellElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public record ElementStats(
        SpellElement element,
        float baseDamage,
        int manaCost,
        float particleR,
        float particleG,
        float particleB
) {

    public static ElementStats fromJson(ResourceLocation id, JsonObject json) {
        String elemId = GsonHelper.getAsString(json, "element");
        SpellElement element = SpellElement.getById(elemId);
        if (element == null) {
            throw new IllegalArgumentException("Unknown spell element id: " + elemId);
        }

        float baseDamage = GsonHelper.getAsFloat(json, "base_damage", element.getDefaultBaseDamage());
        int manaCost = GsonHelper.getAsInt(json, "mana_cost", element.getDefaultManaCost());
        float particleR = GsonHelper.getAsFloat(json, "particle_r", element.getDefaultParticleR());
        float particleG = GsonHelper.getAsFloat(json, "particle_g", element.getDefaultParticleG());
        float particleB = GsonHelper.getAsFloat(json, "particle_b", element.getDefaultParticleB());

        return new ElementStats(element,
                finiteClamp(baseDamage, element.getDefaultBaseDamage(), 0.0F, 1_000_000.0F),
                Math.max(0, Math.min(1_000_000, manaCost)),
                finiteClamp(particleR, element.getDefaultParticleR(), 0.0F, 1.0F),
                finiteClamp(particleG, element.getDefaultParticleG(), 0.0F, 1.0F),
                finiteClamp(particleB, element.getDefaultParticleB(), 0.0F, 1.0F));
    }

    public static ElementStats defaults(SpellElement e) {
        return new ElementStats(e, e.getDefaultBaseDamage(), e.getDefaultManaCost(),
                e.getDefaultParticleR(), e.getDefaultParticleG(), e.getDefaultParticleB());
    }

    private static float finiteClamp(float value, float fallback, float min, float max) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }
}
