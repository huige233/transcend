package com.huige233.transcend.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 检查穿甲注入不依赖合成参数类，并保留原减伤调用及韧性处理规则。 */
class ParticleBoltArmorMixinTest {
    private static final Path SOURCE = Path.of(
            "src/main/java/com/huige233/transcend/mixin/ParticleBoltArmorMixin.java");

    @Test
    void armorHookDoesNotRequireSyntheticArgsClasses() throws Exception {
        String source = Files.readString(SOURCE);
        assertFalse(source.contains("@ModifyArgs"));
        assertFalse(source.contains("injection.invoke.arg.Args"));
        assertTrue(source.contains("@WrapOperation(method = \"getDamageAfterArmorAbsorb\""));
        assertTrue(source.contains("CombatRules;getDamageAfterAbsorb(FFF)F"));
    }

    @Test
    void armorHookDelegatesOriginalDamageAndConditionalToughness() throws Exception {
        String source = Files.readString(SOURCE);
        assertTrue(source.contains("ArmorPenetration.effectiveArmor((LivingEntity) (Object) this, source, armor)"));
        assertTrue(source.contains("return original.call(damage, effective, effective != armor ? 0.0F : toughness);"));
    }
}
