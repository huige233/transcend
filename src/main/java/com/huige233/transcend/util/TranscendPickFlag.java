package com.huige233.transcend.util;

public class TranscendPickFlag {

    private static boolean active = false;

    public static void set(boolean value) {
        active = value;
    }

    public static boolean isActive() {
        return active;
    }
}
