package com.huige233.transcend.gear;

import net.minecraft.ChatFormatting;

public enum ForgeStage {

    CRUCIBLE("crucible", ChatFormatting.GOLD,         "✦"),

    RESONANCE("resonance", ChatFormatting.AQUA,       "◇"),

    SOUL("soul", ChatFormatting.RED,                  "◇"),

    EXPERIENCE("experience", ChatFormatting.GREEN,    "◇"),

    CELESTIAL("celestial", ChatFormatting.LIGHT_PURPLE, "✦");

    public final String id;
    public final ChatFormatting color;
    public final String marker;

    ForgeStage(String id, ChatFormatting color, String marker) {
        this.id = id;
        this.color = color;
        this.marker = marker;
    }

    public String getNameKey() {
        return "gear.transcend.forge.stage." + id;
    }
}
