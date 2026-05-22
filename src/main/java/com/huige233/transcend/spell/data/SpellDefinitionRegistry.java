package com.huige233.transcend.spell.data;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 数据驱动法术定义注册表（单例）。由 {@link SpellDefinitionLoader} 在 reload 时填充。
 *
 * <p>有序保留插入顺序（创造栏排序应可预测），按 {@code tier ASC, id ASC} 排序。
 */
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

    /** 返回按 (tier ASC, id ASC) 排序的所有定义 — 创造栏遍历顺序。 */
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
