package com.huige233.transcend.spell.data;

import com.google.gson.JsonObject;
import com.huige233.transcend.items.SpellScrollItem;
import com.huige233.transcend.spell.SpellCarrier;
import com.huige233.transcend.spell.SpellEffect;
import com.huige233.transcend.spell.SpellElement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 数据驱动的法术定义。Datapack 作者可通过在 {@code data/<namespace>/spells/<id>.json} 投放
 * JSON 文件来添加新的预设法术；mod 在资源 reload 时自动注册并暴露给创造栏。
 *
 * <p>JSON Schema:
 * <pre>{@code
 * {
 *   "carrier":      "orb",             // SpellCarrier id (REQUIRED)
 *   "element":      "fire",            // SpellElement id (REQUIRED)
 *   "effect":       "amplify",         // SpellEffect id, "" / omit = none
 *   "base_power":   1.0,               // 默认 1.0
 *   "base_cooldown":1.0,               // 默认 1.0
 *   "display_name": "spell.transcend.fireball",  // 翻译 key（默认 spell.transcend.<id>）
 *   "tier":         1                  // 1..5 — 用于创造栏排序与展示
 * }
 * }</pre>
 *
 * 文件名 (无 .json) 即为 path；namespace 取自资源域。
 */
public record SpellDefinition(
        ResourceLocation id,
        SpellCarrier carrier,
        SpellElement element,
        @Nullable SpellEffect effect,
        float basePower,
        float baseCooldown,
        String displayKey,
        int tier
) {

    public static SpellDefinition fromJson(ResourceLocation id, JsonObject json) {
        String carrierId = GsonHelper.getAsString(json, "carrier");
        String elementId = GsonHelper.getAsString(json, "element");
        String effectId = GsonHelper.getAsString(json, "effect", "");

        SpellCarrier carrier = SpellCarrier.getById(carrierId);
        SpellElement element = SpellElement.getById(elementId);
        SpellEffect effect = effectId.isEmpty() ? null : SpellEffect.getById(effectId);

        float basePower = GsonHelper.getAsFloat(json, "base_power", 1.0F);
        float baseCooldown = GsonHelper.getAsFloat(json, "base_cooldown", 1.0F);
        String defaultKey = "spell." + id.getNamespace() + "." + id.getPath();
        String displayKey = GsonHelper.getAsString(json, "display_name", defaultKey);
        int tier = GsonHelper.getAsInt(json, "tier", 1);

        return new SpellDefinition(id, carrier, element, effect, basePower, baseCooldown, displayKey, tier);
    }

    /** 创建一个完全配置好的 SpellScroll ItemStack，并把 display_name 写入 NBT 让 tooltip 显示。 */
    public ItemStack toItemStack() {
        ItemStack stack = SpellScrollItem.createScroll(carrier, element, effect, basePower, baseCooldown);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("spell_def_id", id.toString());
        tag.putString("display_key", displayKey);
        stack.setHoverName(Component.translatable(displayKey));
        return stack;
    }
}
