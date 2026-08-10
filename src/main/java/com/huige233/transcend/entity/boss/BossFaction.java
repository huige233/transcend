package com.huige233.transcend.entity.boss;

/** 首领阵营枚举。 */
public enum BossFaction {
    LIGHT,
    VOID,
    TRANSCEND;

    public boolean isHostileTo(BossFaction other) {
        return this != other;
    }
}
