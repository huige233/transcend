package com.huige233.transcend.ascension;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum MageClass {

    NONE("none", "mage_class.transcend.none",
            ChatFormatting.GRAY, 0x888888, "none", false),

    PYROMANCER("pyromancer", "mage_class.transcend.pyromancer",
            ChatFormatting.RED, 0xFF4400, "fire", false),

    CRYOMANCER("cryomancer", "mage_class.transcend.cryomancer",
            ChatFormatting.AQUA, 0x44CCFF, "water", false),

    STORMCALLER("stormcaller", "mage_class.transcend.stormcaller",
            ChatFormatting.YELLOW, 0xFFEE00, "fire", false),

    ABYSSWALKER("abysswalker", "mage_class.transcend.abysswalker",
            ChatFormatting.DARK_PURPLE, 0x660099, "chaos", false),

    EARTHSHAPER("earthshaper", "mage_class.transcend.earthshaper",
            ChatFormatting.GREEN, 0x228822, "earth", false),

    CHRONOWEAVER("chronoweaver", "mage_class.transcend.chronoweaver",
            ChatFormatting.LIGHT_PURPLE, 0xCC88FF, "chaos", false),

    OMNISCIENT("omniscient", "mage_class.transcend.omniscient",
            ChatFormatting.WHITE, 0xFFFFFF, "chaos", true);

    public final String id;
    public final String translationKey;
    public final ChatFormatting color;
    public final int hexColor;
    public final String primaryElement;
    public final boolean hidden;

    MageClass(String id, String translationKey, ChatFormatting color, int hexColor,
              String primaryElement, boolean hidden) {
        this.id = id;
        this.translationKey = translationKey;
        this.color = color;
        this.hexColor = hexColor;
        this.primaryElement = primaryElement;
        this.hidden = hidden;
    }

    public Component getDisplayName() {
        return Component.translatable(translationKey).withStyle(color);
    }

    public Component getDescription() {
        return Component.translatable(translationKey + ".desc").withStyle(ChatFormatting.GRAY);
    }

    public static MageClass getById(String id) {
        if ("arcanist".equals(id)) return CHRONOWEAVER;
        if ("omnimancer".equals(id)) return OMNISCIENT;
        for (MageClass c : values()) {
            if (c.id.equals(id)) return c;
        }
        return NONE;
    }

    public boolean isSelected() {
        return this != NONE;
    }

    public ElementMastery getCanonicalMastery() {
        return switch (this) {
            case PYROMANCER, STORMCALLER -> ElementMastery.FIRE;
            case CRYOMANCER -> ElementMastery.WATER;
            case EARTHSHAPER -> ElementMastery.EARTH;
            case ABYSSWALKER, CHRONOWEAVER, OMNISCIENT -> ElementMastery.CHAOS;
            case NONE -> ElementMastery.NONE;
        };
    }
}
