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
        if (carrier == null) throw new IllegalArgumentException("Unknown spell carrier id: " + carrierId);
        if (element == null) throw new IllegalArgumentException("Unknown spell element id: " + elementId);
        if (!effectId.isEmpty() && effect == null) {
            throw new IllegalArgumentException("Unknown spell effect id: " + effectId);
        }

        float basePower = GsonHelper.getAsFloat(json, "base_power", 1.0F);
        float baseCooldown = GsonHelper.getAsFloat(json, "base_cooldown", 1.0F);
        String defaultKey = "spell." + id.getNamespace() + "." + id.getPath();
        String displayKey = GsonHelper.getAsString(json, "display_name", defaultKey);
        int tier = GsonHelper.getAsInt(json, "tier", 1);

        return new SpellDefinition(id, carrier, element, effect, basePower, baseCooldown, displayKey, tier);
    }

    public ItemStack toItemStack() {
        ItemStack stack = SpellScrollItem.createScroll(carrier, element, effect, basePower, baseCooldown);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("spell_def_id", id.toString());
        tag.putString("display_key", displayKey);
        stack.setHoverName(Component.translatable(displayKey));
        return stack;
    }
}
