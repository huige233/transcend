package com.huige233.transcend.ascension.resource;

import com.huige233.transcend.ascension.MageClass;
import com.huige233.transcend.spell.SpellElement;

public final class ClassResourceMath {
    private ClassResourceMath() {}

    public static float clampFinite(float value, float maximum) {
        if (!Float.isFinite(value) || !Float.isFinite(maximum) || maximum <= 0.0F) return 0.0F;
        return Math.max(0.0F, Math.min(maximum, value));
    }

    public static boolean crossedThreshold(float previous, float current, float threshold) {
        return Float.isFinite(previous) && Float.isFinite(current) && Float.isFinite(threshold)
                && previous < threshold && current >= threshold;
    }

    public static boolean isGroundMovement(double distance, boolean onGround, boolean spectator,
                                           double minimumDistance) {
        return Double.isFinite(distance) && onGround && !spectator
                && distance > minimumDistance && distance <= 1.5D;
    }

    public static boolean isCanonicalClassMatch(MageClass mageClass, SpellElement element) {
        return mageClass != null && element != null
                && mageClass.getCanonicalMastery().element == element.canonical();
    }

    public static float spellDamageMultiplier(MageClass mageClass, SpellElement element, float resourceValue) {
        if (mageClass == null || element == null) return 1.0F;
        SpellElement canonical = element.canonical();
        if (mageClass == MageClass.PYROMANCER && canonical == SpellElement.FIRE) {
            return 1.0F + ratio(resourceValue, ClassResourceType.HEAT.getMaxValue()) * 0.6F;
        }
        if (mageClass == MageClass.ABYSSWALKER && canonical == SpellElement.CHAOS) {
            return 2.0F - ratio(resourceValue, ClassResourceType.HUNGER.getMaxValue());
        }
        return 1.0F;
    }

    private static float ratio(float value, float maximum) {
        return clampFinite(value, maximum) / maximum;
    }
}
