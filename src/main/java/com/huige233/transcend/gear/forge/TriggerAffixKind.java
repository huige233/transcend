package com.huige233.transcend.gear.forge;

import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

public enum TriggerAffixKind {

    EMBER       ("ember",        Category.ON_KILL,  ChatFormatting.RED),

    REPRISE     ("reprise",      Category.ON_KILL,  ChatFormatting.GREEN),

    HARMONIC    ("harmonic",     Category.ON_KILL,  ChatFormatting.YELLOW),

    SANGUINE    ("sanguine",     Category.ON_KILL,  ChatFormatting.DARK_RED),

    SOUL_REAP   ("soul_reap",    Category.ON_KILL,  ChatFormatting.LIGHT_PURPLE),

    THORNBACK   ("thornback",    Category.ON_HURT,  ChatFormatting.DARK_GREEN),

    LAST_DASH   ("last_dash",    Category.ON_HURT,  ChatFormatting.AQUA),

    AEGIS_HEAL  ("aegis_heal",   Category.ON_HURT,  ChatFormatting.GOLD),

    DEATH_ECHO  ("death_echo",   Category.ON_HURT,  ChatFormatting.DARK_PURPLE),

    PULSE       ("pulse",        Category.PERIODIC, ChatFormatting.BLUE),

    AEGIS_AURA  ("aegis_aura",   Category.PERIODIC, ChatFormatting.WHITE),

    OVERFLOW    ("overflow",     Category.PERIODIC, ChatFormatting.LIGHT_PURPLE);

    public enum Category {
        ON_KILL, ON_HURT, PERIODIC
    }

    public final String id;
    public final Category category;
    public final ChatFormatting color;

    TriggerAffixKind(String id, Category category, ChatFormatting color) {
        this.id = id;
        this.category = category;
        this.color = color;
    }

    public String itemId()  { return "trigger_inscription_" + id; }
    public String nameKey() { return "trigger_affix.transcend." + id + ".name"; }
    public String descKey() { return "trigger_affix.transcend." + id + ".desc"; }

    @Nullable
    public static TriggerAffixKind byId(String id) {
        if (id == null) return null;
        for (TriggerAffixKind k : values()) if (k.id.equals(id)) return k;
        return null;
    }
}
