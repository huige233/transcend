package com.huige233.transcend.items.tech;

import java.util.Locale;


/** 按整数或浮点数类型安全格式化科技物品提示中的整数显示值。 */
final class TechTooltipNumbers {
    private TechTooltipNumbers() {
    }

    static String whole(int value) {
        return Integer.toString(value);
    }

    static String whole(float value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }
}
