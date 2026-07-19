package com.huige233.transcend.gear.forge;

import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

public enum AspectKind {
    FIRE  ("fire",   ChatFormatting.RED),
    WATER ("water",  ChatFormatting.AQUA),
    EARTH ("earth",  ChatFormatting.GOLD),
    WIND  ("wind",   ChatFormatting.GREEN),
    SPIRIT("spirit", ChatFormatting.YELLOW),
    VOID  ("void",   ChatFormatting.DARK_PURPLE);

    public final String id;
    public final ChatFormatting color;

    AspectKind(String id, ChatFormatting color) {
        this.id = id;
        this.color = color;
    }

    public String catalystItemId() {
        return "catalyst_" + id;
    }

    public String langKey() {
        return "catalyst.transcend." + id + ".name";
    }

    @Nullable
    public static AspectKind byId(String id) {
        for (AspectKind k : values()) if (k.id.equals(id)) return k;
        return null;
    }
}
