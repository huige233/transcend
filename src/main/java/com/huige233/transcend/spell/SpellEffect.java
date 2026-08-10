package com.huige233.transcend.spell;

import org.jetbrains.annotations.Nullable;

/** 法术效果枚举。 */
public enum SpellEffect {
    AMPLIFY("amplify", 3, true),
    PIERCING("piercing", 1, true),
    SPLIT("split", 3, true),
    HOMING("homing", 2),
    HEALING("healing", 2),
    SHIELD("shield", 1),
    EXPLOSION("explosion", 2, true),
    LIFESTEAL("lifesteal", 3),
    CHAIN_LIGHTNING("chain_lightning", 3),
    MULTISHOT("multishot", 4, true),
    SLOWFIELD("slowfield", 2),
    MARK("mark", 1),
    ROOT("root", 2),
    BLIGHT("blight", 3),
    CURSE("curse", 3),
    OVERLOAD("overload", 4),
    SHATTER("shatter", 3);

    public final String id;
    public final int extraManaCost;
    private final boolean repeatable;

    SpellEffect(String id, int extraManaCost) {
        this(id, extraManaCost, false);
    }

    SpellEffect(String id, int extraManaCost, boolean repeatable) {
        this.id = id;
        this.extraManaCost = extraManaCost;
        this.repeatable = repeatable;
    }

    @Nullable
    public static SpellEffect getById(String id) {
        if (id == null || id.isEmpty()) return null;
        id = switch (id) {
            case "bounce" -> "split";
            case "delayed" -> "explosion";
            case "quickcast" -> "amplify";
            case "gravity_well" -> "root";
            case "echo" -> "multishot";
            case "armor_break" -> "piercing";
            case "lingering", "weaken" -> "blight";
            case "devour" -> "lifesteal";
            case "absorb", "reflect" -> "shield";
            case "unstable" -> "overload";
            case "summon_wisp" -> "homing";
            case "summon_guardian" -> "shield";
            default -> id;
        };
        for (SpellEffect effect : values()) {
            if (effect.id.equals(id)) return effect;
        }
        return null;
    }

    public String getDisplayKey() {
        return "spell.effect." + id;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    public static SpellEffect[] configurableValues() {
        return values();
    }

    public int getExtraManaCost() {
        return com.huige233.transcend.spell.data.EffectStatsRegistry.getInstance().get(this).extraManaCost();
    }
}
