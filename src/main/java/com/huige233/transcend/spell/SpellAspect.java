package com.huige233.transcend.spell;

public enum SpellAspect {
    BLAZE("blaze"),
    FROST("frost"),
    WITHER("wither"),
    VERDANT("verdant"),
    CHAOS("chaos");

    public final String id;

    SpellAspect(String id) {
        this.id = id;
    }

    public String getDisplayKey() {
        return "spell.aspect." + id;
    }

    public boolean isOpposite(SpellAspect other) {
        return (this == BLAZE && other == FROST) || (this == FROST && other == BLAZE)
            || (this == WITHER && other == VERDANT) || (this == VERDANT && other == WITHER);
    }

    public static SpellAspect byName(String name) {
        if (name == null || name.isEmpty()) return null;
        for (SpellAspect a : values()) {
            if (a.name().equalsIgnoreCase(name) || a.id.equalsIgnoreCase(name)) return a;
        }
        return null;
    }
}
