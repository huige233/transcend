package com.huige233.transcend.spell.data;

import com.google.gson.JsonObject;
import com.huige233.transcend.spell.SpellElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/**
 * 数据驱动元素数值覆盖。每枚 JSON 可重定义某 {@link SpellElement} 的基础数值与粒子颜色。
 * <p>Datapack 路径：{@code data/<ns>/element_stats/<element_id>.json}。
 *
 * <p>JSON Schema（全部字段可选 — 缺失字段保留 Java 枚举默认）:
 * <pre>{@code
 * {
 *   "element":     "fire",      // 必填，要覆盖哪个元素
 *   "base_damage": 7.5,         // 可选，浮点
 *   "mana_cost":   3,           // 可选，整数
 *   "particle_r":  1.0,         // 可选，[0,1]
 *   "particle_g":  0.3,
 *   "particle_b":  0.0
 * }
 * }</pre>
 *
 * 注意：必须重启 / reload 才生效。仅 Mod 内已迁移到 getter 的代码路径会读取覆盖值；
 * 直接字段访问（旧路径）继续使用 enum 默认。后续 round 将逐步迁移。
 */
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

        float baseDamage = GsonHelper.getAsFloat(json, "base_damage", element.baseDamage);
        int manaCost = GsonHelper.getAsInt(json, "mana_cost", element.manaCost);
        float particleR = GsonHelper.getAsFloat(json, "particle_r", element.particleR);
        float particleG = GsonHelper.getAsFloat(json, "particle_g", element.particleG);
        float particleB = GsonHelper.getAsFloat(json, "particle_b", element.particleB);

        return new ElementStats(element, baseDamage, manaCost, particleR, particleG, particleB);
    }

    /** 默认值 — 当 JSON 不存在该元素时返回。 */
    public static ElementStats defaults(SpellElement e) {
        return new ElementStats(e, e.baseDamage, e.manaCost, e.particleR, e.particleG, e.particleB);
    }
}
