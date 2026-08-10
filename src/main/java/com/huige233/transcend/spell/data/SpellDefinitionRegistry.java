package com.huige233.transcend.spell.data;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** 法术定义注册表。 */
public class SpellDefinitionRegistry {

    private static final SpellDefinitionRegistry INSTANCE = new SpellDefinitionRegistry();

    private final Map<ResourceLocation, SpellDefinition> definitions = new LinkedHashMap<>();

    private SpellDefinitionRegistry() {}

    public static SpellDefinitionRegistry getInstance() {
        return INSTANCE;
    }

    public void clear() {
        definitions.clear();
    }

    public void register(SpellDefinition def) {
        definitions.put(def.id(), def);
    }

    public Optional<SpellDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public Collection<SpellDefinition> getAllSorted() {
        return definitions.values().stream()
                .sorted((a, b) -> {
                    int t = Integer.compare(a.tier(), b.tier());
                    if (t != 0) return t;
                    return a.id().toString().compareTo(b.id().toString());
                })
                .toList();
    }

    public Collection<SpellDefinition> getAll() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    public int size() {
        return definitions.size();
    }
}
