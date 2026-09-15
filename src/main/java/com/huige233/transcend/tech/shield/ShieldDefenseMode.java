package com.huige233.transcend.tech.shield;


/** 表示覆盖所有伤害或仅覆盖指定伤害类别的护盾模式，并判断来袭类别是否受保护。 */
public record ShieldDefenseMode(boolean omnidirectional, int categoryIndex) {
    public static final int OMNIDIRECTIONAL = -1;

    public ShieldDefenseMode {
        if (omnidirectional) categoryIndex = OMNIDIRECTIONAL;
        else if (categoryIndex < 0) throw new IllegalArgumentException("Directional shields need a category index");
    }

    public static ShieldDefenseMode omni() {
        return new ShieldDefenseMode(true, OMNIDIRECTIONAL);
    }

    public static ShieldDefenseMode directional(int categoryIndex) {
        return new ShieldDefenseMode(false, categoryIndex);
    }

    public boolean protects(int incomingCategory) {
        return omnidirectional || categoryIndex == incomingCategory;
    }
}
