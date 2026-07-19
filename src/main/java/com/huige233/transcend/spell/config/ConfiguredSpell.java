package com.huige233.transcend.spell.config;

import com.huige233.transcend.spell.SpellEffect;
import com.huige233.transcend.spell.SpellElement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record ConfiguredSpell(ResourceLocation baseSpellId, SpellElement element, List<SpellEffect> effects) {
    public static final int MAX_EFFECTS = 7;
    private static final int SCHEMA_VERSION = 1;

    public ConfiguredSpell {
        if (baseSpellId == null) throw new IllegalArgumentException("baseSpellId cannot be null");
        element = element == null ? SpellElement.FIRE : element.canonical();
        effects = effects == null ? List.of() : List.copyOf(effects);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("schema_version", SCHEMA_VERSION);
        tag.putString("base_spell", baseSpellId.toString());
        tag.putString("element", element.id);
        ListTag effectList = new ListTag();
        for (SpellEffect effect : effects) effectList.add(StringTag.valueOf(effect.id));
        tag.put("effects", effectList);
        return tag;
    }

    @Nullable
    public static ConfiguredSpell load(CompoundTag tag) {
        ResourceLocation baseId = ResourceLocation.tryParse(tag.getString("base_spell"));
        if (baseId == null) return null;
        SpellElement element = SpellElement.getById(tag.getString("element"));
        if (element == null) return null;
        element = element.canonical();
        List<SpellEffect> effects = new ArrayList<>();
        if (tag.contains("effects", Tag.TAG_LIST)) {
            ListTag list = tag.getList("effects", Tag.TAG_STRING);
            int limit = Math.min(MAX_EFFECTS, list.size());
            for (int i = 0; i < limit; i++) {
                SpellEffect effect = SpellEffect.getById(list.getString(i));
                if (effect == null) return null;
                effects.add(effect);
            }
        }
        return new ConfiguredSpell(baseId, element, effects);
    }
}
