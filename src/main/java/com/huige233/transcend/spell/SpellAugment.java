package com.huige233.transcend.spell;

public enum SpellAugment {

    AMPLIFY("amplify", 4),
    DAMPEN("dampen", 1),
    QUICKFIRE("quickfire", 1),
    SPLIT("split", 1),

    PIERCE("pierce", 3),
    CHAIN("chain", 1),
    EXTEND("extend", 3),
    HOMING("homing", 1);

    public final String id;
    public final int maxStack;

    SpellAugment(String id, int maxStack) {
        this.id = id;
        this.maxStack = maxStack;
    }

    public static SpellAugment byOrdinal(int ord) {
        SpellAugment[] all = values();
        if (ord < 0 || ord >= all.length) return AMPLIFY;
        return all[ord];
    }
}
