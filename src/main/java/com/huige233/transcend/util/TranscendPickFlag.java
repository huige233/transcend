package com.huige233.transcend.util;


/** 保存特殊实体拾取判定的临时开关，供渲染与交互检查共享当前选取上下文。 */
public class TranscendPickFlag {

    private static boolean active = false;

    public static void set(boolean value) {
        active = value;
    }

    public static boolean isActive() {
        return active;
    }
}
