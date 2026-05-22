package com.huige233.transcend.ascension;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * 六大法师职业分支
 * 玩家在飞升时选择一个职业，解锁对应的天赋树
 */
public enum MageClass {

    NONE("none", "mage_class.transcend.none",
            ChatFormatting.GRAY, 0x888888, "none"),

    PYROMANCER("pyromancer", "mage_class.transcend.pyromancer",
            ChatFormatting.RED, 0xFF4400, "fire"),

    CRYOMANCER("cryomancer", "mage_class.transcend.cryomancer",
            ChatFormatting.AQUA, 0x44CCFF, "ice"),

    STORMCALLER("stormcaller", "mage_class.transcend.stormcaller",
            ChatFormatting.YELLOW, 0xFFEE00, "thunder"),

    ARCANIST("arcanist", "mage_class.transcend.arcanist",
            ChatFormatting.GOLD, 0xFFAA00, "holy"),

    ABYSSWALKER("abysswalker", "mage_class.transcend.abysswalker",
            ChatFormatting.DARK_PURPLE, 0x660099, "void"),

    EARTHSHAPER("earthshaper", "mage_class.transcend.earthshaper",
            ChatFormatting.GREEN, 0x228822, "earth"),

    OMNIMANCER("omnimancer", "mage_class.transcend.omnimancer",
            ChatFormatting.WHITE, 0xFFFFFF, "omni");

    public final String id;
    public final String translationKey;
    public final ChatFormatting color;
    public final int hexColor;
    public final String primaryElement;

    MageClass(String id, String translationKey, ChatFormatting color, int hexColor, String primaryElement) {
        this.id = id;
        this.translationKey = translationKey;
        this.color = color;
        this.hexColor = hexColor;
        this.primaryElement = primaryElement;
    }

    public Component getDisplayName() {
        return Component.translatable(translationKey).withStyle(color);
    }

    public Component getDescription() {
        return Component.translatable(translationKey + ".desc").withStyle(ChatFormatting.GRAY);
    }

    public static MageClass getById(String id) {
        for (MageClass c : values()) {
            if (c.id.equals(id)) return c;
        }
        return NONE;
    }

    public boolean isSelected() {
        return this != NONE;
    }
}
