package com.huige233.transcend.gear.forge;

import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

public enum CelestialKind {
    SUN  ("sun",   ChatFormatting.GOLD),
    MOON ("moon",  ChatFormatting.AQUA),
    STAR ("star",  ChatFormatting.YELLOW),
    ABYSS("abyss", ChatFormatting.DARK_PURPLE);

    public final String id;
    public final ChatFormatting color;

    CelestialKind(String id, ChatFormatting color) {
        this.id = id;
        this.color = color;
    }

    public String fragmentItemId() { return "celestial_fragment_" + id; }
    public String langKey()        { return "celestial.transcend." + id + ".name"; }

    @Nullable
    public static CelestialKind byId(String id) {
        for (CelestialKind k : values()) if (k.id.equals(id)) return k;
        return null;
    }
}
