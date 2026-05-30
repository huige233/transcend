package com.huige233.transcend.block.ascension;

import com.huige233.transcend.ascension.AscensionRitual;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * R69: 进阶图案配置注册中心（单例）。由 {@link AscensionPatternLoader} 在 reload 时填充。
 *
 * <p>每个 {@link AscensionRitual} 最多一个配置；若加载到重复则后注册覆盖前者并 warn log。
 * 缺失某仪式的配置时，{@link AscensionAnchorBlockEntity#patternFor} 走 hard-coded 默认。
 */
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
