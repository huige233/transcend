package com.huige233.transcend.tech.shield;


/** 表示覆盖所有伤害或仅覆盖指定伤害类别的护盾模式，并判断来袭类别是否受保护。 */
public record ShieldDefenseMode(boolean omnidirectional, int categoryIndex, int categoryMask) {
    public static final int OMNIDIRECTIONAL = -1;

    public ShieldDefenseMode {
        if (omnidirectional) categoryIndex = OMNIDIRECTIONAL;
        else if (categoryIndex < 0) throw new IllegalArgumentException("Directional shields need a category index");
    }

    public ShieldDefenseMode(boolean omnidirectional, int categoryIndex) {
        this(omnidirectional, categoryIndex, 0);
    }

    /** 低阶基础场覆盖近战、投射物、爆炸和火焰；额外类型由特化模块覆盖。 */
    public static ShieldDefenseMode conventional() {
        return new ShieldDefenseMode(false, 0, 0b1111);
    }

    public static ShieldDefenseMode omni() {
        return new ShieldDefenseMode(true, OMNIDIRECTIONAL);
    }

    public static ShieldDefenseMode directional(int categoryIndex) {
        return new ShieldDefenseMode(false, categoryIndex);
    }

    public boolean protects(int incomingCategory) {
        return omnidirectional || (categoryMask == 0 ? categoryIndex == incomingCategory
                : incomingCategory >= 0 && incomingCategory < 31 && (categoryMask & (1 << incomingCategory)) != 0);
    }
}
