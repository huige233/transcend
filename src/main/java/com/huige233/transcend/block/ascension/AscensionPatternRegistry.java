package com.huige233.transcend.block.ascension;

import com.huige233.transcend.ascension.AscensionRitual;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/** 飞升法阵图案注册表。 */
public class AscensionPatternRegistry {

    private static final AscensionPatternRegistry INSTANCE = new AscensionPatternRegistry();

    private final Map<AscensionRitual, AscensionPatternConfig> byRitual = new EnumMap<>(AscensionRitual.class);

    private AscensionPatternRegistry() {}

    public static AscensionPatternRegistry getInstance() {
        return INSTANCE;
    }

    public void clear() {
        byRitual.clear();
    }

    public void register(AscensionPatternConfig config) {
        byRitual.put(config.ritual(), config);
    }

    @Nullable
    public AscensionPatternConfig get(AscensionRitual ritual) {
        return byRitual.get(ritual);
    }

    public int size() {
        return byRitual.size();
    }
}
