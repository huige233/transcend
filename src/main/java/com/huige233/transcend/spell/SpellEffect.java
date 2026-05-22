package com.huige233.transcend.spell;

import org.jetbrains.annotations.Nullable;

public enum SpellEffect {
    // === 原有 ===
    EXPLOSION("explosion", 2),
    PIERCING("piercing", 1),
    SPLIT("split", 3),
    HOMING("homing", 2),
    HEALING("healing", 2),
    SHIELD("shield", 1),
    // === 新增 ===
    CHAIN_LIGHTNING("chain_lightning", 3),
    BOUNCE("bounce", 2),
    DELAYED("delayed", 1),
    AMPLIFY("amplify", 3),
    LIFESTEAL("lifesteal", 3),
    QUICKCAST("quickcast", 2),
    MULTISHOT("multishot", 4),
    SLOWFIELD("slowfield", 2),
    GRAVITY_WELL("gravity_well", 3),
    MARK("mark", 1),
    ECHO("echo", 2),
    ARMOR_BREAK("armor_break", 2),
    ROOT("root", 2),
    BLIGHT("blight", 3),
    LINGERING("lingering", 3),
    DEVOUR("devour", 4),
    ABSORB("absorb", 3),
    REFLECT("reflect", 4),
    CURSE("curse", 3),
    OVERLOAD("overload", 4),
    WEAKEN("weaken", 2),
    UNSTABLE("unstable", 2),
    SHATTER("shatter", 3),
    SUMMON_WISP("summon_wisp", 4),
    SUMMON_GUARDIAN("summon_guardian", 5);

    public final String id;
    public final int extraManaCost;

    SpellEffect(String id, int extraManaCost) {
        this.id = id;
        this.extraManaCost = extraManaCost;
    }

    @Nullable
    public static SpellEffect getById(String id) {
        if (id == null || id.isEmpty()) return null;
        for (SpellEffect effect : values()) {
            if (effect.id.equals(id)) return effect;
        }
        return null;
    }

    public String getDisplayKey() {
        return "spell.effect." + id;
    }

    // === Round 11: 数据驱动覆盖访问器 ===
    public int getExtraManaCost() {
        return com.huige233.transcend.spell.data.EffectStatsRegistry.getInstance().get(this).extraManaCost();
    }
}
