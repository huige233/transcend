package com.huige233.transcend.lib;

public final class MathToolkit {

    private MathToolkit() {
    }

    public static float lerp(float from, float to, float alpha) {
        return from + (to - from) * alpha;
    }

    public static double lerp(double from, double to, double alpha) {
        return from + (to - from) * alpha;
    }

    public static float approachLinear(float from, float to, float maxStep) {
        if (from > to) {
            return from - to < maxStep ? to : from - maxStep;
        }
        return to - from < maxStep ? to : from + maxStep;
    }

    public static double approachLinear(double from, double to, double maxStep) {
        if (from > to) {
            return from - to < maxStep ? to : from - maxStep;
        }
        return to - from < maxStep ? to : from + maxStep;
    }

    public static double approachExp(double from, double to, double ratio) {
        return from + (to - from) * ratio;
    }

    public static double approachExp(double from, double to, double ratio, double cap) {
        double delta = (to - from) * ratio;
        if (Math.abs(delta) > cap) {
            delta = Math.signum(delta) * cap;
        }
        return from + delta;
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double map(double valueIn, double inMin, double inMax, double outMin, double outMax) {
        if (Math.abs(inMax - inMin) < 1.0e-9) {
            return outMin;
        }
        return (valueIn - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }

    public static float map(float valueIn, float inMin, float inMax, float outMin, float outMax) {
        if (Math.abs(inMax - inMin) < 1.0e-6f) {
            return outMin;
        }
        return (valueIn - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }

    public static boolean between(double min, double value, double max) {
        return value >= min && value <= max;
    }
}
