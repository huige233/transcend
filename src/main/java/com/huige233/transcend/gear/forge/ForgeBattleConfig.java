package com.huige233.transcend.gear.forge;

public final class ForgeBattleConfig {

    private ForgeBattleConfig() {}

    public static final float SHARPNESS_PER_SOCKET = 0.05f;
    public static final float WARD_PER_SOCKET      = 0.03f;
    public static final float SPARK_CRIT_PER_SOCKET = 0.02f;
    public static final float SPARK_CRIT_BONUS_DAMAGE = 0.50f;
    public static final float SWIFTNESS_COOLDOWN_PER_SOCKET = 0.04f;
    public static final float LEECH_HEAL_PER_SOCKET = 0.02f;
    public static final float FOCUS_MANA_PER_SOCKET = 0.04f;

    public static final float SOUL_ECHO_DAMAGE_BONUS = 0.25f;

    public static final float[] TIER_MULT = { 0.00f, 0.05f, 0.12f, 0.25f };

    public static final float BLESSING_PURE_BONUS = 0.30f;
    public static final float BLESSING_DUAL_BONUS = 0.15f;
    public static final float BLESSING_INDETERMINATE_BONUS = 0.0f;

    public static final float SOLAR_DAY_BONUS = 0.20f;
    public static final float LUNAR_NIGHT_BONUS = 0.20f;
}
