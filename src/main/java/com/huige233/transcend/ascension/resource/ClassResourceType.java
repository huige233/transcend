package com.huige233.transcend.ascension.resource;

import com.huige233.transcend.ascension.MageClass;
import net.minecraft.ChatFormatting;

/** 职业资源类型枚举。 */
public enum ClassResourceType {

    HEAT("heat", MageClass.PYROMANCER, 100, 80,
            0xFF4400, 0xFFEE00, ChatFormatting.RED,
            DecayBehavior.SLOW_DECAY, 0.5f),

    FROST_ARMOR("frost_armor", MageClass.CRYOMANCER, 10, 8,
            0x44CCFF, 0xAAEEFF, ChatFormatting.AQUA,
            DecayBehavior.NO_DECAY, 0f),

    CHARGE("charge", MageClass.STORMCALLER, 100, 70,
            0xFFEE00, 0xFFFFAA, ChatFormatting.YELLOW,
            DecayBehavior.IDLE_DECAY, 1.0f),

    HUNGER("hunger", MageClass.ABYSSWALKER, 100, 70,
            0x660099, 0xFF00FF, ChatFormatting.DARK_PURPLE,
            DecayBehavior.PASSIVE_RISE, 0.8f),

    SEDIMENT("sediment", MageClass.EARTHSHAPER, 8, 6,
            0x228822, 0x88CC44, ChatFormatting.GREEN,
            DecayBehavior.MOVE_DECAY, 1.0f),

    TEMPO("tempo", MageClass.CHRONOWEAVER, 100, 60,
            0xCC88FF, 0xFFCCFF, ChatFormatting.LIGHT_PURPLE,
            DecayBehavior.SLOW_DECAY, 0.3f);

    public final String id;
    public final MageClass ownerClass;

    public final int maxValue;

    public final int criticalThreshold;

    public final int primaryColor;

    public final int criticalColor;

    public final ChatFormatting textColor;

    public final DecayBehavior decayBehavior;

    public final float decayRate;

    ClassResourceType(String id, MageClass ownerClass, int maxValue, int criticalThreshold,
                      int primaryColor, int criticalColor, ChatFormatting textColor,
                      DecayBehavior decayBehavior, float decayRate) {
        this.id = id;
        this.ownerClass = ownerClass;
        this.maxValue = maxValue;
        this.criticalThreshold = criticalThreshold;
        this.primaryColor = primaryColor;
        this.criticalColor = criticalColor;
        this.textColor = textColor;
        this.decayBehavior = decayBehavior;
        this.decayRate = decayRate;
    }

    public float getMaxValue() {
        return (float) maxValue;
    }

    public float getThreshold() {
        return (float) criticalThreshold;
    }

    public boolean isInverted() {
        return this == HUNGER;
    }

    public boolean isLayered() {
        return this == FROST_ARMOR || this == SEDIMENT;
    }

    public static ClassResourceType forClass(MageClass mageClass) {
        if (mageClass == null) return null;
        for (ClassResourceType type : values()) {
            if (type.ownerClass == mageClass) return type;
        }
        return null;
    }

    public static ClassResourceType getById(String id) {
        if (id == null || id.isEmpty()) return null;
        for (ClassResourceType type : values()) {
            if (type.id.equals(id)) return type;
        }
        return null;
    }

    public int getBarColor() {
        return 0xFF000000 | primaryColor;
    }

    public float getThresholdRatio() {
        return maxValue > 0 ? (float) criticalThreshold / maxValue : 0f;
    }

    public String getIcon() {
        return switch (this) {
            case HEAT        -> "🔥";
            case FROST_ARMOR -> "❄";
            case CHARGE      -> "⚡";
            case HUNGER      -> "🩸";
            case SEDIMENT    -> "�ite";
            case TEMPO       -> "⏳";
        };
    }

    public String getDisplayId() {
        return switch (this) {
            case HEAT        -> "Heat";
            case FROST_ARMOR -> "Frost";
            case CHARGE      -> "Charge";
            case HUNGER      -> "Hunger";
            case SEDIMENT    -> "Sediment";
            case TEMPO       -> "Tempo";
        };
    }

    public enum DecayBehavior {

        NO_DECAY,

        SLOW_DECAY,

        IDLE_DECAY,

        PASSIVE_RISE,

        MOVE_DECAY
    }
}
