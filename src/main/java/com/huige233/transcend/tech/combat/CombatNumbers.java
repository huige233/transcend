package com.huige233.transcend.tech.combat;


/** 将战斗伤害、穿透强度、弹体数量和散布角规范为有限且边界明确的数值。 */
public final class CombatNumbers {
    private CombatNumbers() {}

    public static float nonNegative(double value) {
        return Double.isFinite(value) ? (float) Math.max(0.0D, Math.min(Float.MAX_VALUE, value)) : 0.0F;
    }

    public static int strength(double value) {
        return Double.isFinite(value) ? (int) Math.max(0.0D, Math.min(Integer.MAX_VALUE, Math.floor(value))) : 0;
    }

    public static int shotCount(double attribute, int moduleCount) {
        double count = Double.isFinite(attribute) ? Math.floor(attribute + 0.5D) : 1.0D;
        return (int) Math.max(1.0D, Math.min(16.0D, Math.max(1.0D, count) + Math.max(0L, (long) moduleCount - 1L)));
    }

    public static double spread(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, Math.min(180.0D, value)) : 0.0D;
    }
}
