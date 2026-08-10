package com.huige233.transcend.ascension;

import com.huige233.transcend.spell.SpellElement;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** 元素掌控等级枚举。 */
public enum ElementMastery {

    NONE("none", null, ChatFormatting.GRAY,
            0f, 0f,
            "mastery.transcend.none", "mastery.transcend.none.desc"),

    OMNI("omni", null, ChatFormatting.WHITE,
            0.40f, 0.40f,
            "mastery.transcend.omni", "mastery.transcend.omni.desc"),

    METAL("metal", SpellElement.METAL, ChatFormatting.GOLD,
             0.75f, -0.30f,
            "mastery.transcend.metal", "mastery.transcend.metal.desc"),
    WOOD("wood", SpellElement.WOOD, ChatFormatting.GREEN,
             0.75f, -0.30f,
            "mastery.transcend.wood", "mastery.transcend.wood.desc"),
    WATER("water", SpellElement.WATER, ChatFormatting.AQUA,
             0.75f, -0.30f,
            "mastery.transcend.water", "mastery.transcend.water.desc"),
    FIRE("fire", SpellElement.FIRE, ChatFormatting.RED,
             0.75f, -0.30f,
            "mastery.transcend.fire", "mastery.transcend.fire.desc"),
    EARTH("earth", SpellElement.EARTH, ChatFormatting.DARK_GREEN,
             0.75f, -0.30f,
            "mastery.transcend.earth", "mastery.transcend.earth.desc"),
    CHAOS("chaos", SpellElement.CHAOS, ChatFormatting.LIGHT_PURPLE,
            0.75f, -0.30f,
            "mastery.transcend.chaos",  "mastery.transcend.chaos.desc");

    public final String id;

    public final SpellElement element;
    public final ChatFormatting color;

    public final float masteredBonus;

    public final float otherBonus;
    public final String nameKey;
    public final String descKey;

    ElementMastery(String id, SpellElement element, ChatFormatting color,
                   float masteredBonus, float otherBonus,
                   String nameKey, String descKey) {
        this.id = id;
        this.element = element;
        this.color = color;
        this.masteredBonus = masteredBonus;
        this.otherBonus = otherBonus;
        this.nameKey = nameKey;
        this.descKey = descKey;
    }

    public boolean isSelected() { return this != NONE; }
    public boolean isOmni()     { return this == OMNI; }
    public boolean isSpecific() { return this != NONE && this != OMNI; }

    public float getDamageBonus(SpellElement castElement) {
        if (this == NONE) return 0f;
        if (this == OMNI) return masteredBonus;
        if (castElement == this.element) return masteredBonus;
        return otherBonus;
    }

    public float getManaCostReduction(SpellElement castElement) {
        if (this == NONE) return 0f;
        if (this == OMNI) return 0.10f;
        if (castElement == this.element) return 0.25f;
        return 0f;
    }

    public Component getDisplayName() {
        return Component.translatable(nameKey).withStyle(color);
    }

    public Component getDescription() {
        return Component.translatable(descKey).withStyle(ChatFormatting.GRAY);
    }

    public static ElementMastery getById(String id) {
        if (id == null || id.isEmpty()) return NONE;
        return switch (id) {
            case "none" -> NONE;
            case "omni" -> OMNI;
            case "metal", "holy", "arcane", "light" -> METAL;
            case "wood", "wind", "nature", "blood" -> WOOD;
            case "water", "ice" -> WATER;
            case "fire", "thunder", "lightning" -> FIRE;
            case "earth", "poison", "dark" -> EARTH;
            case "chaos", "void", "time", "space" -> CHAOS;
            default -> NONE;
        };
    }

    public static ElementMastery fromElement(SpellElement el) {
        for (ElementMastery m : values()) {
            if (m.element == el) return m;
        }
        return NONE;
    }
}
