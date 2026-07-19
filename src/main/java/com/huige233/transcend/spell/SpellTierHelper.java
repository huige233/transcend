package com.huige233.transcend.spell;

import java.util.EnumMap;
import java.util.Map;

public final class SpellTierHelper {
    private static final Map<SpellCarrier, Integer> CARRIER_TIERS = new EnumMap<>(SpellCarrier.class);
    private static final Map<SpellEffect, Integer> EFFECT_TIERS = new EnumMap<>(SpellEffect.class);

    static {
        putCarriers(1, SpellCarrier.ORB, SpellCarrier.ARROW, SpellCarrier.SLASH);
        putCarriers(2, SpellCarrier.DASH, SpellCarrier.BEAM, SpellCarrier.NOVA,
                SpellCarrier.TRAP, SpellCarrier.BARRIER);
        putCarriers(3, SpellCarrier.CHAIN, SpellCarrier.VORTEX, SpellCarrier.RAIN);

        putEffects(1, SpellEffect.PIERCING, SpellEffect.HEALING, SpellEffect.SHIELD,
                SpellEffect.MARK, SpellEffect.ROOT);
        putEffects(2, SpellEffect.EXPLOSION, SpellEffect.SPLIT, SpellEffect.HOMING,
                SpellEffect.BLIGHT, SpellEffect.SLOWFIELD);
        putEffects(3, SpellEffect.CHAIN_LIGHTNING, SpellEffect.AMPLIFY, SpellEffect.LIFESTEAL,
                SpellEffect.MULTISHOT, SpellEffect.SHATTER);
        putEffects(4, SpellEffect.CURSE, SpellEffect.OVERLOAD);
    }

    private SpellTierHelper() {}

    public static int getCarrierTier(SpellCarrier carrier) {
        return CARRIER_TIERS.getOrDefault(carrier, 1);
    }

    public static int getElementTier(SpellElement element) {
        return element.canonical() == SpellElement.CHAOS ? 6 : 1;
    }

    public static int getEffectTier(SpellEffect effect) {
        return effect == null ? 1 : EFFECT_TIERS.getOrDefault(effect, 1);
    }

    public static int getBaseTier(int baseTierLevel) {
        return Math.max(1, Math.min(12, baseTierLevel));
    }

    public static int requiredTier(SpellCarrier carrier, SpellElement element,
                                   SpellEffect effect, int baseTierLevel) {
        int tier = Math.max(getCarrierTier(carrier), getElementTier(element));
        tier = Math.max(tier, getEffectTier(effect));
        return Math.max(tier, getBaseTier(baseTierLevel));
    }

    @Deprecated
    public static int requiredStage(SpellCarrier carrier, SpellElement element,
                                    SpellEffect effect, int baseTierLevel) {
        return requiredTier(carrier, element, effect, baseTierLevel);
    }

    private static void putCarriers(int tier, SpellCarrier... carriers) {
        for (SpellCarrier carrier : carriers) CARRIER_TIERS.put(carrier, tier);
    }

    private static void putEffects(int tier, SpellEffect... effects) {
        for (SpellEffect effect : effects) EFFECT_TIERS.put(effect, tier);
    }
}
