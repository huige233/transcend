package com.huige233.transcend.lib;

/**
 * 通用数学工具类。
 * <p>
 * 参考 ending_library 的 MathUtils 思路，保留常用且高频的方法，
 * 用于动画插值、范围映射、逼近计算等。
 */
public final class MathToolkit {

    private MathToolkit() {
    }

    /**
     * 线性插值（float）。
     */
    public static float lerp(float from, float to, float alpha) {
        return from + (to - from) * alpha;
    }

    /**
     * 线性插值（double）。
     */
    public static double lerp(double from, double to, double alpha) {
        return from + (to - from) * alpha;
    }

    /**
     * 线性逼近：每次最多移动 maxStep。
     */
    public static float approachLinear(float from, float to, float maxStep) {
        if (from > to) {
            return from - to < maxStep ? to : from - maxStep;
        }
        return to - from < maxStep ? to : from + maxStep;
    }

    /**
     * 线性逼近：每次最多移动 maxStep。
     */
    public static double approachLinear(double from, double to, double maxStep) {
        if (from > to) {
            return from - to < maxStep ? to : from - maxStep;
        }
        return to - from < maxStep ? to : from + maxStep;
    }

    /**
     * 指数逼近（每次按比例靠近目标）。
     */
    public static double approachExp(double from, double to, double ratio) {
        return from + (to - from) * ratio;
    }

    /**
     * 指数逼近并限制最大步长。
     */
    public static double approachExp(double from, double to, double ratio, double cap) {
        double delta = (to - from) * ratio;
        if (Math.abs(delta) > cap) {
            delta = Math.signum(delta) * cap;
        }
        return from + delta;
    }

    /**
     * 数值裁剪到区间 [min, max]。
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 数值裁剪到区间 [min, max]。
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 数值裁剪到区间 [min, max]。
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 线性映射：将输入区间映射到输出区间。
     */
    public static double map(double valueIn, double inMin, double inMax, double outMin, double outMax) {
        if (Math.abs(inMax - inMin) < 1.0e-9) {
            return outMin;
        }
        return (valueIn - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }

    /**
     * 线性映射：将输入区间映射到输出区间。
     */
    public static float map(float valueIn, float inMin, float inMax, float outMin, float outMax) {
        if (Math.abs(inMax - inMin) < 1.0e-6f) {
            return outMin;
        }
        return (valueIn - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }

    /**
     * 判断 value 是否位于 [min, max] 区间。
     */
    public static boolean between(double min, double value, double max) {
        return value >= min && value <= max;
    }
}
