package com.huige233.transcend.tech.shield;


/** 根据护盾是否存在、启用和有能量判定显示状态，并提供翻译键与网络值解析。 */
public enum PhaseShieldStatus {
    ABSENT("inactive"), INACTIVE("inactive"), ACTIVE("active"), EMPTY("empty");

    private final String key;

    PhaseShieldStatus(String key) { this.key = key; }

    public String translationKey() { return "phase_shield.transcend.status." + key; }

    public static PhaseShieldStatus evaluate(boolean present, boolean enabled, boolean powered) {
        if (!present) return ABSENT;
        if (!enabled) return INACTIVE;
        return powered ? ACTIVE : EMPTY;
    }

    public static PhaseShieldStatus fromNetwork(int value) {
        return value >= 0 && value < values().length ? values()[value] : ABSENT;
    }
}
