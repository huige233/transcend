package com.huige233.transcend.gear.forge;

import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

public enum ResonanceKind {
    SHARPNESS("sharpness", ChatFormatting.RED),
    SWIFTNESS("swiftness", ChatFormatting.YELLOW),
    LEECH    ("leech",     ChatFormatting.DARK_RED),
    WARD     ("ward",      ChatFormatting.AQUA),
    FOCUS    ("focus",     ChatFormatting.BLUE),
    SPARK    ("spark",     ChatFormatting.LIGHT_PURPLE);

    public final String id;
    public final ChatFormatting color;

    ResonanceKind(String id, ChatFormatting color) {
        this.id = id;
        this.color = color;
    }

    public String crystalItemId() { return "resonance_crystal_" + id; }
    public String langKey()       { return "resonance.transcend." + id + ".name"; }
    public String descKey()       { return "resonance.transcend." + id + ".desc"; }

    @Nullable
    public static ResonanceKind byId(String id) {
        for (ResonanceKind k : values()) if (k.id.equals(id)) return k;
        return null;
    }
}
