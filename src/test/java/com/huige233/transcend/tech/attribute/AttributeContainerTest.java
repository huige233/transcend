package com.huige233.transcend.tech.attribute;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证科技属性先加后乘、同标识修饰器替换、临时修饰器过期及存档恢复行为。 */
class AttributeContainerTest {
    private static final double EPSILON = 0.000001D;

    @Test
    void appliesAdditionBeforeMultiplication() {
        AttributeContainer attributes = new AttributeContainer();
        attributes.addModifier(TechAttribute.DAMAGE_BASE,
                TechAttrModifier.permanent(UUID.randomUUID(), "base", 2.0D, ModifierOp.ADD));
        attributes.addModifier(TechAttribute.DAMAGE_BASE,
                TechAttrModifier.permanent(UUID.randomUUID(), "mult", 1.5D, ModifierOp.MULT));

        assertEquals((TechAttribute.DAMAGE_BASE.defaultValue + 2.0D) * 1.5D,
                attributes.getValue(TechAttribute.DAMAGE_BASE), EPSILON);
    }

    @Test
    void replacesModifiersWithSameUuidAndCleansExpiredEntries() {
        AttributeContainer attributes = new AttributeContainer();
        UUID id = UUID.randomUUID();
        attributes.addModifier(TechAttribute.CRIT_CHANCE,
                TechAttrModifier.permanent(id, "first", 0.1D, ModifierOp.ADD));
        attributes.addModifier(TechAttribute.CRIT_CHANCE,
                TechAttrModifier.temporary(id, "replacement", 0.25D, ModifierOp.ADD, 20L));

        assertEquals(TechAttribute.CRIT_CHANCE.defaultValue + 0.25D,
                attributes.getValue(TechAttribute.CRIT_CHANCE), EPSILON);
        attributes.tickCleanup(20L);
        assertFalse(attributes.hasModifiers(TechAttribute.CRIT_CHANCE));
    }

    @Test
    void roundTripsAndSkipsMalformedModifierData() {
        AttributeContainer original = new AttributeContainer();
        UUID id = UUID.randomUUID();
        original.addModifier(TechAttribute.HEAT_CAPACITY,
                TechAttrModifier.permanent(id, "capacity", 25.0D, ModifierOp.ADD));
        CompoundTag saved = original.save();

        AttributeContainer restored = new AttributeContainer();
        restored.load(saved);
        assertTrue(restored.hasModifiers(TechAttribute.HEAT_CAPACITY));
        assertEquals(original.getValue(TechAttribute.HEAT_CAPACITY),
                restored.getValue(TechAttribute.HEAT_CAPACITY), EPSILON);

        CompoundTag malformed = new CompoundTag();
        malformed.put("Attributes", new net.minecraft.nbt.ListTag());
        restored.load(malformed);
        assertFalse(restored.hasModifiers(TechAttribute.HEAT_CAPACITY));
    }
}
