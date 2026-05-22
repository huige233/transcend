package com.huige233.transcend.spell;

import net.minecraft.ChatFormatting;

public enum WandRune {
    MANA_SIPHON("mana_siphon", "rune.transcend.mana_siphon", ChatFormatting.AQUA),
    RAPID_FIRE("rapid_fire", "rune.transcend.rapid_fire", ChatFormatting.YELLOW),
    OVERCHARGE("overcharge", "rune.transcend.overcharge", ChatFormatting.RED),
    SPELL_ECHO("spell_echo", "rune.transcend.spell_echo", ChatFormatting.LIGHT_PURPLE),
    ELEMENTAL_MASTERY("elemental_mastery", "rune.transcend.elemental_mastery", ChatFormatting.GOLD),
    GLASS_CANNON("glass_cannon", "rune.transcend.glass_cannon", ChatFormatting.DARK_RED),
    CONSERVATION("conservation", "rune.transcend.conservation", ChatFormatting.GREEN),
    CHAIN_CASTER("chain_caster", "rune.transcend.chain_caster", ChatFormatting.BLUE);

    public final String id;
    public final String displayKey;
    public final ChatFormatting color;

    WandRune(String id, String displayKey, ChatFormatting color) {
        this.id = id;
        this.displayKey = displayKey;
        this.color = color;
    }

    public static WandRune getById(String id) {
        if (id == null || id.isEmpty()) return null;
        for (WandRune rune : values()) {
            if (rune.id.equals(id)) return rune;
        }
        return null;
    }

    public String getDescKey() {
        return displayKey + ".desc";
    }
}
