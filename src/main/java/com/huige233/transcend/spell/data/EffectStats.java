package com.huige233.transcend.spell.data;

import com.google.gson.JsonObject;
import com.huige233.transcend.spell.SpellEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/**
 * 数据驱动 effect 数值覆盖。
 * <p>路径：{@code data/<ns>/effect_stats/<effect_id>.json}。
 *
 * <p>Schema:
 * <pre>{@code
 * {
 *   "effect":          "amplify",
 *   "extra_mana_cost": 3
 * }
 * }</pre>
 */
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
