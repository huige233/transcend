package com.huige233.transcend.items.tech;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 验证科技提示中的整数容量和小数电量能够安全格式化为预期的整数字符串。 */
class TechTooltipNumbersTest {
    @Test
    void integerCapacityDoesNotUseFloatingPointFormatter() {
        assertEquals("1000", TechTooltipNumbers.whole(1000));
        assertEquals("0", TechTooltipNumbers.whole(0));
        assertEquals("2147483647", TechTooltipNumbers.whole(Integer.MAX_VALUE));
    }

    @Test
    void fractionalChargeAndHeatKeepWholeNumberDisplay() {
        assertEquals("12", TechTooltipNumbers.whole(12.25F));
        assertEquals("13", TechTooltipNumbers.whole(12.75F));
        assertEquals("0", TechTooltipNumbers.whole(0.0F));
    }
}
