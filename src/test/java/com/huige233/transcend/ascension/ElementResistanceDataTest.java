package com.huige233.transcend.ascension;

import com.huige233.transcend.spell.SpellElement;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElementResistanceDataTest {
    @Test
    void setAndAddRejectNonFiniteValuesAndClampBonuses() {
        ElementResistanceData data = new ElementResistanceData();
        data.setBonus(SpellElement.FIRE, Float.NaN);
        assertEquals(0.0F, data.getBonus(SpellElement.FIRE));

        data.setBonus(SpellElement.FIRE, 2.0F);
        assertEquals(0.95F, data.getBonus(SpellElement.FIRE));
        data.addBonus(SpellElement.FIRE, Float.POSITIVE_INFINITY);
        assertEquals(0.95F, data.getBonus(SpellElement.FIRE));
        data.addBonus(SpellElement.FIRE, -10.0F);
        assertEquals(-0.50F, data.getBonus(SpellElement.FIRE));
    }

    @Test
    void loadRejectsNonFiniteValuesClampsAndIgnoresChaos() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("fire", Float.NaN);
        tag.putDouble("water", 4.0D);
        tag.putFloat("earth", -4.0F);
        tag.putFloat("chaos", 0.5F);

        ElementResistanceData data = new ElementResistanceData();
        data.load(tag);

        assertEquals(0.0F, data.getBonus(SpellElement.FIRE));
        assertEquals(0.95F, data.getBonus(SpellElement.WATER));
        assertEquals(-0.50F, data.getBonus(SpellElement.EARTH));
        assertEquals(0.0F, data.getBonus(SpellElement.CHAOS));
    }
}
