package com.huige233.transcend.util;

/** 采样标记工具类。 */
public class TranscendPickFlag {

    private static boolean active = false;

    public static void set(boolean value) {
        active = value;
    }

    public static boolean isActive() {
        return active;
    }
}
