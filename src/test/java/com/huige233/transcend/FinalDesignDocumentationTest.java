package com.huige233.transcend;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 检查最终设计文档是否保留规定的数值常量、伤害管线和修炼系统运行边界说明。 */
class FinalDesignDocumentationTest {
    @Test
    void authoritativeSpecificationPinsKeyRuntimeConstants() throws IOException {
        String document = Files.readString(Path.of("docs/five-elements-cultivation-redesign.md"));

        for (String required : new String[] {
                "最终实现规范", "100 tick", "1.50x", "0.70x", "0.05", "0.01",
                "40 tick", "2` 魔力", "十七种效果", "1.00, 1.15, 1.30",
                "500*r^2", "750*r^2", "1000*r^2", "3600 tick", "60 轮",
                "52` 点", "43`", "15%", "30%", "20%", "2.5x",
                "规范伤害管线", "有意例外", "已知运行时测试范围"
        }) {
            assertTrue(document.contains(required), required);
        }
    }

    @Test
    void authoritativeSpecificationPinsCultivationRuntimeBoundaries() throws IOException {
        String document = Files.readString(Path.of("docs/five-elements-cultivation-redesign.md"));

        for (String required : new String[] {
                "混沌是第六个法术元素",
                "Nexus 的 ENTROPY 不是元素",
                "按本次实际生命伤害的 15% 治疗",
                "HUD 显示法术公式的原始配置消耗",
                "服务端权威支付",
                "只应用一次当前区块相染倍率"
        }) {
            assertTrue(document.contains(required), required);
        }
    }
}
