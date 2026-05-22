package com.huige233.transcend.spell;

import java.util.Map;

/**
 * 法术组件分阶系统。
 * 将载体、元素、效果按阶级(0-3)分类，对应飞升阶段门控。
 */
public class SpellTierHelper {

    // Carrier tiers (use SpellCarrier enum name strings)
    private static final Map<String, Integer> CARRIER_TIERS = Map.ofEntries(
        // T0 - Stage 0
        Map.entry("ORB", 0), Map.entry("ARROW", 0), Map.entry("SLASH", 0),
        Map.entry("GROUND", 0), Map.entry("DASH", 0),
        // T1 - Stage 1
        Map.entry("BEAM", 1), Map.entry("NOVA", 1), Map.entry("SPIKE", 1),
        Map.entry("TRAP", 1), Map.entry("BARRIER", 1), Map.entry("BREATH", 1),
        Map.entry("RING", 1),
        // T2 - Stage 2
        Map.entry("CHAIN", 2), Map.entry("VORTEX", 2), Map.entry("TELEPORT", 2),
        Map.entry("RAIN", 2), Map.entry("SUMMON", 2)
    );

    // Element tiers
    private static final Map<String, Integer> ELEMENT_TIERS = Map.ofEntries(
        // T0
        Map.entry("FIRE", 0), Map.entry("ICE", 0), Map.entry("THUNDER", 0),
        Map.entry("WIND", 0), Map.entry("EARTH", 0), Map.entry("NATURE", 0),
        Map.entry("POISON", 0), Map.entry("LIGHT", 0), Map.entry("DARK", 0),
        // T1
        Map.entry("HOLY", 1), Map.entry("BLOOD", 1), Map.entry("ACID", 1),
        Map.entry("SONIC", 1),
        // T2
        Map.entry("VOID", 2), Map.entry("TIME", 2), Map.entry("SPACE", 2),
        // T3
        Map.entry("CHAOS", 3), Map.entry("ELDRITCH", 3)
    );

    // Effect tiers
    private static final Map<String, Integer> EFFECT_TIERS = Map.ofEntries(
        // T0
        Map.entry("PIERCING", 0), Map.entry("HEALING", 0), Map.entry("SHIELD", 0),
        Map.entry("DELAYED", 0), Map.entry("MARK", 0), Map.entry("ROOT", 0),
        Map.entry("WEAKEN", 0),
        // T1
        Map.entry("EXPLOSION", 1), Map.entry("SPLIT", 1), Map.entry("HOMING", 1),
        Map.entry("BOUNCE", 1), Map.entry("QUICKCAST", 1), Map.entry("ARMOR_BREAK", 1),
        Map.entry("BLIGHT", 1), Map.entry("LINGERING", 1), Map.entry("SLOWFIELD", 1),
        // T2
        Map.entry("CHAIN_LIGHTNING", 2), Map.entry("AMPLIFY", 2), Map.entry("LIFESTEAL", 2),
        Map.entry("MULTISHOT", 2), Map.entry("GRAVITY_WELL", 2), Map.entry("ECHO", 2),
        Map.entry("ABSORB", 2), Map.entry("REFLECT", 2), Map.entry("SHATTER", 2),
        Map.entry("SUMMON_WISP", 2),
        // T3
        Map.entry("DEVOUR", 3), Map.entry("CURSE", 3), Map.entry("OVERLOAD", 3),
        Map.entry("UNSTABLE", 3), Map.entry("SUMMON_GUARDIAN", 3)
    );

    // Base tiers
    private static final Map<Integer, Integer> BASE_TIERS = Map.of(
        1, 0,  // basic
        2, 1,  // advanced
        3, 3   // master
    );

    public static int getCarrierTier(SpellCarrier carrier) {
        return CARRIER_TIERS.getOrDefault(carrier.name(), 0);
    }

    public static int getElementTier(SpellElement element) {
        return ELEMENT_TIERS.getOrDefault(element.name(), 0);
    }

    public static int getEffectTier(SpellEffect effect) {
        return EFFECT_TIERS.getOrDefault(effect.name(), 0);
    }

    public static int getBaseTier(int baseTierLevel) {
        return BASE_TIERS.getOrDefault(baseTierLevel, 0);
    }

    /** 计算法术卷轴的最低飞升阶段要求 */
    public static int requiredStage(SpellCarrier carrier, SpellElement element,
                                     SpellEffect effect, int baseTierLevel) {
        int max = getCarrierTier(carrier);
        max = Math.max(max, getElementTier(element));
        if (effect != null) max = Math.max(max, getEffectTier(effect));
        max = Math.max(max, getBaseTier(baseTierLevel));
        return max;
    }
}
