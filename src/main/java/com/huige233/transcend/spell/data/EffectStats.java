package com.huige233.transcend.spell.data;

import com.google.gson.JsonObject;
import com.huige233.transcend.spell.SpellEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/** 效果数据记录。 */
public record EffectStats(SpellEffect effect, int extraManaCost) {

    public static EffectStats fromJson(ResourceLocation id, JsonObject json) {
        String effectId = GsonHelper.getAsString(json, "effect");
        SpellEffect effect = SpellEffect.getById(effectId);
        if (effect == null) {
            throw new IllegalArgumentException("Unknown spell effect id: " + effectId);
        }
        int extra = GsonHelper.getAsInt(json, "extra_mana_cost", effect.extraManaCost);
        return new EffectStats(effect, extra);
    }

    public static EffectStats defaults(SpellEffect e) {
        return new EffectStats(e, e.extraManaCost);
    }
}
