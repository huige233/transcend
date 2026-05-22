package com.huige233.transcend.client.magic;

import net.minecraft.ChatFormatting;

public enum MagicCircleType {

    ARCANE("arcane", "magic_circle", "special.bind_strength",
            0, 1, 1,
            0.5F, 0.3F, 1.0F,
            100, 120, 1.5F,
            ChatFormatting.AQUA, ChatFormatting.LIGHT_PURPLE),

    ELDRITCH("eldritch", "magic_circle_alt", "special.bind_strength",
            0.4F, 0.1F, 0.9F,
            0.8F, 0.2F, 1.0F,
            120, 140, 0.8F,
            ChatFormatting.DARK_PURPLE, ChatFormatting.LIGHT_PURPLE),

    INFERNO("inferno", "magic_circle_inferno", "special.burn_duration",
            1.0F, 0.3F, 0.0F,
            1.0F, 0.8F, 0.0F,
            100, 140, 1.2F,
            ChatFormatting.RED, ChatFormatting.GOLD),

    GLACIAL("glacial", "magic_circle_glacial", "special.freeze_depth",
            0.5F, 0.8F, 1.0F,
            1.0F, 1.0F, 1.0F,
            120, 150, 1.6F,
            ChatFormatting.AQUA, ChatFormatting.WHITE),

    SANCTUM("sanctum", "magic_circle_sanctum", "special.heal_range",
            0.2F, 0.9F, 0.3F,
            1.0F, 0.85F, 0.2F,
            80, 160, 1.8F,
            ChatFormatting.GREEN, ChatFormatting.GOLD),

    GRAVITY("gravity", "magic_circle_gravity", "special.pull_force",
            0.3F, 0.0F, 0.5F,
            0.15F, 0.0F, 0.3F,
            100, 130, 0.5F,
            ChatFormatting.DARK_PURPLE, ChatFormatting.DARK_GRAY),

    THUNDER("thunder", "magic_circle_thunder", "special.chain_range",
            1.0F, 1.0F, 0.3F,
            0.3F, 0.5F, 1.0F,
            80, 120, 1.0F,
            ChatFormatting.YELLOW, ChatFormatting.BLUE),

    TEMPEST("tempest", "magic_circle_tempest", "special.wind_force",
            0.3F, 0.9F, 1.0F,
            1.0F, 1.0F, 1.0F,
            90, 130, 1.4F,
            ChatFormatting.AQUA, ChatFormatting.WHITE),

    TERRA("terra", "magic_circle_terra", "special.armor_level",
            0.55F, 0.35F, 0.1F,
            0.3F, 0.6F, 0.1F,
            140, 160, 0.6F,
            ChatFormatting.DARK_GREEN, ChatFormatting.GOLD),

    VOID("void", "magic_circle_void", "special.wither_level",
            0.15F, 0.0F, 0.2F,
            0.4F, 0.0F, 0.5F,
            100, 150, 0.3F,
            ChatFormatting.DARK_PURPLE, ChatFormatting.DARK_RED),

    CHRONO("chrono", "magic_circle_chrono", "special.time_accel",
            1.0F, 0.85F, 0.2F,
            0.9F, 0.7F, 0.1F,
            100, 140, 1.0F,
            ChatFormatting.GOLD, ChatFormatting.YELLOW),

    BLOOD("blood", "magic_circle_blood", "special.lifesteal",
            0.6F, 0.0F, 0.0F,
            0.8F, 0.1F, 0.1F,
            90, 130, 0.6F,
            ChatFormatting.DARK_RED, ChatFormatting.RED),

    DIVINE("divine", "magic_circle_divine", "special.grace_range",
            1.0F, 0.9F, 0.3F,
            1.0F, 1.0F, 0.8F,
            120, 160, 1.8F,
            ChatFormatting.GOLD, ChatFormatting.WHITE),

    CHAOS("chaos", "magic_circle_chaos", "special.chaos_freq",
            0.8F, 0.2F, 0.8F,
            0.2F, 0.8F, 0.2F,
            80, 110, 1.0F,
            ChatFormatting.LIGHT_PURPLE, ChatFormatting.GREEN),

    PHANTOM("phantom", "magic_circle_phantom", "special.phantom_density",
            0.4F, 0.4F, 0.5F,
            0.2F, 0.2F, 0.3F,
            100, 140, 1.2F,
            ChatFormatting.GRAY, ChatFormatting.DARK_GRAY),

    SKYBOUND("skybound", "magic_circle_skybound", "special.chain_force",
            0.9F, 0.15F, 0.1F,
            0.6F, 0.0F, 0.0F,
            120, 140, 0.7F,
            ChatFormatting.RED, ChatFormatting.DARK_RED);

    public final String id;
    public final String registryName;
    public final String specialNameKey;
    public final float baseR, baseG, baseB;
    public final float accentR, accentG, accentB;
    public final int duration;
    public final int cooldown;
    public final float soundPitch;
    public final ChatFormatting primaryFormat;
    public final ChatFormatting secondaryFormat;

    MagicCircleType(String id, String registryName, String specialNameKey,
                    float baseR, float baseG, float baseB,
                    float accentR, float accentG, float accentB,
                    int duration, int cooldown, float soundPitch,
                    ChatFormatting primaryFormat, ChatFormatting secondaryFormat) {
        this.id = id;
        this.registryName = registryName;
        this.specialNameKey = specialNameKey;
        this.baseR = baseR;
        this.baseG = baseG;
        this.baseB = baseB;
        this.accentR = accentR;
        this.accentG = accentG;
        this.accentB = accentB;
        this.duration = duration;
        this.cooldown = cooldown;
        this.soundPitch = soundPitch;
        this.primaryFormat = primaryFormat;
        this.secondaryFormat = secondaryFormat;
    }

    public String getTooltipKey(String suffix) {
        return "tooltip.transcend." + registryName + "." + suffix;
    }

    public String getSpecialNameKey() {
        return "tooltip.transcend." + specialNameKey;
    }
}
