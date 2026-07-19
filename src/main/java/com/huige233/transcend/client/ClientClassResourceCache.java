package com.huige233.transcend.client;

import com.huige233.transcend.ascension.resource.ClassResourceType;

public class ClientClassResourceCache {

    private static ClassResourceType type = null;
    private static float value = 0f;
    private static float maxValue = 100f;
    private static boolean inWindow = false;
    private static boolean overflowed = false;

    public static void update(ClassResourceType t, float val, float max,
                              boolean window, boolean overflow) {
        type = t;
        value = val;
        maxValue = max;
        inWindow = window;
        overflowed = overflow;
    }

    public static void clear() {
        type = null;
        value = 0f;
        maxValue = 100f;
        inWindow = false;
        overflowed = false;
    }

    public static ClassResourceType getType() { return type; }
    public static float getValue() { return value; }
    public static float getMaxValue() { return maxValue; }
    public static boolean isInWindow() { return inWindow; }
    public static boolean isOverflowed() { return overflowed; }
    public static float getRatio() {
        return maxValue > 0 ? Math.min(1f, value / maxValue) : 0f;
    }
}
