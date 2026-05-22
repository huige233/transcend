package com.huige233.transcend.entity.boss;

public enum BossFaction {
    LIGHT,
    VOID,
    TRANSCEND;

    public boolean isHostileTo(BossFaction other) {
        return this != other;
    }
}
